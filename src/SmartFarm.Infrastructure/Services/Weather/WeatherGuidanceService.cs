using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services;
using SmartFarm.Core.Services.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Integrations.Gemini;
using SmartFarm.Infrastructure.Persistence;
namespace SmartFarm.Infrastructure.Services.Weather;

public sealed class WeatherGuidanceService(SmartFarmDbContext db, IFarmAccessService access, UserPreferencesService preferences,
    IWeatherGuidanceProvider provider, IOptions<GeminiOptions> options, TimeProvider clock)
{
    public async Task<WeatherGuidance> GetAsync(Guid farmId, Guid advisoryId, Guid actor, Guid? cropId, CancellationToken ct = default)
    {
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct)) throw Missing();
        var advisory = await db.WeatherAdvisories.AsNoTracking().SingleOrDefaultAsync(a => a.Id == advisoryId && a.FarmId == farmId, ct) ?? throw Missing();
        var forecast = await db.WeatherForecasts.AsNoTracking().SingleOrDefaultAsync(f => f.Id == advisory.WeatherForecastId && f.FarmId == farmId, ct);
        var farm = await db.Farms.AsNoTracking().SingleAsync(f => f.Id == farmId, ct);
        var now = clock.GetUtcNow().UtcDateTime;
        var language = await preferences.LanguageAsync(actor, ct);
        GuidanceCropContext? crop = null;
        DateTime? observedAt = null;
        if (cropId is { } id)
        {
            var record = await (from c in db.CropCycles.AsNoTracking() join p in db.Plots on c.PlotId equals p.Id
                join t in db.CropTypes on c.CropTypeId equals t.Id where c.Id == id && p.FarmId == farmId
                select new { c.Status, p.AreaSqMeters, t.Name }).SingleOrDefaultAsync(ct) ?? throw Missing();
            var observation = await db.CropObservations.AsNoTracking().Where(o => o.CropCycleId == id)
                .OrderByDescending(o => o.ObservedAtUtc).ThenByDescending(o => o.Id)
                .Select(o => new { o.HealthStatus, o.IssueCategory, o.IssueSeverity, o.ObservedAtUtc }).FirstOrDefaultAsync(ct);
            observedAt = observation?.ObservedAtUtc;
            crop = new(GuidanceCatalog.CanonicalCrop(record.Name), record.Status, record.AreaSqMeters,
                observation?.HealthStatus, observation?.IssueCategory, observation?.IssueSeverity, observation?.ObservedAtUtc);
        }
        var today = FarmTime.Today(now, farm.TimeZoneId);
        var stale = forecast?.ForecastDate != advisory.AdvisoryDate || !AlertRules.IsFresh(forecast?.RetrievedAtUtc, now) || advisory.AdvisoryDate < today || advisory.AdvisoryDate.DayNumber > today.DayNumber + 4;
        var synthetic = forecast?.Provider == "SyntheticDemo";
        var context = new GuidanceContext(advisory.AdvisoryType, advisory.Severity, advisory.AdvisoryDate, forecast?.RetrievedAtUtc ?? DateTime.UnixEpoch,
            forecast?.RainfallMm, forecast?.MaxTemperatureC, forecast?.MaxWindKph, language, crop);
        var limitations = new List<string> { "forecast_not_observed_conditions", "advisory_only_no_automatic_changes" };
        if (crop is null) limitations.Add("crop_context_not_selected");
        else
        {
            if (crop.Crop == "other") limitations.Add("crop_type_not_in_guidance_catalog");
            if (observedAt is null) limitations.Add("no_recorded_crop_observation");
            else limitations.Add("observation_is_historical_not_current_reading");
        }
        GuidanceSelection? selected = null;
        if (stale) limitations.Add("weather_stale_or_unavailable");
        if (synthetic) limitations.Add("synthetic_demonstration_data");
        if (!stale && !synthetic && advisory.Status == 0) selected = await provider.SelectAsync(context, ct);
        if (selected is not null && !GuidanceCatalog.IsValid(selected, context)) selected = null;
        if (selected is null) limitations.Add("deterministic_fallback");
        var selection = selected ?? GuidanceCatalog.Fallback(context);
        return new(advisory.Id, cropId, forecast?.Id, language, advisory.Severity, advisory.AdvisoryDate,
            forecast?.Provider, forecast?.RetrievedAtUtc, stale, synthetic, selected is null ? "deterministic" : "gemini",
            selected is null ? null : options.Value.Model,
            selection.ExplanationKeys.Select(k => new GuidanceMessage(k, GuidanceCatalog.Text(k, language))).ToList(),
            selection.RecommendationKeys.Select(k => new GuidanceMessage(k, GuidanceCatalog.Text(k, language))).ToList(), limitations);
    }
    private static FarmOperationException Missing() => new(FarmError.NotFound, "Resource not found.");
}
