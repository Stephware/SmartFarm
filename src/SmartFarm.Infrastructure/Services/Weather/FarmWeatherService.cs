using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Weather;

// Shared read model for MVC and API. Forecast retrieval remains a Worker concern.
public sealed class FarmWeatherService(IFarmAccessService access, SmartFarmDbContext db)
    : IFarmWeatherService
{
    public async Task<WeatherOverview> GetAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct))
            throw new FarmOperationException(FarmError.NotFound, "Resource not found.");

        var farm = await db.Farms.AsNoTracking().SingleOrDefaultAsync(
            f => f.Id == farmId && f.ArchivedAtUtc == null, ct);
        if (farm is null)
            throw new FarmOperationException(FarmError.NotFound, "Resource not found.");

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        try
        {
            today = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
                DateTime.UtcNow, TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId)));
        }
        catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
        {
            // Legacy rows with invalid zones can still view their weather data.
        }

        var forecasts = await db.WeatherForecasts.AsNoTracking()
            .Where(f => f.FarmId == farmId && f.ForecastDate >= today &&
                f.ForecastDate <= today.AddDays(4))
            .OrderByDescending(f => f.RetrievedAtUtc)
            .Take(100).ToListAsync(ct);
        var days = forecasts.GroupBy(f => f.ForecastDate)
            .Select(g => g.OrderByDescending(f => f.RetrievedAtUtc).First())
            .OrderBy(f => f.ForecastDate)
            .Select(f => new WeatherDayDetails(f.ForecastDate, f.MinTemperatureC,
                f.MaxTemperatureC, f.RainfallMm, f.RetrievedAtUtc, f.Provider,
                f.MaxPrecipitationProbability, f.MaxWindKph))
            .ToList();

        var alerts = await db.WeatherAdvisories.AsNoTracking()
            .Where(a => a.FarmId == farmId && a.AdvisoryDate >= today &&
                a.AdvisoryDate <= today.AddDays(4))
            .OrderBy(a => a.AdvisoryDate).ThenByDescending(a => a.Severity)
            .Select(a => new WeatherAdvisoryDetails(
                a.Id, a.AdvisoryDate, a.AdvisoryType, a.Severity,
                a.Status, a.Message, a.CreatedAtUtc))
            .ToListAsync(ct);

        return new WeatherOverview(farmId, farm.Latitude.HasValue && farm.Longitude.HasValue,
            farm.TimeZoneId, days, alerts);
    }
}
