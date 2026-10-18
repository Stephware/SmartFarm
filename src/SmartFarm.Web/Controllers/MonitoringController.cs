using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Monitoring;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class MonitoringController(
    UserManager<SmartFarmUser> users,
    ICropManagementService crops,
    IMonitoringService monitoring) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id)
        ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, Guid cycleId, CancellationToken ct)
    {
        try
        {
            var cycle = await crops.GetCycleAsync(farmId, cycleId, Actor, ct);
            if (cycle is null) return NotFound();
            return View(new MonitoringWorkspaceViewModel(cycle,
                await monitoring.GetAsync(farmId, cycleId, Actor, ct)));
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
        try { await monitoring.RecordActivityAsync(farmId, cycleId, Actor, input, ct); }
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
        try { await monitoring.RecordObservationAsync(farmId, cycleId, Actor, input, ct); }
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
            await monitoring.AddPhotoAsync(farmId, cycleId, observationId,
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
            var (stream, contentType) = await monitoring.OpenPhotoAsync(
                farmId, cycleId, photoId, Actor, ct);
            Response.Headers["X-Content-Type-Options"] = "nosniff";
            Response.Headers.CacheControl = "private, no-store";
            return File(stream, contentType);
        }
        catch (FarmOperationException) { return NotFound(); }
    }
}

