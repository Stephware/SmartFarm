using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Crops;
using SmartFarm.Web.ViewModels.Monitoring;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class CropsController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly ICropManagementService _crops;
    private readonly IFarmManagementService _farms;
    private readonly IMonitoringService _monitoring;
    private readonly IHarvestService _harvests;
    private readonly IFarmTaskService _tasks;

    public CropsController(
        UserManager<SmartFarmUser> users,
        ICropManagementService crops,
        IFarmManagementService farms, IMonitoringService monitoring,
        IHarvestService harvests, IFarmTaskService tasks)
    {
        _users = users;
        _crops = crops;
        _farms = farms;
        _monitoring = monitoring;
        _harvests = harvests;
        _tasks = tasks;
    }

    private Guid Actor => Guid.TryParse(_users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public Task<IActionResult> Index(Guid farmId, CancellationToken ct, string? month = null,
        Guid? plotId = null, Guid? cycleId = null) =>
        Workspace(farmId, ct, plotId: plotId, month: month, historyCycleId: cycleId);

    private async Task<IActionResult> Workspace(Guid farmId, CancellationToken ct,
        CropTypeInput? cropType = null, CropCyclePlanInput? plan = null, Guid? plotId = null,
        CropCycleTransitionInput? transition = null, Guid? cycleId = null, string? month = null,
        Guid? historyCycleId = null)
    {
        var farm = await _farms.GetAsync(farmId, Actor, ct);
        if (farm is null) return NotFound();

        var cycles = await _crops.ListCyclesAsync(farmId, Actor, ct);
        CropTimelinePage? history = null;
        if (historyCycleId is { } selectedId)
        {
            var selected = cycles.FirstOrDefault(c => c.Id == selectedId);
            if (selected is null) return NotFound();
            try
            {
                history = CropTimelineBuilder.Build(selected,
                    await _monitoring.GetAsync(farmId, selectedId, Actor, ct),
                    await _harvests.GetAsync(farmId, selectedId, Actor, ct),
                    await _tasks.ListAsync(farmId, Actor, ct), farm.TimeZoneId);
            }
            catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
            { return NotFound(); }
        }
        return View("Index", new CropWorkspaceViewModel(
            farm, await _farms.ListPlotsAsync(farmId, Actor, ct),
            await _crops.ListCropTypesAsync(farmId, Actor, ct),
            cycles)
        {
            Agenda = CropAgendaBuilder.Build(cycles,
                CropAgendaBuilder.SelectMonth(month, farm.TimeZoneId, DateTime.UtcNow)),
            CropType = cropType ?? new(), Plan = plan ?? new(), PlanPlotId = plotId,
            Transition = transition ?? new(), TransitionCycleId = cycleId,
            SelectedHistory = history
        });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> CreateCropType(Guid farmId,
        [Bind(Prefix = "CropType")] CropTypeInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, ct, cropType: input);
        }
        try { await _crops.CreateCropTypeAsync(farmId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await Workspace(farmId, ct, cropType: input);
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Plan(Guid farmId, Guid plotId,
        [Bind(Prefix = "Plan")] CropCyclePlanInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, ct, plan: input, plotId: plotId);
        }
        try { await _crops.PlanAsync(farmId, plotId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await Workspace(farmId, ct, plan: input, plotId: plotId);
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
            return await InvalidEditPlan(farmId, cycleId, input, ct);
        }

        try
        {
            await _crops.UpdatePlanAsync(farmId, cycleId, Actor, input, ct);
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await InvalidEditPlan(farmId, cycleId, input, ct);
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    private async Task<IActionResult> InvalidEditPlan(Guid farmId, Guid cycleId,
        CropCyclePlanInput input, CancellationToken ct)
    {
        var result = await EditPlan(farmId, cycleId, ct);
        if (result is ViewResult view && view.Model is CropPlanEditViewModel model)
        {
            view.ViewName = "EditPlan";
            view.ViewData.Model = model with { Input = input };
        }
        return result;
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Transition(Guid farmId, Guid cycleId,
        [Bind(Prefix = "Transition")] CropCycleTransitionInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return await Workspace(farmId, ct, transition: input, cycleId: cycleId);
        }
        try { await _crops.TransitionAsync(farmId, cycleId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            return await Workspace(farmId, ct, transition: input, cycleId: cycleId);
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }
}

