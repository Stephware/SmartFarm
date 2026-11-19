using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Query;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services;
using SmartFarm.Core.Services.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Weather;

public sealed class WeatherFeedService(SmartFarmDbContext db, IFarmAccessService access, UserPreferencesService preferences, TimeProvider clock)
{
    public async Task<WeatherFeed> ReadAsync(Guid farmId, Guid actor, string category, PageRequest p, CancellationToken ct = default)
    {
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct)) throw new FarmOperationException(FarmError.NotFound, "Resource not found.");
        try { p.Validate(); } catch (ArgumentException e) { throw new FarmOperationException(FarmError.InvalidInput, e.Message); }
        if (category is not ("recent" or "active" or "upcoming" or "resolved" or "history"))
            throw new FarmOperationException(FarmError.InvalidInput, "Unknown weather category.");
        var farm = await db.Farms.AsNoTracking().SingleAsync(f => f.Id == farmId, ct);
        var now = clock.GetUtcNow().UtcDateTime;
        var today = FarmTime.Today(now, farm.TimeZoneId);
        var language = await preferences.LanguageAsync(actor, ct);
        PageResult<WeatherRecord> result;
        if (category is "active" or "resolved")
        {
            var q = from a in db.WeatherAdvisories.AsNoTracking()
                join f in db.WeatherForecasts.AsNoTracking() on a.WeatherForecastId equals f.Id into forecasts
                from f in forecasts.DefaultIfEmpty()
                where a.FarmId == farmId && a.Status == (category == "active" ? 0 : 1) &&
                    (p.From == null || a.AdvisoryDate >= p.From) && (p.To == null || a.AdvisoryDate <= p.To) &&
                    (p.Type == null || a.AdvisoryType == p.Type) && (p.Search == null || a.Message.Contains(p.Search))
                select new { Advisory = a, Forecast = f };
            var count = await q.CountAsync(ct);
            var order = p.Sort == "asc" ? q.OrderBy(a => a.Advisory.AdvisoryDate).ThenBy(a => a.Advisory.Id) : q.OrderByDescending(a => a.Advisory.AdvisoryDate).ThenByDescending(a => a.Advisory.Id);
            var rows = await order.Skip((p.Page - 1) * p.PageSize).Take(p.PageSize).ToListAsync(ct);
            result = new(rows.Select(row => new WeatherRecord(row.Advisory.Id, row.Advisory.AdvisoryDate, category,
                row.Advisory.AdvisoryType, row.Advisory.Severity, row.Advisory.Status, BackendMessages.WeatherKey(row.Advisory.AdvisoryType),
                BackendMessages.Text(BackendMessages.WeatherKey(row.Advisory.AdvisoryType), language), row.Forecast?.Provider,
                row.Forecast?.RetrievedAtUtc, !AlertRules.IsFresh(row.Forecast?.RetrievedAtUtc, now), row.Forecast?.Provider == "SyntheticDemo", false,
                row.Forecast?.MinTemperatureC, row.Forecast?.MaxTemperatureC, row.Forecast?.RainfallMm, row.Forecast?.MaxWindKph, row.Advisory.ResolvedAtUtc)).ToList(), p.Page, p.PageSize, count);
        }
        else
        {
            var q = db.WeatherForecasts.AsNoTracking().Where(f => f.FarmId == farmId &&
                (category != "upcoming" || f.ForecastDate >= today) && (category != "history" || f.ForecastDate < today) &&
                (p.From == null || f.ForecastDate >= p.From) && (p.To == null || f.ForecastDate <= p.To) &&
                (p.Search == null || f.Provider.Contains(p.Search)));
            var count = await q.CountAsync(ct);
            var order = p.Sort == "asc" ? q.OrderBy(f => f.RetrievedAtUtc).ThenBy(f => f.Id) : q.OrderByDescending(f => f.RetrievedAtUtc).ThenByDescending(f => f.Id);
            var rows = await order.Skip((p.Page - 1) * p.PageSize).Take(p.PageSize).ToListAsync(ct);
            result = new(rows.Select(f => new WeatherRecord(f.Id, f.ForecastDate, category, null, null, null, null, null, f.Provider,
                f.RetrievedAtUtc, !AlertRules.IsFresh(f.RetrievedAtUtc, now), f.Provider == "SyntheticDemo", false,
                f.MinTemperatureC, f.MaxTemperatureC, f.RainfallMm, f.MaxWindKph, null)).ToList(), p.Page, p.PageSize, count);
        }
        return new(farmId, farm.TimeZoneId, today, farm.Latitude.HasValue && farm.Longitude.HasValue, category, "weather.caution", result);
    }
}
