using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Infrastructure.Services.Weather;
namespace SmartFarm.Infrastructure.Integrations.Gemini;
public static class GeminiRegistration
{
    public static IServiceCollection AddWeatherGuidance(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddOptions<GeminiOptions>().Bind(configuration.GetSection("Gemini")).Validate(o => o.Valid(), "Invalid Gemini configuration.").ValidateOnStart();
        services.AddSingleton<GeminiGuidanceState>();
        services.AddHttpClient<IWeatherGuidanceProvider, GeminiGuidanceClient>(client => client.Timeout = Timeout.InfiniteTimeSpan)
            .ConfigurePrimaryHttpMessageHandler(() => new HttpClientHandler { AllowAutoRedirect = false })
            .RedactLoggedHeaders(_ => true).RemoveAllLoggers();
        services.AddScoped<WeatherGuidanceService>();
        return services;
    }
}
