using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Infrastructure.Integrations.Weather;

// Implement only after the provider's endpoint, authentication and response contract are confirmed.
public interface IWeatherProviderAdapter
{
    HttpRequestMessage CreateForecastRequest(WeatherLocation location);

    Task<WeatherForecastResult> ReadForecastAsync(
        HttpResponseMessage response,
        CancellationToken cancellationToken);
}
