using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.ViewModels.Admin;

namespace SmartFarm.Web.Services.Admin;

public sealed class AdminReportService(SmartFarmDbContext db)
{
    public async Task<AdminPlatformReportPage> BuildAsync(int? requestedDays, CancellationToken ct)
    {
        var days = AdminReportRules.NormalizeDays(requestedDays);
        var now = DateTime.UtcNow;
        // Include today plus (days - 1) preceding UTC calendar days.
        var start = now.Date.AddDays(1 - days);
        var end = now.Date.AddDays(1);

        var registered = await db.Users.AsNoTracking().CountAsync(ct);
        var disabled = await db.Users.AsNoTracking().CountAsync(u => u.DisabledAtUtc != null, ct);
        var farmTotal = await db.Farms.AsNoTracking().CountAsync(ct);
        var activeFarms = await db.Farms.AsNoTracking().CountAsync(f => f.ArchivedAtUtc == null, ct);
        var plots = await db.Plots.AsNoTracking().CountAsync(p => p.ArchivedAtUtc == null &&
            db.Farms.Any(f => f.Id == p.FarmId && f.ArchivedAtUtc == null), ct);
        var cycles = await db.CropCycles.AsNoTracking().CountAsync(c => c.Status == 1 &&
            db.Plots.Any(p => p.Id == c.PlotId && p.ArchivedAtUtc == null &&
                db.Farms.Any(f => f.Id == p.FarmId && f.ArchivedAtUtc == null)), ct);
        var openSupport = await db.SupportTickets.AsNoTracking().CountAsync(t =>
            t.Status == "Open" || t.Status == "InProgress", ct);
        var resolvedSupport = await db.SupportTickets.AsNoTracking().CountAsync(t =>
            t.ResolvedAtUtc >= start && t.ResolvedAtUtc < end, ct);

        var farmDaily = await db.Farms.AsNoTracking()
            .Where(f => f.CreatedAtUtc >= start && f.CreatedAtUtc < end)
            .GroupBy(f => f.CreatedAtUtc.Date)
            .Select(g => new { Date = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Date, g => g.Count, ct);
        var harvestDaily = await db.Harvests.AsNoTracking()
            .Where(h => h.VoidedAtUtc == null && h.HarvestedAtUtc >= start && h.HarvestedAtUtc < end)
            .GroupBy(h => h.HarvestedAtUtc.Date)
            .Select(g => new { Date = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Date, g => g.Count, ct);
        var supportDaily = await db.SupportTickets.AsNoTracking()
            .Where(t => t.CreatedAtUtc >= start && t.CreatedAtUtc < end)
            .GroupBy(t => t.CreatedAtUtc.Date)
            .Select(g => new { Date = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Date, g => g.Count, ct);
        var syncDaily = await db.SyncOperations.AsNoTracking()
            .Where(s => s.Status == 1 && s.ReceivedAtUtc >= start && s.ReceivedAtUtc < end)
            .GroupBy(s => s.ReceivedAtUtc.Date)
            .Select(g => new { Date = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Date, g => g.Count, ct);

        var rows = Enumerable.Range(0, days).Select(offset =>
        {
            var dateTime = start.AddDays(offset);
            return new AdminReportDay(DateOnly.FromDateTime(dateTime),
                farmDaily.GetValueOrDefault(dateTime),
                harvestDaily.GetValueOrDefault(dateTime),
                supportDaily.GetValueOrDefault(dateTime),
                syncDaily.GetValueOrDefault(dateTime));
        }).ToArray();
        return new AdminPlatformReportPage(
            days, DateOnly.FromDateTime(start), DateOnly.FromDateTime(end.AddDays(-1)), now,
            registered, disabled, farmTotal, activeFarms, plots, cycles, openSupport,
            rows.Sum(x => x.Farms), rows.Sum(x => x.HarvestEvents),
            rows.Sum(x => x.SupportRequests), resolvedSupport,
            rows.Sum(x => x.SuccessfulSyncReceipts), rows);
    }
}
