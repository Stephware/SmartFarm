using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Core.Interfaces.Weather;

public interface IWeatherService
{
    Task<WeatherForecastResult> GetForecastAsync(
        WeatherLocation location,
        CancellationToken cancellationToken = default);
}
