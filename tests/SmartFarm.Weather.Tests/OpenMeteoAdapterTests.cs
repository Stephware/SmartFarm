using System.Net;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Infrastructure.Integrations.Weather;
using Xunit;

namespace SmartFarm.Weather.Tests;

public sealed class OpenMeteoAdapterTests
{
    [Fact]
    public void BuildsForecastRequestUsingInvariantCoordinates()
    {
        var adapter = new OpenMeteoAdapter();
        using var request = adapter.CreateForecastRequest(new WeatherLocation(14.6, 121.1));

        Assert.Contains("latitude=14.6", request.RequestUri!.OriginalString);
        Assert.Contains("longitude=121.1", request.RequestUri.OriginalString);
        Assert.Contains("precipitation_sum", request.RequestUri.OriginalString);
        Assert.Contains("precipitation_probability_max", request.RequestUri.OriginalString);
        Assert.Contains("wind_speed_10m_max", request.RequestUri.OriginalString);
    }

    [Fact]
    public async Task ReadsDailyForecast()
    {
        var adapter = new OpenMeteoAdapter();
        using var response = new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                """{"daily":{"time":["2026-10-20"],"temperature_2m_min":[23.5],"temperature_2m_max":[36.2],"precipitation_sum":[25.0]}}""")
        };

        var result = await adapter.ReadForecastAsync(response, CancellationToken.None);
        var day = Assert.Single(result.Days);

        Assert.Equal("Open-Meteo", result.Provider);
        Assert.Equal(new DateOnly(2026, 10, 20), day.Date);
        Assert.Equal(36.2m, day.MaximumTemperatureC);
        Assert.Equal(25m, day.RainfallMm);
    }

    [Fact]
    public async Task ReadsRainChanceAndWindWhenPresent()
    {
        var adapter = new OpenMeteoAdapter();
        using var response = new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                """{"daily":{"time":["2026-10-20"],"temperature_2m_min":[23.5],"temperature_2m_max":[36.2],"precipitation_sum":[25.0],"precipitation_probability_max":[80],"wind_speed_10m_max":[65.3]}}""")
        };

        var day = Assert.Single((await adapter.ReadForecastAsync(
            response, CancellationToken.None)).Days);
        Assert.Equal(80m, day.MaxPrecipitationProbability);
        Assert.Equal(65.3m, day.MaxWindKph);
    }

    [Fact]
    public async Task MalformedOptionalWeatherNumbersAreRejected()
    {
        var adapter = new OpenMeteoAdapter();
        using var response = new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(
                """{"daily":{"time":["2026-10-20"],"temperature_2m_min":[23.5],"temperature_2m_max":[36.2],"precipitation_sum":[25.0],"wind_speed_10m_max":["invalid"]}}""")
        };

        await Assert.ThrowsAsync<System.Text.Json.JsonException>(() =>
            adapter.ReadForecastAsync(response, CancellationToken.None));
    }

    [Fact]
    public async Task MissingDailyFieldsAreRejectedAsInvalidWeather()
    {
        var adapter = new OpenMeteoAdapter();
        using var response = new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent("""{"daily":{"time":["2026-10-20"]}}""")
        };
        await Assert.ThrowsAsync<System.Text.Json.JsonException>(() =>
            adapter.ReadForecastAsync(response, CancellationToken.None));
    }

    [Fact]
    public void ProviderRequiresExpectedHttpsOrigin()
    {
        var services = new ServiceCollection();
        var configuration = new ConfigurationBuilder().AddInMemoryCollection(
            new Dictionary<string, string?>
            {
                ["Weather:Provider"] = "OpenMeteo",
                ["Weather:BaseAddress"] = "https://wrong.example/"
            }).Build();

        Assert.Throws<InvalidOperationException>(() => services.AddSmartFarmWeather(configuration));
    }

    [Fact]
    public void ProviderRegistrationIsOptional()
    {
        var services = new ServiceCollection();
        services.AddSmartFarmWeather(new ConfigurationBuilder().Build());

        Assert.DoesNotContain(services, s => s.ServiceType == typeof(IWeatherProviderAdapter));
    }
}
