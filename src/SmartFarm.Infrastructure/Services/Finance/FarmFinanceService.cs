using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Finance;

public sealed class FarmFinanceService(SmartFarmDbContext db, IFarmAccessService access)
    : IFarmFinanceService
{
    private static FarmOperationException NotFound() => new(FarmError.NotFound, "Record not found.");
    private static FarmOperationException Invalid(string message) => new(FarmError.InvalidInput, message);

    private static FarmFinanceEntryDetails Map(FarmFinanceEntry x) => new(
        x.Id, x.FarmId, x.CropCycleId, x.EntryDate, x.Kind, x.Category,
        x.AmountPhp, x.Notes, x.RecordedAtUtc, x.VoidedAtUtc is not null, x.VoidReason);

    private async Task Require(Guid farmId, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, action, ct)) throw NotFound();
    }

    public async Task<FarmFinanceOverview> ListAsync(Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var entries = await db.Set<FarmFinanceEntry>().AsNoTracking()
            .Where(x => x.FarmId == farmId)
            .OrderByDescending(x => x.EntryDate).ThenByDescending(x => x.RecordedAtUtc)
            .Take(500).ToListAsync(ct);
        var sums = await db.Set<FarmFinanceEntry>().AsNoTracking()
            .Where(x => x.FarmId == farmId && x.VoidedAtUtc == null)
            .GroupBy(x => x.Kind)
            .Select(g => new { Kind = g.Key, Total = g.Sum(e => e.AmountPhp) })
            .ToListAsync(ct);
        var expenses = sums.FirstOrDefault(x => x.Kind == "Expense")?.Total ?? 0m;
        var income = sums.FirstOrDefault(x => x.Kind == "Income")?.Total ?? 0m;
        return new(farmId, expenses, income, income - expenses, entries.Select(Map).ToList());
    }

    public async Task<FarmFinanceEntryDetails> RecordAsync(Guid farmId, Guid actor,
        FarmFinanceInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var category = input.Category?.Trim() ?? "";
        if (input.Kind is not ("Expense" or "Income") ||
            category.Length is < 2 or > 60 ||
            input.AmountPhp < 0.01m || input.AmountPhp > 999999999999.99m ||
            decimal.Round(input.AmountPhp, 2) != input.AmountPhp ||
            input.EntryDate < new DateOnly(2000, 1, 1) ||
            input.EntryDate > new DateOnly(2100, 12, 30) ||
            input.Notes?.Length > 500 || input.CropCycleId == Guid.Empty)
            throw Invalid("Enter a valid date, entry type, category and positive PHP amount.");

        if (input.CropCycleId is { } cycleId && !await (
            from crop in db.CropCycles.AsNoTracking()
            join plot in db.Plots.AsNoTracking() on crop.PlotId equals plot.Id
            where crop.Id == cycleId && plot.FarmId == farmId
            select crop.Id).AnyAsync(ct))
            throw Invalid("Select a crop cycle belonging to this farm.");

        var entry = new FarmFinanceEntry
        {
            FarmId = farmId, CropCycleId = input.CropCycleId, RecordedByUserId = actor,
            EntryDate = input.EntryDate, Kind = input.Kind, Category = category,
            AmountPhp = input.AmountPhp, Notes = input.Notes?.Trim(),
            RecordedAtUtc = DateTime.UtcNow
        };
        db.Set<FarmFinanceEntry>().Add(entry);
        await db.SaveChangesAsync(ct);
        return Map(entry);
    }

    public async Task VoidAsync(Guid farmId, Guid entryId, Guid actor,
        FarmFinanceVoidInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var reason = input.Reason?.Trim() ?? "";
        if (reason.Length is < 3 or > 300) throw Invalid("Provide a 3–300 character reason.");
        var entry = await db.Set<FarmFinanceEntry>().SingleOrDefaultAsync(
            x => x.FarmId == farmId && x.Id == entryId, ct) ?? throw NotFound();
        if (entry.VoidedAtUtc is not null)
            throw new FarmOperationException(FarmError.Conflict, "The entry is already voided.");
        entry.VoidedAtUtc = DateTime.UtcNow;
        entry.VoidedByUserId = actor;
        entry.VoidReason = reason;
        await db.SaveChangesAsync(ct);
    }
}
