using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Tasks;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class TasksController(
    UserManager<SmartFarmUser> users,
    IFarmManagementService farms,
    ICropManagementService crops,
    IFarmTaskService tasks) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct, string? view = null)
    {
        var farm = await farms.GetAsync(farmId, Actor, ct);
        if (farm is null) return NotFound();
        DateOnly? today = null;
        try
        {
            var zone = TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId);
            today = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(DateTime.UtcNow, zone));
        }
        catch (TimeZoneNotFoundException) { /* Don't guess the farm's local day. */ }
        catch (InvalidTimeZoneException) { /* Dates are unavailable until farm zone is fixed. */ }
        var selected = view is "today" or "upcoming" or "completed" or "all" ? view : "today";
        return View(new TaskPage(farm, await tasks.ListAsync(farmId, Actor, ct),
            await farms.MembersAsync(farmId, Actor, ct),
            await crops.ListCyclesAsync(farmId, Actor, ct))
        {
            FarmToday = today,
            SelectedView = selected
        });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(Guid farmId, FarmTaskInput input, CancellationToken ct)
    {
        try { await tasks.CreateAsync(farmId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Update(Guid farmId, Guid taskId,
        FarmTaskUpdateInput input, CancellationToken ct)
    {
        try { await tasks.UpdateAsync(farmId, taskId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Close(Guid farmId, Guid taskId, FarmTaskCloseInput input, CancellationToken ct)
    {
        try { await tasks.CloseAsync(farmId, taskId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }
}
