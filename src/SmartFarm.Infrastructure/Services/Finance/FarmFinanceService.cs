using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Core.Services.Finance;
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

    public async Task<FarmFinanceInsights> InsightsAsync(Guid farmId, Guid actor,
        DateOnly? from = null, DateOnly? to = null, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var earliest = new DateOnly(2000, 1, 1);
        var latest = new DateOnly(2100, 12, 30);
        if (from is { } start && (start < earliest || start > latest) ||
            to is { } end && (end < earliest || end > latest) ||
            from is not null && to is not null && from > to)
            throw Invalid("Choose valid finance dates between 2000 and 2100, with From before To.");

        // Compute all matching rows with SQL aggregation, not the 500-entry
        // display limit in ListAsync. Never count voided cash entries.
        var query = db.Set<FarmFinanceEntry>().AsNoTracking()
            .Where(x => x.FarmId == farmId && x.VoidedAtUtc == null);
        if (from is { } first) query = query.Where(x => x.EntryDate >= first);
        if (to is { } last) query = query.Where(x => x.EntryDate <= last);
        var aggregates = await query
            .GroupBy(x => new
            {
                Year = x.EntryDate.Year,
                Month = x.EntryDate.Month,
                x.Kind, x.Category, x.CropCycleId
            })
            .Select(g => new
            {
                g.Key.Year, g.Key.Month, g.Key.Kind, g.Key.Category,
                g.Key.CropCycleId,
                Amount = g.Sum(x => x.AmountPhp),
                Count = g.Count()
            })
            .ToListAsync(ct);
        var rows = aggregates.Select(x => new FarmFinanceAggregate(
            x.Year, x.Month, x.Kind, x.Category, x.CropCycleId,
            x.Amount, x.Count)).ToList();
        var cycleIds = rows.Where(x => x.CycleId is not null)
            .Select(x => x.CycleId!.Value).Distinct().ToArray();
        var labels = new Dictionary<Guid, string>();
        if (cycleIds.Length > 0)
        {
            var names = await (
                from cycle in db.CropCycles.AsNoTracking()
                join plot in db.Plots.AsNoTracking() on cycle.PlotId equals plot.Id
                join type in db.CropTypes.AsNoTracking() on cycle.CropTypeId equals type.Id
                where plot.FarmId == farmId && cycleIds.Contains(cycle.Id)
                select new { cycle.Id, Crop = type.Name, Plot = plot.Name }
            ).ToListAsync(ct);
            labels = names.ToDictionary(x => x.Id,
                x => x.Crop + " · " + x.Plot + " · " + x.Id.ToString("N")[..6]);
        }
        return FarmFinanceInsightRules.Build(farmId, from, to, rows, labels);
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
