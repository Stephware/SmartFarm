using System.Data;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Harvests;

namespace SmartFarm.Infrastructure.Services.Harvests;

public sealed class HarvestService(
    ICropCycleRepository cycles,
    IPlotRepository plots,
    ICropTypeRepository cropTypes,
    IHarvestRepository harvests,
    IUnitOfWork unit,
    IFarmAccessService access) : IHarvestService
{
    private static FarmOperationException Missing() =>
        new(FarmError.NotFound, "Harvest resource not found.");
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);
    private static FarmOperationException Conflict(string message) =>
        new(FarmError.Conflict, message);

    private async Task Require(Guid farmId, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, action, ct))
            throw Missing();
    }

    private Task<CropCycle?> FindCycle(Guid farmId, Guid cycleId, bool activePlot, CancellationToken ct) =>
        cycles.GetForFarmAsync(farmId, cycleId, activePlot, ct);

    private static HarvestEntry Map(Harvest h) =>
        new(h.Id, h.CropCycleId, h.Quantity, h.Unit, h.HarvestedAtUtc,
            h.RecordedAtUtc, h.RecordedByUserId, h.Notes,
            h.VoidedAtUtc is not null, h.VoidedAtUtc, h.VoidedByUserId,
            h.VoidReason);

    public async Task<HarvestOverview> GetAsync(
        Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var cycle = await FindCycle(farmId, cycleId, false, ct) ?? throw Missing();
        var plotName = await plots.GetNameAsync(cycle.PlotId, ct);
        var cropName = await cropTypes.GetNameAsync(cycle.CropTypeId, ct);
        var records = (await harvests.ListForCycleAsync(cycleId, ct)).Select(Map).ToList();

        var valid = records.Where(h => !h.IsVoided).ToList();
        var permission = await access.GetAccessLevelAsync(farmId, actor, ct);
        var activePlot = await plots.IsActiveAsync(farmId, cycle.PlotId, ct);
        return new HarvestOverview(farmId, cycleId, cropName, plotName,
            cycle.HarvestUnit, cycle.Status,
            activePlot && HarvestRules.CanHarvest(cycle.Status),
            permission is FarmAccessLevel.Owner or FarmAccessLevel.Manager,
            valid.Sum(h => h.Quantity), valid.Count, records.Count - valid.Count, records);
    }

    public async Task<HarvestEntry> RecordAsync(
        Guid farmId, Guid cycleId, Guid actor, RecordHarvestInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct); // Field Workers may record partial harvests.
        if (!HarvestRules.IsValidQuantity(input.Quantity) || input.Notes?.Length > 1000)
            throw Invalid("Use a positive harvest quantity with at most 3 decimal places and notes up to 1000 characters.");

        // Hold the crop cycle read lock until the harvest insert commits. A
        // concurrent completion must not slip between checking Active and saving.
        // A sync request already has a serializable transaction that also saves its
        // idempotency record. Do not open a nested transaction inside that request.
        var transaction = !unit.HasActiveTransaction
            ? await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct)
            : null;
        try
        {
            var cycle = await FindCycle(farmId, cycleId, true, ct) ?? throw Missing();
            if (!HarvestRules.CanHarvest(cycle.Status))
                throw Conflict("Record harvests while the crop cycle is Active. Completion is a separate action.");

            var entry = new Harvest
            {
                CropCycleId = cycleId, HarvestedAtUtc = DateTime.UtcNow,
                Quantity = input.Quantity, Unit = cycle.HarvestUnit,
                RecordedByUserId = actor,
                Notes = string.IsNullOrWhiteSpace(input.Notes) ? null : input.Notes.Trim()
            };
            harvests.Add(entry);
            await unit.SaveChangesAsync(ct);
            if (transaction is not null)
                await transaction.CommitAsync(ct);
            return Map(entry);
        }
        catch (DbUpdateException ex) when (ex.InnerException is SqlException sql && sql.Number == 547)
        {
            throw Invalid("The harvest violates the existing SQL schema constraints.");
        }
        catch (DbUpdateException ex) when (ex.InnerException is SqlException sql && sql.Number == 1205)
        {
            throw Conflict("Harvest was not saved due to a concurrent update. Refresh before retrying.");
        }
        catch (SqlException ex) when (ex.Number == 1205)
        {
            throw Conflict("Harvest was not saved due to a concurrent update. Please retry.");
        }
        finally
        {
            if (transaction is not null)
                await transaction.DisposeAsync();
        }
    }

    public async Task<HarvestEntry> VoidAsync(
        Guid farmId, Guid cycleId, Guid harvestId, Guid actor,
        VoidHarvestInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (!HarvestRules.IsValidVoidReason(input.Reason))
            throw Invalid("Provide a reason of 3–300 characters.");

        // Read-only verification; the database UPDATE condition below ensures
        // two void requests cannot both succeed on the same harvest.
        if (await FindCycle(farmId, cycleId, false, ct) is null) throw Missing();
        var original = await harvests.GetByIdAsync(cycleId, harvestId, ct) ?? throw Missing();
        if (original.VoidedAtUtc is not null) throw Conflict("This harvest is already voided.");

        var when = DateTime.UtcNow;
        var voided = await harvests.TryVoidAsync(cycleId, harvestId, actor, when, input.Reason.Trim(), ct);
        if (!voided) throw Conflict("This harvest was already voided. Refresh the history.");

        return Map(new Harvest
        {
            Id = original.Id, CropCycleId = original.CropCycleId,
            Quantity = original.Quantity, Unit = original.Unit,
            HarvestedAtUtc = original.HarvestedAtUtc, RecordedAtUtc = original.RecordedAtUtc,
            RecordedByUserId = original.RecordedByUserId, Notes = original.Notes,
            VoidedAtUtc = when, VoidedByUserId = actor, VoidReason = input.Reason.Trim()
        });
    }
}

