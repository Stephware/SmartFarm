using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Harvests;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class HarvestsController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly ICropManagementService _crops;
    private readonly IHarvestService _harvests;

    public HarvestsController(
        UserManager<SmartFarmUser> users,
        ICropManagementService crops,
        IHarvestService harvests)
    {
        _users = users;
        _crops = crops;
        _harvests = harvests;
    }

    private Guid Actor => Guid.TryParse(_users.GetUserId(User), out var id)
        ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, Guid cycleId, CancellationToken ct)
    {
        try
        {
            var crop = await _crops.GetCycleAsync(farmId, cycleId, Actor, ct);
            if (crop is null) return NotFound();
            var overview = await _harvests.GetAsync(farmId, cycleId, Actor, ct);
            return View(new HarvestPage(crop, overview));
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound)
        {
            return NotFound();
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Record(Guid farmId, Guid cycleId,
        RecordHarvestInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["HarvestError"] = "Enter a positive quantity with no more than 3 decimal places.";
            return RedirectToAction(nameof(Index), new { farmId, cycleId });
        }
        try
        {
            await _harvests.RecordAsync(farmId, cycleId, Actor, input, ct);
            TempData["HarvestNotice"] = "Partial harvest recorded.";
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["HarvestError"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId, cycleId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Void(Guid farmId, Guid cycleId, Guid harvestId,
        VoidHarvestInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["HarvestError"] = "A reason of 3–300 characters is required.";
            return RedirectToAction(nameof(Index), new { farmId, cycleId });
        }
        try
        {
            await _harvests.VoidAsync(farmId, cycleId, harvestId, Actor, input, ct);
            TempData["HarvestNotice"] = "Harvest entry voided; its audit record is preserved.";
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["HarvestError"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId, cycleId });
    }
}

