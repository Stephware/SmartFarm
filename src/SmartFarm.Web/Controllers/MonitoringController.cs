using System.Data.Common;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Monitoring;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class MonitoringController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly ICropManagementService _crops;
    private readonly IFarmManagementService _farms;
    private readonly IMonitoringService _monitoring;
    private readonly IFarmTaskService _tasks;

    public MonitoringController(
        UserManager<SmartFarmUser> users,
        ICropManagementService crops,
        IFarmManagementService farms,
        IMonitoringService monitoring,
        IFarmTaskService tasks)
    {
        _users = users;
        _crops = crops;
        _farms = farms;
        _monitoring = monitoring;
        _tasks = tasks;
    }

    private Guid Actor => Guid.TryParse(_users.GetUserId(User), out var id)
        ? id : Guid.Empty;

    [HttpGet]
    public Task<IActionResult> Index(Guid farmId, Guid cycleId, CancellationToken ct, string? quick = null) =>
        Workspace(farmId, cycleId, ct,
            activity: new RecordActivityInput
            {
                ActivityType = quick == "fertilizing" ? (byte)2 : (byte)1
            });

    private async Task<IActionResult> Workspace(Guid farmId, Guid cycleId, CancellationToken ct,
        RecordActivityInput? activity = null, RecordObservationInput? observation = null)
    {
        try
        {
            var cycle = await _crops.GetCycleAsync(farmId, cycleId, Actor, ct);
            if (cycle is null) return NotFound();
            var monitoring = await _monitoring.GetAsync(farmId, cycleId, Actor, ct);
            var taskList = await _tasks.ListAsync(farmId, Actor, ct);
            // An optional manager control must not take all monitoring offline
            // when the separate farm-details service is temporarily unavailable.
            bool canManageIssues = false;
            try
            {
                var farm = await _farms.GetAsync(farmId, Actor, ct);
                canManageIssues = farm?.Role is "Owner" or "Manager";
            }
            catch (DbException) { /* Read-only monitoring remains available. */ }
            catch (HttpRequestException) { /* API role lookup unavailable. */ }
            return View("Index", new MonitoringWorkspaceViewModel(cycle,
                monitoring, taskList)
            {
                CanManageIssues = canManageIssues,
                Activity = activity ?? new() { ActivityType = 1 }, Observation = observation ?? new()
            });
        }
        catch (FarmOperationException) { return NotFound(); }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Activity(Guid farmId, Guid cycleId,
        [Bind(Prefix = "Activity")] RecordActivityInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, cycleId, ct, activity: input);
        }
        try
        {
            await _monitoring.RecordActivityAsync(farmId, cycleId, Actor, input, ct);
            TempData["Notice"] = "Activity saved to this crop's history.";
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", error.Message);
            return await Workspace(farmId, cycleId, ct, activity: input);
        }
        return RedirectToAction(nameof(Index), new { farmId, cycleId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Observation(Guid farmId, Guid cycleId,
        [Bind(Prefix = "Observation")] RecordObservationInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, cycleId, ct, observation: input);
        }
        try
        {
            await _monitoring.RecordObservationAsync(farmId, cycleId, Actor, input, ct);
            TempData["Notice"] = "Inspection saved. You can now attach photos to the inspection below.";
            TempData["NextStep"] = "photos";
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", error.Message);
            return await Workspace(farmId, cycleId, ct, observation: input);
        }
        return RedirectToAction(nameof(Index), new { farmId, cycleId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> FollowUp(Guid farmId, Guid cycleId, Guid observationId,
        UpdateObservationFollowUpInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Choose a valid follow-up status and task.";
            return RedirectToAction(nameof(Index), new { farmId, cycleId });
        }
        try
        {
            await _monitoring.UpdateFollowUpAsync(
                farmId, cycleId, observationId, Actor, input, ct);
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = error.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId, cycleId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    [RequestSizeLimit(6 * 1024 * 1024)]
    [RequestFormLimits(MultipartBodyLengthLimit = 6 * 1024 * 1024)]
    public async Task<IActionResult> Upload(Guid farmId, Guid cycleId, Guid observationId,
        IFormFile? photo, CancellationToken ct)
    {
        if (photo is null)
        {
            TempData["Error"] = "Choose a photo.";
            return RedirectToAction(nameof(Index), new { farmId, cycleId });
        }
        try
        {
            await using var fileStream = photo.OpenReadStream();
            await _monitoring.AddPhotoAsync(farmId, cycleId, observationId,
                Actor, fileStream, photo.Length, photo.ContentType, ct);
            TempData["Notice"] = "Photo attached to this inspection.";
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = error.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId, cycleId });
    }

    [HttpGet]
    public async Task<IActionResult> Photo(Guid farmId, Guid cycleId, Guid photoId, CancellationToken ct)
    {
        try
        {
            var (stream, contentType) = await _monitoring.OpenPhotoAsync(
                farmId, cycleId, photoId, Actor, ct);
            Response.Headers["X-Content-Type-Options"] = "nosniff";
            Response.Headers.CacheControl = "private, no-store";
            return File(stream, contentType);
        }
        catch (FarmOperationException) { return NotFound(); }
    }
}

