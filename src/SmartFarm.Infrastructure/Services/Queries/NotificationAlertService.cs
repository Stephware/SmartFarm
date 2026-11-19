using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Query;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Services;
using SmartFarm.Core.Services.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Queries;

public sealed class NotificationAlertService(SmartFarmDbContext db, UserPreferencesService preferences, TimeProvider clock)
{
    public async Task<PageResult<AniAlert>> ListAsync(Guid actor, PageRequest p, CancellationToken ct = default)
    {
        try { p.Validate(); } catch (ArgumentException e) { throw new FarmOperationException(FarmError.InvalidInput, e.Message); }
        var language = await preferences.LanguageAsync(actor, ct);
        var now = clock.GetUtcNow().UtcDateTime;
        var q = Visible(actor).Where(n => (p.Type == null || n.Type == p.Type) &&
            (p.Unread == null || (n.ReadAtUtc == null) == p.Unread) &&
            (p.Search == null || n.Title.Contains(p.Search) || n.Message.Contains(p.Search)));
        var total = await q.CountAsync(ct);
        var order = p.Sort == "asc" ? q.OrderBy(n => n.CreatedAtUtc).ThenBy(n => n.Id) : q.OrderByDescending(n => n.CreatedAtUtc).ThenByDescending(n => n.Id);
        var rows = await order.Skip((p.Page - 1) * p.PageSize).Take(p.PageSize).ToListAsync(ct);
        var taskIds = rows.Where(n => n.FarmTaskId.HasValue).Select(n => n.FarmTaskId!.Value).ToList();
        var advisoryIds = rows.Where(n => n.WeatherAdvisoryId.HasValue).Select(n => n.WeatherAdvisoryId!.Value).ToList();
        var tasks = await db.FarmTasks.AsNoTracking().Where(t => taskIds.Contains(t.Id)).ToDictionaryAsync(t => t.Id, ct);
        var advisories = await db.WeatherAdvisories.AsNoTracking().Where(a => advisoryIds.Contains(a.Id)).ToDictionaryAsync(a => a.Id, ct);
        var forecastIds = advisories.Values.Where(a => a.WeatherForecastId.HasValue).Select(a => a.WeatherForecastId!.Value).ToList();
        var forecasts = await db.WeatherForecasts.AsNoTracking().Where(f => forecastIds.Contains(f.Id)).ToDictionaryAsync(f => f.Id, ct);
        var farms = await db.Farms.AsNoTracking().Where(f => f.ArchivedAtUtc == null &&
            (f.OwnerUserId == actor || db.FarmMembers.Any(m => m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null))).ToListAsync(ct);
        var items = new List<AniAlert>();
        foreach (var n in rows)
        {
            var task = n.FarmTaskId is { } t && tasks.TryGetValue(t, out var foundTask) ? foundTask : null;
            var advisory = n.WeatherAdvisoryId is { } a && advisories.TryGetValue(a, out var foundAdvisory) ? foundAdvisory : null;
            var forecast = advisory?.WeatherForecastId is { } f && forecasts.TryGetValue(f, out var foundForecast) ? foundForecast : null;
            Guid? farmId = task?.FarmId ?? advisory?.FarmId ?? farms.FirstOrDefault(f => n.DeduplicationKey.StartsWith($"stock:{f.Id:N}:"))?.Id;
            var farm = farms.FirstOrDefault(f => f.Id == farmId);
            var key = n.Type switch { 1 => "task.reminder", 2 => BackendMessages.WeatherKey(advisory?.AdvisoryType ?? 0), 3 => "stock.low", _ => "enrollment.update" };
            var priority = advisory?.Severity >= 3 ? "high" : advisory?.Severity >= 2 ? "important" : "routine";
            var destination = advisory is not null ? "weather" : task is not null ? "task" : n.Type == 3 ? "supplies" : "enrollment";
            items.Add(new(n.Id, farmId, n.Type, advisory is not null ? "weather" : destination, priority, n.Title, n.Message,
                key, BackendMessages.Text(key, language), forecast?.Provider ?? "AniGo", n.CreatedAtUtc, n.ReadAtUtc, n.ReadAtUtc,
                destination, n.FarmTaskId, n.WeatherAdvisoryId, advisory?.Severity, forecast?.RetrievedAtUtc,
                advisory is not null && !AlertRules.IsFresh(forecast?.RetrievedAtUtc, now),
                advisory is not null && farm is not null && AlertRules.CanInterrupt(advisory.Severity, advisory.Status, n.ReadAtUtc,
                    forecast?.RetrievedAtUtc, advisory.AdvisoryDate, FarmTime.Today(now, farm.TimeZoneId), forecast?.Provider, now)));
        }
        return new(items, p.Page, p.PageSize, total);
    }

    public async Task AcknowledgeAsync(Guid actor, Guid id, CancellationToken ct = default)
    {
        var n = await Visible(actor).SingleOrDefaultAsync(n => n.Id == id, ct) ?? throw new FarmOperationException(FarmError.NotFound, "Resource not found.");
        n.ReadAtUtc ??= clock.GetUtcNow().UtcDateTime;
        await db.SaveChangesAsync(ct);
    }

    private IQueryable<SmartFarm.Core.Entities.Notification> Visible(Guid actor) => NotificationVisibility.ForActor(db, actor);
}
