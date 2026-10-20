using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Interfaces.Crops;
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
    private readonly IMonitoringService _monitoring;
    private readonly IFarmTaskService _tasks;

    public MonitoringController(
        UserManager<SmartFarmUser> users,
        ICropManagementService crops,
        IMonitoringService monitoring,
        IFarmTaskService tasks)
    {
        _users = users;
        _crops = crops;
        _monitoring = monitoring;
        _tasks = tasks;
    }

    private Guid Actor => Guid.TryParse(_users.GetUserId(User), out var id)
        ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, Guid cycleId, CancellationToken ct)
    {
        try
        {
            var cycle = await _crops.GetCycleAsync(farmId, cycleId, Actor, ct);
            if (cycle is null) return NotFound();
            return View(new MonitoringWorkspaceViewModel(cycle,
                await _monitoring.GetAsync(farmId, cycleId, Actor, ct),
                await _tasks.ListAsync(farmId, Actor, ct)));
        }
        catch (FarmOperationException) { return NotFound(); }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Activity(Guid farmId, Guid cycleId,
        RecordActivityInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Choose a valid activity and notes.";
            return RedirectToAction(nameof(Index), new { farmId, cycleId });
        }
        try { await _monitoring.RecordActivityAsync(farmId, cycleId, Actor, input, ct); }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = error.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId, cycleId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Observation(Guid farmId, Guid cycleId,
        RecordObservationInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Choose valid health status, stage and notes.";
            return RedirectToAction(nameof(Index), new { farmId, cycleId });
        }
        try { await _monitoring.RecordObservationAsync(farmId, cycleId, Actor, input, ct); }
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

