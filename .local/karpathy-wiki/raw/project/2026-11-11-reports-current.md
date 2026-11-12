# SmartFarm reports-current source capture

> Source: local repository at ac624be7e5e721f00d990d01f720c682f1977930
> Collected: 2026-11-11
> Published: Unknown

Source evidence only. No build, deployment, SQL or device success is implied.

## `src/SmartFarm.Core/Contracts/Finance/FarmFinanceContracts.cs`

````text
using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Core.Contracts.Finance;

public sealed class FarmFinanceInput
{
    [Required, RegularExpression("Expense|Income")]
    public string Kind { get; set; } = "Expense";
    [Required, StringLength(60, MinimumLength = 2)]
    public string Category { get; set; } = "";
    [Range(typeof(decimal), "0.01", "999999999999.99")]
    public decimal AmountPhp { get; set; }
    public DateOnly EntryDate { get; set; }
    public Guid? CropCycleId { get; set; }
    [StringLength(500)]
    public string? Notes { get; set; }
}

public sealed class FarmFinanceVoidInput
{
    [Required, StringLength(300, MinimumLength = 3)]
    public string Reason { get; set; } = "";
}

public sealed record FarmFinanceEntryDetails(Guid Id, Guid FarmId, Guid? CropCycleId,
    DateOnly EntryDate, string Kind, string Category, decimal AmountPhp, string? Notes,
    DateTime RecordedAtUtc, bool IsVoided, string? VoidReason);

public sealed record FarmFinanceOverview(Guid FarmId, decimal ExpensesPhp,
    decimal IncomePhp, decimal NetPhp, IReadOnlyList<FarmFinanceEntryDetails> Entries);

 
// These figures describe manually entered cash transactions only; they are
// not accounting profit, crop sale estimates, or imputed inventory costs.
public sealed record FarmFinanceCategoryTotal(
    string Kind, string Category, decimal AmountPhp, int EntryCount);

public sealed record FarmFinanceMonthlyTotal(
    string Month, decimal IncomePhp, decimal ExpensesPhp, decimal NetPhp);

public sealed record FarmFinanceCropTotal(
    Guid? CycleId, string Label, decimal IncomePhp,
    decimal ExpensesPhp, decimal NetPhp, int EntryCount);

public sealed record FarmFinanceInsights(
    Guid FarmId, DateOnly? FromDate, DateOnly? ToDate,
    decimal IncomePhp, decimal ExpensesPhp, decimal NetPhp, int EntryCount,
    IReadOnlyList<FarmFinanceCategoryTotal> Categories,
    IReadOnlyList<FarmFinanceMonthlyTotal> Months,
    IReadOnlyList<FarmFinanceCropTotal> CropTotals);
````

## `src/SmartFarm.Core/Contracts/Reports/FarmReportContracts.cs`

````text
namespace SmartFarm.Core.Contracts.Reports;

// Quantities are always grouped by harvest unit; kg, pieces and boxes are not interchangeable.
public sealed record HarvestReportEntry(
    Guid Id, Guid CycleId, Guid PlotId, string PlotName,
    Guid CropTypeId, string CropTypeName, decimal Quantity, string Unit,
    DateTime HarvestedAtUtc, DateOnly FarmDate, bool IsVoided, string? Notes);

public sealed record HarvestUnitTotal(string Unit, decimal Quantity, int HarvestCount);

public sealed record HarvestCropPlotTotal(
    Guid CropTypeId, string CropTypeName, Guid PlotId, string PlotName,
    string Unit, decimal Quantity, int HarvestCount);

public sealed record HarvestMonthlyTotal(string Month, string Unit, decimal Quantity, int HarvestCount);

// Combine repeated partial harvests for the same crop cycle, never across
// unrelated units or different planting cycles on the same plot.
public sealed record HarvestCycleTotal(
    Guid CycleId, Guid CropTypeId, string CropTypeName, Guid PlotId, string PlotName,
    string Unit, decimal Quantity, int HarvestCount,
    DateOnly FirstHarvestDate, DateOnly LastHarvestDate);

public sealed record HarvestReport(
    Guid FarmId, string FarmName, string TimeZoneId,
    DateOnly? FromDate, DateOnly? ToDate, Guid? PlotId, Guid? CropTypeId,
    int ValidHarvestCount, int VoidedHarvestCount,
    IReadOnlyList<HarvestUnitTotal> UnitTotals,
    IReadOnlyList<HarvestCropPlotTotal> CropPlotTotals,
    IReadOnlyList<HarvestMonthlyTotal> MonthlyTotals,
    IReadOnlyList<HarvestReportEntry> Entries)
{
    // Additive field: older clients and manually created reports remain valid.
    public IReadOnlyList<HarvestCycleTotal> CycleTotals { get; init; } = [];
}

public sealed record DashboardAttentionItem(
    string Kind, Guid TargetId, string Title, string Detail, DateOnly? DueDate = null);

public sealed record FarmDashboard(
    Guid FarmId, string FarmName, DateOnly FarmToday,
    int ActivePlots, int PlannedCrops, int ActiveCrops, int CompletedCrops,
    int OpenTasks, int OverdueTasks, int LowStockItems, int ConcerningObservations,
    IReadOnlyList<HarvestUnitTotal> HarvestLast30Days,
    IReadOnlyList<HarvestReportEntry> RecentHarvests)
{
    // Read-only drill-downs. Existing response fields remain unchanged for Android.
    public IReadOnlyList<DashboardAttentionItem> AttentionItems { get; init; } = [];
}
````

## `src/SmartFarm.Infrastructure/Services/Finance/FarmFinanceService.cs`

````text
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
````

## `src/SmartFarm.Infrastructure/Services/Reports/FarmReportService.cs`

````text
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Reports;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Reports;

public sealed class FarmReportService(SmartFarmDbContext db, IFarmAccessService access)
    : IFarmReportService
{
    private static FarmOperationException NotFound() =>
        new(FarmError.NotFound, "Farm not found.");
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);

    private async Task<Farm> RequireFarm(Guid farmId, Guid actor, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct)) throw NotFound();
        return await db.Farms.AsNoTracking().SingleOrDefaultAsync(
            f => f.Id == farmId && f.ArchivedAtUtc == null, ct) ?? throw NotFound();
    }

    private static TimeZoneInfo Zone(Farm farm)
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId); }
        catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
        { return TimeZoneInfo.Utc; }
    }

    private static DateOnly FarmDate(DateTime utc, TimeZoneInfo zone) =>
        DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
            DateTime.SpecifyKind(utc, DateTimeKind.Utc), zone));

    private static DateTime BeginningUtc(DateOnly day, TimeZoneInfo zone) =>
        TimeZoneInfo.ConvertTimeToUtc(
            DateTime.SpecifyKind(day.ToDateTime(TimeOnly.MinValue), DateTimeKind.Unspecified),
            zone);

    public async Task<HarvestReport> HarvestsAsync(Guid farmId, Guid actor,
        DateOnly? from = null, DateOnly? to = null,
        Guid? plotId = null, Guid? cropTypeId = null, CancellationToken ct = default)
    {
        var farm = await RequireFarm(farmId, actor, ct);
        var earliest = new DateOnly(2000, 1, 1);
        var latest = new DateOnly(2100, 12, 30);
        if (from is { } start && (start < earliest || start > latest) ||
            to is { } end && (end < earliest || end > latest) ||
            from is not null && to is not null && from.Value > to.Value ||
            plotId == Guid.Empty || cropTypeId == Guid.Empty)
            throw Invalid("Choose valid report dates between 2000 and 2100, with From before To.");

        var zone = Zone(farm);
        // Filter with the farm's local calendar day boundaries. The end is exclusive
        // so every harvest on the selected end date is included, including late evenings.
        DateTime? startUtc = from is { } first ? BeginningUtc(first, zone) : null;
        DateTime? endUtc = to is { } last ? BeginningUtc(last.AddDays(1), zone) : null;
        var query =
            from harvest in db.Harvests.AsNoTracking()
            join crop in db.CropCycles.AsNoTracking() on harvest.CropCycleId equals crop.Id
            join plot in db.Plots.AsNoTracking() on crop.PlotId equals plot.Id
            join type in db.CropTypes.AsNoTracking() on crop.CropTypeId equals type.Id
            where plot.FarmId == farmId
                && (plotId == null || plot.Id == plotId.Value)
                && (cropTypeId == null || type.Id == cropTypeId.Value)
                && (startUtc == null || harvest.HarvestedAtUtc >= startUtc.Value)
                && (endUtc == null || harvest.HarvestedAtUtc < endUtc.Value)
            select new
            {
                harvest.Id, CycleId = crop.Id, PlotId = plot.Id, PlotName = plot.Name,
                CropTypeId = type.Id, CropTypeName = type.Name,
                harvest.Quantity, harvest.Unit, harvest.HarvestedAtUtc,
                harvest.VoidedAtUtc, harvest.Notes
            };
        var raw = await query.ToListAsync(ct);
        var records = raw.Select(row => new HarvestReportEntry(
            row.Id, row.CycleId, row.PlotId, row.PlotName,
            row.CropTypeId, row.CropTypeName, row.Quantity, row.Unit,
            row.HarvestedAtUtc, FarmDate(row.HarvestedAtUtc, zone),
            row.VoidedAtUtc is not null, row.Notes));
        return HarvestReportRules.Build(farmId, farm.Name, farm.TimeZoneId,
            from, to, plotId, cropTypeId, records);
    }

    public async Task<FarmDashboard> DashboardAsync(Guid farmId, Guid actor,
        CancellationToken ct = default)
    {
        var farm = await RequireFarm(farmId, actor, ct);
        var today = FarmDate(DateTime.UtcNow, Zone(farm));
        var activePlots = await db.Plots.AsNoTracking().CountAsync(
            p => p.FarmId == farmId && p.ArchivedAtUtc == null, ct);
        var cropStatuses = await (
            from crop in db.CropCycles.AsNoTracking()
            join plot in db.Plots.AsNoTracking() on crop.PlotId equals plot.Id
            where plot.FarmId == farmId
            select crop.Status).ToListAsync(ct);
        var openTasks = await db.FarmTasks.AsNoTracking().CountAsync(
            task => task.FarmId == farmId && task.Status == 0, ct);
        var overdueTasks = await db.FarmTasks.AsNoTracking().CountAsync(
            task => task.FarmId == farmId && task.Status == 0 && task.DueDate < today, ct);
        var lowStock = await db.InventoryItems.AsNoTracking().CountAsync(
            item => item.FarmId == farmId && item.ArchivedAtUtc == null &&
                item.ReorderLevel != null && item.QuantityOnHand <= item.ReorderLevel, ct);
        // Count only currently concerning active crops, not older observations
        // that may have been followed by a healthy inspection.
        var concerning = await (
            from obs in db.CropObservations.AsNoTracking()
            join cycle in db.CropCycles.AsNoTracking() on obs.CropCycleId equals cycle.Id
            join plot in db.Plots.AsNoTracking() on cycle.PlotId equals plot.Id
            where plot.FarmId == farmId && plot.ArchivedAtUtc == null &&
                cycle.Status == (byte)CropCycleStatus.Active && obs.HealthStatus >= 2 &&
                !db.CropObservations.AsNoTracking().Any(newer =>
                    newer.CropCycleId == cycle.Id && newer.ObservedAtUtc > obs.ObservedAtUtc)
            select cycle.Id).Distinct().CountAsync(ct);
        // Focused drill-downs: fetch a bounded set in each category to avoid
        // expensive N+1 lookups and keep every item scoped to the current farm.
        var late = await db.FarmTasks.AsNoTracking()
            .Where(t => t.FarmId == farmId && t.Status == 0 && t.DueDate < today)
            .OrderBy(t => t.DueDate).ThenBy(t => t.Id)
            .Select(t => new { t.Id, t.TaskType, t.DueDate, t.Description })
            .Take(4).ToListAsync(ct);

        var shortages = await db.InventoryItems.AsNoTracking()
            .Where(i => i.FarmId == farmId && i.ArchivedAtUtc == null &&
                i.ReorderLevel != null && i.QuantityOnHand <= i.ReorderLevel)
            .OrderBy(i => i.QuantityOnHand).ThenBy(i => i.Name)
            .Select(i => new { i.Id, i.Name, i.QuantityOnHand, i.ReorderLevel, i.Unit })
            .Take(4).ToListAsync(ct);

        var inspections = await (
            from observation in db.CropObservations.AsNoTracking()
            join cycle in db.CropCycles.AsNoTracking()
                on observation.CropCycleId equals cycle.Id
            join plot in db.Plots.AsNoTracking() on cycle.PlotId equals plot.Id
            join type in db.CropTypes.AsNoTracking() on cycle.CropTypeId equals type.Id
            where plot.FarmId == farmId && plot.ArchivedAtUtc == null &&
                cycle.Status == (byte)CropCycleStatus.Active && observation.HealthStatus >= 2 &&
                !db.CropObservations.AsNoTracking().Any(newer =>
                    newer.CropCycleId == cycle.Id &&
                    newer.ObservedAtUtc > observation.ObservedAtUtc)
            orderby observation.ObservedAtUtc descending
            select new { CycleId = cycle.Id, PlotName = plot.Name,
                CropName = type.Name, observation.HealthStatus, observation.ObservedAtUtc })
            .Take(30).ToListAsync(ct);

        var attention = new List<DashboardAttentionItem>();
        attention.AddRange(late.Select(t => new DashboardAttentionItem("task", t.Id,
            FarmCodeLabels.Task(t.TaskType) + " overdue",
            t.Description ?? "Scheduled farm task needs attention.", t.DueDate)));
        attention.AddRange(shortages.Select(i => new DashboardAttentionItem("inventory", i.Id,
            i.Name + " · Low stock",
            $"{i.QuantityOnHand:N3} {i.Unit} remaining; threshold {i.ReorderLevel:N3} {i.Unit}.")));
        attention.AddRange(inspections.GroupBy(i => i.CycleId)
            .Select(g => g.First()).Take(4)
            .Select(i => new DashboardAttentionItem("inspection", i.CycleId,
                i.CropName + " · " + i.PlotName,
                FarmCodeLabels.Health(i.HealthStatus) + " at the latest crop inspection.")));

        var recent = await HarvestsAsync(farmId, actor, today.AddDays(-29), today, ct: ct);
        return new FarmDashboard(
            farmId, farm.Name, today, activePlots,
            cropStatuses.Count(s => s == 0), cropStatuses.Count(s => s == 1),
            cropStatuses.Count(s => s == 2),
            openTasks, overdueTasks, lowStock, concerning,
            recent.UnitTotals, recent.Entries.Where(e => !e.IsVoided).Take(5).ToList())
        {
            AttentionItems = attention
        };
    }
}
````

## `src/SmartFarm.Core/Services/Reports/HarvestReportRules.cs`

````text
using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Core.Services.Reports;

public static class HarvestReportRules
{
    public static HarvestReport Build(Guid farmId, string farmName, string timeZoneId,
        DateOnly? from, DateOnly? to, Guid? plotId, Guid? cropTypeId,
        IEnumerable<HarvestReportEntry> harvests)
    {
        var entries = harvests.OrderByDescending(h => h.HarvestedAtUtc)
            .ThenByDescending(h => h.Id).ToList();
        var valid = entries.Where(h => !h.IsVoided).ToList();
        var units = valid.GroupBy(h => h.Unit)
            .Select(g => new HarvestUnitTotal(g.Key, g.Sum(h => h.Quantity), g.Count()))
            .OrderBy(g => g.Unit).ToList();
        var crops = valid.GroupBy(h => new
            { h.CropTypeId, h.CropTypeName, h.PlotId, h.PlotName, h.Unit })
            .Select(g => new HarvestCropPlotTotal(g.Key.CropTypeId, g.Key.CropTypeName,
                g.Key.PlotId, g.Key.PlotName, g.Key.Unit,
                g.Sum(h => h.Quantity), g.Count()))
            .OrderBy(g => g.CropTypeName).ThenBy(g => g.PlotName)
            .ThenBy(g => g.Unit).ToList();
        var monthly = valid.GroupBy(h => new
            { Month = h.FarmDate.ToString("yyyy-MM"), h.Unit })
            .Select(g => new HarvestMonthlyTotal(g.Key.Month, g.Key.Unit,
                g.Sum(h => h.Quantity), g.Count()))
            .OrderByDescending(g => g.Month).ThenBy(g => g.Unit).ToList();
        var cycles = valid.GroupBy(h => new
            { h.CycleId, h.CropTypeId, h.CropTypeName, h.PlotId, h.PlotName, h.Unit })
            .Select(g => new HarvestCycleTotal(
                g.Key.CycleId, g.Key.CropTypeId, g.Key.CropTypeName,
                g.Key.PlotId, g.Key.PlotName, g.Key.Unit,
                g.Sum(h => h.Quantity), g.Count(),
                g.Min(h => h.FarmDate), g.Max(h => h.FarmDate)))
            .OrderBy(c => c.Unit).ThenBy(c => c.CropTypeName)
            .ThenByDescending(c => c.LastHarvestDate).ThenBy(c => c.CycleId)
            .ToList();
        return new HarvestReport(farmId, farmName, timeZoneId,
            from, to, plotId, cropTypeId, valid.Count, entries.Count - valid.Count,
            units, crops, monthly, entries)
        {
            CycleTotals = cycles
        };
    }
}
````

## `src/SmartFarm.Core/Services/Finance/FarmFinanceInsightRules.cs`

````text
using SmartFarm.Core.Contracts.Finance;

namespace SmartFarm.Core.Services.Finance;

// Each input row is an aggregate from SQL, not a truncated history page.
public sealed record FarmFinanceAggregate(
    int Year, int Month, string Kind, string Category,
    Guid? CycleId, decimal AmountPhp, int Count);

public static class FarmFinanceInsightRules
{
    public static FarmFinanceInsights Build(Guid farmId, DateOnly? from, DateOnly? to,
        IEnumerable<FarmFinanceAggregate> rows,
        IReadOnlyDictionary<Guid, string>? cycleLabels = null)
    {
        var data = rows.ToList();
        var income = data.Where(x => x.Kind == "Income").Sum(x => x.AmountPhp);
        var expenses = data.Where(x => x.Kind == "Expense").Sum(x => x.AmountPhp);
        var categories = data.GroupBy(x => new { x.Kind, x.Category })
            .Select(g => new FarmFinanceCategoryTotal(g.Key.Kind, g.Key.Category,
                g.Sum(x => x.AmountPhp), g.Sum(x => x.Count)))
            .OrderBy(x => x.Kind).ThenByDescending(x => x.AmountPhp)
            .ThenBy(x => x.Category).ToList();
        var months = data.GroupBy(x => new { x.Year, x.Month })
            .Select(g =>
            {
                var inMonth = g.Where(x => x.Kind == "Income").Sum(x => x.AmountPhp);
                var outMonth = g.Where(x => x.Kind == "Expense").Sum(x => x.AmountPhp);
                return new FarmFinanceMonthlyTotal($"{g.Key.Year:0000}-{g.Key.Month:00}",
                    inMonth, outMonth, inMonth - outMonth);
            })
            .OrderByDescending(x => x.Month).ToList();
        var cycles = data.GroupBy(x => x.CycleId).Select(g =>
        {
            var inCycle = g.Where(x => x.Kind == "Income").Sum(x => x.AmountPhp);
            var outCycle = g.Where(x => x.Kind == "Expense").Sum(x => x.AmountPhp);
            var label = g.Key is { } id
                ? (cycleLabels?.GetValueOrDefault(id) ?? "Crop cycle " + id.ToString("N")[..6])
                : "Farm-wide / unassigned";
            return new FarmFinanceCropTotal(g.Key, label,
                inCycle, outCycle, inCycle - outCycle, g.Sum(x => x.Count));
        }).OrderByDescending(x => x.IncomePhp + x.ExpensesPhp)
            .ThenBy(x => x.Label).ToList();
        return new FarmFinanceInsights(farmId, from, to, income, expenses,
            income - expenses, data.Sum(x => x.Count), categories, months, cycles);
    }
}
````

## `src/SmartFarm.Api/Controllers/FarmReportsController.cs`

````text
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}")]
public sealed class FarmReportsController(
    UserManager<SmartFarmUser> users, IFarmReportService reports)
    : FarmApiControllerBase(users)
{
    [HttpGet("dashboard")]
    public Task<IActionResult> Dashboard(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await reports.DashboardAsync(farmId, ActorId, ct)));

    [HttpGet("reports/harvests")]
    public Task<IActionResult> Harvests(Guid farmId, [FromQuery] DateOnly? from,
        [FromQuery] DateOnly? to, [FromQuery] Guid? plotId,
        [FromQuery] Guid? cropTypeId, CancellationToken ct) =>
        Result(async () => Ok(await reports.HarvestsAsync(
            farmId, ActorId, from, to, plotId, cropTypeId, ct)));
}
````

## `src/SmartFarm.Api/Controllers/FarmFinanceController.cs`

````text
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/finance")]
public sealed class FarmFinanceController(
    UserManager<SmartFarmUser> users, IFarmFinanceService finance)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await finance.ListAsync(farmId, ActorId, ct)));

    [HttpGet("insights")]
    public Task<IActionResult> Insights(Guid farmId,
        [FromQuery] DateOnly? from, [FromQuery] DateOnly? to, CancellationToken ct) =>
        Result(async () => Ok(await finance.InsightsAsync(farmId, ActorId, from, to, ct)));

    [HttpPost]
    public Task<IActionResult> Record(Guid farmId, [FromBody] FarmFinanceInput input,
        CancellationToken ct) =>
        Result(async () => Ok(await finance.RecordAsync(farmId, ActorId, input, ct)));

    [HttpPost("{entryId:guid}/void")]
    public Task<IActionResult> Void(Guid farmId, Guid entryId,
        [FromBody] FarmFinanceVoidInput input, CancellationToken ct) =>
        Result(async () =>
        {
            await finance.VoidAsync(farmId, entryId, ActorId, input, ct);
            return NoContent();
        });
}
````

## `src/SmartFarm.Web/Controllers/ReportsController.cs`

````text
using System.Text;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.Clients;
using SmartFarm.Web.ViewModels.Reports;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class ReportsController(
    UserManager<SmartFarmUser> users, IFarmReportService reports,
    IFarmManagementService farms, ICropManagementService crops) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id)
        ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Harvests(Guid farmId, DateOnly? from,
        DateOnly? to, Guid? plotId, Guid? cropTypeId, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await reports.HarvestsAsync(farmId, Actor, from, to, plotId, cropTypeId, ct);
            var plots = await farms.ListPlotsAsync(farmId, Actor, ct);
            var types = await crops.ListCropTypesAsync(farmId, Actor, ct);
            return View(new HarvestReportPage(report, plots, types));
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound) { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }

    // Export the same filter-scoped, per-cycle summaries shown in the report.
    [HttpGet]
    public async Task<IActionResult> ExportCycleCsv(Guid farmId, DateOnly? from,
        DateOnly? to, Guid? plotId, Guid? cropTypeId, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await reports.HarvestsAsync(farmId, Actor,
                from, to, plotId, cropTypeId, ct);
            return File(Encoding.UTF8.GetBytes(HarvestCycleCsv.Build(report)),
                "text/csv; charset=utf-8",
                $"smartfarm-cycle-yields-{farmId:N}.csv");
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound)
        { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }

    // Read-only report download; the API checks farm membership before any data is returned.
    [HttpGet]
    public async Task<IActionResult> ExportCsv(Guid farmId, DateOnly? from,
        DateOnly? to, Guid? plotId, Guid? cropTypeId, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await reports.HarvestsAsync(farmId, Actor, from, to, plotId, cropTypeId, ct);
            var content = HarvestCsv.Build(report);
            return File(Encoding.UTF8.GetBytes(content), "text/csv; charset=utf-8",
                $"smartfarm-harvests-{farmId:N}.csv");
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound) { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }
}
````

## `src/SmartFarm.Web/Controllers/FarmFinanceController.cs`

````text
using System.Text;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Finance;
using SmartFarm.Web.Clients;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class FarmFinanceController(
    UserManager<SmartFarmUser> users, IFarmFinanceService finance,
    IFarmManagementService farms, ICropManagementService crops) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct,
        DateOnly? from = null, DateOnly? to = null)
    {
        try
        {
            var farm = await farms.GetAsync(farmId, Actor, ct);
            if (farm is null) return NotFound();
            var overview = await finance.ListAsync(farmId, Actor, ct);
            var insights = await finance.InsightsAsync(farmId, Actor, from, to, ct);
            return View(new FarmFinancePage(farm, overview,
                await crops.ListCyclesAsync(farmId, Actor, ct))
            {
                Insights = insights
            });
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound)
        { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }

    [HttpGet]
    public async Task<IActionResult> ExportInsightsCsv(Guid farmId, CancellationToken ct,
        DateOnly? from = null, DateOnly? to = null)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await finance.InsightsAsync(farmId, Actor, from, to, ct);
            return File(Encoding.UTF8.GetBytes(FinanceInsightsCsv.Build(report)),
                "text/csv; charset=utf-8", $"smartfarm-finance-{farmId:N}.csv");
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound)
        { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Record(Guid farmId, FarmFinanceInput input,
        CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Check the entry date, category and amount.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try
        {
            await finance.RecordAsync(farmId, Actor, input, ct);
            TempData["Message"] = "Financial entry recorded.";
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Void(Guid farmId, Guid entryId,
        FarmFinanceVoidInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Provide a reason to void this entry.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try
        {
            await finance.VoidAsync(farmId, entryId, Actor, input, ct);
            TempData["Message"] = "Entry voided; history retained.";
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }
}
````

## `src/SmartFarm.Web/Clients/HarvestCycleCsv.cs`

````text
using System.Globalization;
using System.Text;
using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Web.Clients;

// Export the exact filtered planting-cycle totals shown on the report page.
// Keep dates and units explicit: filtered partial totals are not lifetime yields.
public static class HarvestCycleCsv
{
    public static string Build(HarvestReport report)
    {
        var csv = new StringBuilder();
        csv.AppendLine("Crop cycle ID,Crop,Plot,Unit,First harvest date,Last harvest date,Harvest entries,Recorded quantity,Filter from,Filter to");
        foreach (var cycle in report.CycleTotals)
        {
            csv.Append(HarvestCsv.Cell(cycle.CycleId.ToString("D"))).Append(',')
                .Append(HarvestCsv.Cell(cycle.CropTypeName)).Append(',')
                .Append(HarvestCsv.Cell(cycle.PlotName)).Append(',')
                .Append(HarvestCsv.Cell(cycle.Unit)).Append(',')
                .Append(HarvestCsv.Cell(cycle.FirstHarvestDate.ToString("yyyy-MM-dd"))).Append(',')
                .Append(HarvestCsv.Cell(cycle.LastHarvestDate.ToString("yyyy-MM-dd"))).Append(',')
                .Append(cycle.HarvestCount.ToString(CultureInfo.InvariantCulture)).Append(',')
                .Append(cycle.Quantity.ToString("0.000", CultureInfo.InvariantCulture)).Append(',')
                .Append(HarvestCsv.Cell(report.FromDate?.ToString("yyyy-MM-dd") ?? "All dates")).Append(',')
                .AppendLine(HarvestCsv.Cell(report.ToDate?.ToString("yyyy-MM-dd") ?? "All dates"));
        }
        return csv.ToString();
    }
}
````

## `src/SmartFarm.Web/Clients/FinanceInsightsCsv.cs`

````text
using System.Globalization;
using System.Text;
using SmartFarm.Core.Contracts.Finance;

namespace SmartFarm.Web.Clients;

public static class FinanceInsightsCsv
{
    private static string Money(decimal amount) => amount.ToString("0.00", CultureInfo.InvariantCulture);
    private static string Cell(string? value) => HarvestCsv.Cell(value);

    public static string Build(FarmFinanceInsights insight)
    {
        var csv = new StringBuilder();
        csv.AppendLine("Section,Period or allocation,Type,Category,Entry count,Income PHP,Expenses PHP,Net PHP,Category amount PHP");
        csv.Append("Summary,")
            .Append(Cell((insight.FromDate?.ToString("yyyy-MM-dd") ?? "All dates") + " to " +
                (insight.ToDate?.ToString("yyyy-MM-dd") ?? "All dates")))
            .Append(",,,").Append(insight.EntryCount).Append(',')
            .Append(Money(insight.IncomePhp)).Append(',').Append(Money(insight.ExpensesPhp))
            .Append(',').AppendLine(Money(insight.NetPhp) + ",");
        foreach (var month in insight.Months.OrderBy(x => x.Month))
            csv.Append("Month,").Append(Cell(month.Month)).Append(",,,0,")
                .Append(Money(month.IncomePhp)).Append(',').Append(Money(month.ExpensesPhp))
                .Append(',').AppendLine(Money(month.NetPhp) + ",");
        foreach (var category in insight.Categories)
            csv.Append("Category,,").Append(Cell(category.Kind)).Append(',')
                .Append(Cell(category.Category)).Append(',').Append(category.EntryCount)
                .Append(",,,,").AppendLine(Money(category.AmountPhp));
        foreach (var crop in insight.CropTotals)
            csv.Append("Crop cycle,").Append(Cell(crop.Label)).Append(",,,")
                .Append(crop.EntryCount).Append(',').Append(Money(crop.IncomePhp))
                .Append(',').Append(Money(crop.ExpensesPhp)).Append(',')
                .AppendLine(Money(crop.NetPhp) + ",");
        return csv.ToString();
    }
}
````

## `database/06_FarmFinance.sql`

````text
/* AniGo Phase 3: manual farm cash ledger. Back up SQL Server before applying.
   Safe to rerun. No existing table or inventory record is changed. */
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'SmartFarmDB'
    THROW 50060, 'Select SmartFarmDB before applying the farm finance upgrade.', 1;
IF OBJECT_ID(N'dbo.Farm', N'U') IS NULL OR OBJECT_ID(N'dbo.CropCycle', N'U') IS NULL
    THROW 50061, 'Apply the existing SmartFarm schema before this upgrade.', 1;
IF OBJECT_ID(N'dbo.FarmFinanceEntry', N'U') IS NULL
BEGIN
    BEGIN TRANSACTION;
    CREATE TABLE dbo.FarmFinanceEntry (
        Id uniqueidentifier NOT NULL CONSTRAINT PK_FarmFinanceEntry PRIMARY KEY,
        FarmId uniqueidentifier NOT NULL,
        CropCycleId uniqueidentifier NULL,
        RecordedByUserId uniqueidentifier NOT NULL,
        EntryDate date NOT NULL,
        Kind nvarchar(10) NOT NULL,
        Category nvarchar(60) NOT NULL,
        AmountPhp decimal(14,2) NOT NULL,
        Notes nvarchar(500) NULL,
        RecordedAtUtc datetime2(3) NOT NULL CONSTRAINT DF_FarmFinanceEntry_RecordedAt DEFAULT SYSUTCDATETIME(),
        VoidedAtUtc datetime2(3) NULL,
        VoidedByUserId uniqueidentifier NULL,
        VoidReason nvarchar(300) NULL,
        CONSTRAINT CK_FarmFinanceEntry_Kind CHECK (Kind IN (N'Expense', N'Income')),
        CONSTRAINT CK_FarmFinanceEntry_Amount CHECK (AmountPhp > 0),
        CONSTRAINT CK_FarmFinanceEntry_Void CHECK (
            (VoidedAtUtc IS NULL AND VoidedByUserId IS NULL AND VoidReason IS NULL) OR
            (VoidedAtUtc IS NOT NULL AND VoidedByUserId IS NOT NULL AND VoidReason IS NOT NULL)),
        CONSTRAINT FK_FarmFinanceEntry_Farm FOREIGN KEY(FarmId) REFERENCES dbo.Farm(Id),
        CONSTRAINT FK_FarmFinanceEntry_CropCycle FOREIGN KEY(CropCycleId) REFERENCES dbo.CropCycle(Id),
        CONSTRAINT FK_FarmFinanceEntry_RecordedBy FOREIGN KEY(RecordedByUserId) REFERENCES dbo.AspNetUsers(Id),
        CONSTRAINT FK_FarmFinanceEntry_VoidedBy FOREIGN KEY(VoidedByUserId) REFERENCES dbo.AspNetUsers(Id)
    );
    CREATE INDEX IX_FarmFinanceEntry_Farm_Date ON dbo.FarmFinanceEntry(FarmId, EntryDate);
    CREATE INDEX IX_FarmFinanceEntry_Cycle ON dbo.FarmFinanceEntry(CropCycleId);
    CREATE INDEX IX_FarmFinanceEntry_RecordedBy ON dbo.FarmFinanceEntry(RecordedByUserId);
    COMMIT TRANSACTION;
END;
````
