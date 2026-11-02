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
