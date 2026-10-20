using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Crops;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Services.Crops;

public sealed class CropManagementService(
    ICropTypeRepository cropTypes,
    ICropCycleRepository cycles,
    IPlotRepository plots,
    IFarmTaskRepository tasks,
    IUnitOfWork unit,
    IFarmAccessService access) : ICropManagementService
{
    private static FarmOperationException NotFound() =>
        new(FarmError.NotFound, "Resource not found.");

    private static FarmOperationException Invalid(string description) =>
        new(FarmError.InvalidInput, description);

    private static FarmOperationException Conflict(string description) =>
        new(FarmError.Conflict, description);

    private async Task Require(Guid farmId, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, action, ct)) throw NotFound();
    }

    private static byte[] ParseVersion(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) throw Invalid("A current row version is required.");
        try
        {
            var bytes = Convert.FromBase64String(value);
            if (bytes.Length == 8) return bytes;
        }
        catch (FormatException) { }
        throw Invalid("Invalid row version.");
    }

    private static void ValidatePlan(CropCyclePlanInput input)
    {
        if (input.CropTypeId == Guid.Empty || input.PlannedPlantingDate == default)
            throw Invalid("Choose a crop type and planting date.");
        if (input.ExpectedHarvestDate is { } harvest && harvest < input.PlannedPlantingDate)
            throw Invalid("Expected harvest cannot precede planned planting.");
        if (input.WateringIntervalDays is <= 0 ||
            input.HarvestUnit?.Trim().Length > 20)
            throw Invalid("Invalid watering interval or harvest unit.");
    }

    private async Task Save(CancellationToken ct)
    {
        try
        {
            await unit.SaveChangesAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            throw Conflict("This crop cycle has changed. Reload and retry.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number is 2601 or 2627)
        {
            throw Conflict("A crop with this name already exists or the plot already has an active crop.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number is 547)
        {
            throw Invalid("The crop operation violates an existing database rule.");
        }
    }

    private static CropTypeDetails Map(CropType type) => new(
        type.Id, type.Name, type.DefaultGrowthDays,
        type.DefaultWateringIntervalDays, type.DefaultHarvestUnit);

    private static CropCycleDetails Map(CropCycle cycle, string plotName, string typeName, Guid farmId) => new(
        cycle.Id, farmId, cycle.PlotId, plotName, cycle.CropTypeId, typeName,
        (CropCycleStatus)cycle.Status, cycle.PlannedPlantingDate,
        cycle.PlantedDate, cycle.ExpectedHarvestDate,
        cycle.HarvestUnit, cycle.WateringIntervalDays,
        cycle.CompletedAtUtc, cycle.TerminationReason,
        Convert.ToBase64String(cycle.RowVersion));

    private async Task<CropCycleDetails> MapLoaded(CropCycle cycle, Guid farmId, CancellationToken ct)
    {
        var plotName = await plots.GetNameAsync(cycle.PlotId, ct);
        var typeName = await cropTypes.GetNameAsync(cycle.CropTypeId, ct);
        return Map(cycle, plotName, typeName, farmId);
    }

    public async Task<IReadOnlyList<CropTypeDetails>> ListCropTypesAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var catalog = await cropTypes.ListActiveAsync(ct);
        return catalog.Select(Map).ToList();
    }

    public async Task<CropTypeDetails> CreateCropTypeAsync(
        Guid farmId, Guid actor, CropTypeInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var name = input.Name?.Trim() ?? "";
        var unit = input.DefaultHarvestUnit?.Trim() ?? "";
        if (name.Length is < 2 or > 100 || unit.Length is < 1 or > 20 ||
            input.DefaultGrowthDays is <= 0 || input.DefaultWateringIntervalDays is <= 0)
            throw Invalid("Enter a valid crop name, harvest unit, and positive intervals.");

        // CropType is a global shared catalog in the existing schema. To avoid
        // cross-farm edits, this phase permits only creation, never editing/deactivation.
        var type = new CropType
        {
            Name = name, DefaultHarvestUnit = unit,
            DefaultGrowthDays = input.DefaultGrowthDays,
            DefaultWateringIntervalDays = input.DefaultWateringIntervalDays,
            IsActive = true
        };
        cropTypes.Add(type);
        await Save(ct);
        return Map(type);
    }

    public async Task<IReadOnlyList<CropCycleDetails>> ListCyclesAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);

        // History includes completed, failed, cancelled and planned cycles;
        // exclude data from archived plots only when creating new cycles.
        var listed = await cycles.ListWithNamesForFarmAsync(farmId, ct);
        return listed.Select(c => Map(c.Cycle, c.PlotName, c.TypeName, farmId)).ToList();
    }

    public async Task<CropCycleDetails?> GetCycleAsync(
        Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, false, ct);
        return cycle is null ? null : await MapLoaded(cycle, farmId, ct);
    }

    public async Task<CropCycleDetails> PlanAsync(
        Guid farmId, Guid plotId, Guid actor, CropCyclePlanInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidatePlan(input);
        if (!await plots.IsActiveAsync(farmId, plotId, ct))
            throw NotFound();

        var type = await cropTypes.GetActiveAsync(input.CropTypeId, ct) ?? throw Invalid("Choose an active crop type.");

        var cycle = new CropCycle
        {
            PlotId = plotId, CropTypeId = type.Id,
            PlannedPlantingDate = input.PlannedPlantingDate,
            ExpectedHarvestDate = input.ExpectedHarvestDate,
            HarvestUnit = string.IsNullOrWhiteSpace(input.HarvestUnit) ? type.DefaultHarvestUnit : input.HarvestUnit.Trim(),
            WateringIntervalDays = input.WateringIntervalDays ?? type.DefaultWateringIntervalDays,
            Status = (byte)CropCycleStatus.Planned
        };
        cycles.Add(cycle);
        await Save(ct);
        return await MapLoaded(cycle, farmId, ct);
    }

    public async Task<CropCycleDetails> UpdatePlanAsync(
        Guid farmId, Guid cycleId, Guid actor, CropCyclePlanInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidatePlan(input);
        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, true, ct) ?? throw NotFound();
        if (cycle.Status != (byte)CropCycleStatus.Planned)
            throw Conflict("Only planned crops can be edited.");

        var type = await cropTypes.GetActiveAsync(input.CropTypeId, ct) ?? throw Invalid("Choose an active crop type.");
        cycles.SetOriginalRowVersion(cycle, ParseVersion(input.RowVersion));
        cycle.CropTypeId = type.Id;
        cycle.PlannedPlantingDate = input.PlannedPlantingDate;
        cycle.ExpectedHarvestDate = input.ExpectedHarvestDate;
        cycle.HarvestUnit = string.IsNullOrWhiteSpace(input.HarvestUnit) ? type.DefaultHarvestUnit : input.HarvestUnit.Trim();
        cycle.WateringIntervalDays = input.WateringIntervalDays ?? type.DefaultWateringIntervalDays;
        // Keep existing generated planting assignments aligned when the plan date moves.
        var changedTasks = await tasks.ListPendingForCycleAsync(cycleId, ct);
        var changedAt = DateTime.UtcNow;
        foreach (var task in changedTasks.Where(t => t.TaskType == 1 && t.DedupeKey is not null))
        {
            if (task.DueDate == cycle.PlannedPlantingDate) continue;
            task.DueDate = cycle.PlannedPlantingDate;
            task.DedupeKey = TaskRules.ScheduleKey(cycleId, 1, cycle.PlannedPlantingDate);
            await tasks.DismissRemindersAsync(task.Id, changedAt, ct);
        }
        await Save(ct);
        return await MapLoaded(cycle, farmId, ct);
    }

    public async Task<CropCycleDetails> TransitionAsync(
        Guid farmId, Guid cycleId, Guid actor, CropCycleTransitionInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, true, ct) ?? throw NotFound();

        var before = (CropCycleStatus)cycle.Status;
        var after = input.Status;
        if (!CropCycleRules.CanTransition(before, after))
            throw Conflict("This crop status change is not allowed.");
        if (after == CropCycleStatus.Active)
        {
            if (input.PlantedDate is not { } planted || planted == default)
                throw Invalid("Provide the actual planting date.");
            if (cycle.ExpectedHarvestDate is { } expected && expected < planted)
                throw Invalid("Expected harvest cannot precede actual planting. Edit the plan first.");
            cycle.PlantedDate = planted;
        }
        else if (input.PlantedDate is not null)
            throw Invalid("Only activation can set the planting date.");

        var reason = input.TerminationReason?.Trim();
        if (after is CropCycleStatus.Failed or CropCycleStatus.Cancelled &&
            (string.IsNullOrEmpty(reason) || reason.Length > 300))
            throw Invalid("A reason (up to 300 characters) is required to end a crop early.");
        if (after is CropCycleStatus.Active or CropCycleStatus.Completed &&
            !string.IsNullOrEmpty(reason))
            throw Invalid("A reason is only used for failed or cancelled crops.");

        cycles.SetOriginalRowVersion(cycle, ParseVersion(input.RowVersion));
        cycle.Status = (byte)after;
        cycle.TerminationReason = CropCycleRules.IsTerminal(after) ? reason : null;
        if (after == CropCycleStatus.Completed)
            cycle.CompletedAtUtc = DateTime.UtcNow;

        // Crop status and related task changes are saved in one transaction.
        if (after == CropCycleStatus.Active || CropCycleRules.IsTerminal(after))
        {
            var now = DateTime.UtcNow;
            var openTasks = await tasks.ListPendingForCycleAsync(cycleId, ct);
            foreach (var task in openTasks)
            {
                if (after == CropCycleStatus.Active && task.TaskType != 1) continue;
                var nextStatus = after == CropCycleStatus.Active ? (byte)1 : (byte)3;
                if (TaskRules.Close(task, nextStatus, actor, now))
                    await tasks.DismissRemindersAsync(task.Id, now, ct);
            }
        }

        // SQL has UX_CropCycle_OneActivePerPlot (filtered Status=1).
        // A second activation is rejected by the unique index, even under races.
        await Save(ct);
        return await MapLoaded(cycle, farmId, ct);
    }
}

