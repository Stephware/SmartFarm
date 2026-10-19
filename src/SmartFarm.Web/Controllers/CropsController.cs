using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Crops;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class CropsController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly ICropManagementService _crops;
    private readonly IFarmManagementService _farms;

    public CropsController(
        UserManager<SmartFarmUser> users,
        ICropManagementService crops,
        IFarmManagementService farms)
    {
        _users = users;
        _crops = crops;
        _farms = farms;
    }

    private Guid Actor => Guid.TryParse(_users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(farmId, Actor, ct);
        if (farm is null) return NotFound();

        return View(new CropWorkspaceViewModel(
            farm, await _farms.ListPlotsAsync(farmId, Actor, ct),
            await _crops.ListCropTypesAsync(farmId, Actor, ct),
            await _crops.ListCyclesAsync(farmId, Actor, ct)));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> CreateCropType(Guid farmId, CropTypeInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Check the crop type name, units and optional durations.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try { await _crops.CreateCropTypeAsync(farmId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Plan(Guid farmId, Guid plotId, CropCyclePlanInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Choose a crop type, planting date, and valid plan details.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try { await _crops.PlanAsync(farmId, plotId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpGet]
    public async Task<IActionResult> EditPlan(Guid farmId, Guid cycleId, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(farmId, Actor, ct);
        if (farm?.Role is not ("Owner" or "Manager")) return NotFound();

        var cycle = await _crops.GetCycleAsync(farmId, cycleId, Actor, ct);
        if (cycle?.Status != CropCycleStatus.Planned) return NotFound();

        return View(new CropPlanEditViewModel(farm, cycle, new CropCyclePlanInput
        {
            CropTypeId = cycle.CropTypeId,
            PlannedPlantingDate = cycle.PlannedPlantingDate,
            ExpectedHarvestDate = cycle.ExpectedHarvestDate,
            HarvestUnit = cycle.HarvestUnit,
            WateringIntervalDays = cycle.WateringIntervalDays,
            RowVersion = cycle.RowVersion
        }, await _crops.ListCropTypesAsync(farmId, Actor, ct)));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> EditPlan(Guid farmId, Guid cycleId,
        [Bind(Prefix = "Input")] CropCyclePlanInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Check the plan details before saving.";
            return RedirectToAction(nameof(Index), new { farmId });
        }

        try
        {
            await _crops.UpdatePlanAsync(farmId, cycleId, Actor, input, ct);
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Transition(Guid farmId, Guid cycleId,
        CropCycleTransitionInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Please provide valid crop status information.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try { await _crops.TransitionAsync(farmId, cycleId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }
}

