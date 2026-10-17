using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Interfaces.Weather;

namespace SmartFarm.Infrastructure.Integrations.Weather;

public static class WeatherServiceCollectionExtensions
{
    public static IHttpClientBuilder AddSmartFarmWeather(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        services.AddOptions<WeatherOptions>()
            .Bind(configuration.GetSection(WeatherOptions.SectionName))
            .Validate(options => options.TimeoutSeconds is >= 1 and <= 120,
                "Weather:TimeoutSeconds must be between 1 and 120.")
            .Validate(options => options.HasValidBaseAddress(),
                "Weather:BaseAddress must be an absolute HTTPS URL without credentials, query or fragment.")
            .Validate(options => options.HasValidApiKeyHeader(),
                "Weather API-key header configuration is invalid; configure both name and key or neither.")
            .ValidateOnStart();

        return services.AddHttpClient<IWeatherService, WeatherHttpClient>((provider, client) =>
            {
                client.BaseAddress = provider.GetRequiredService<IOptions<WeatherOptions>>().Value.BaseAddress;
                // The typed client applies one timeout to sending and parsing the response.
                client.Timeout = Timeout.InfiniteTimeSpan;
            })
            .ConfigurePrimaryHttpMessageHandler(() => new HttpClientHandler { AllowAutoRedirect = false })
            .RedactLoggedHeaders(_ => true)
            .RemoveAllLoggers();
    }
}
