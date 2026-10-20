using System.Globalization;
using System.Net.Http;
using System.Text.Json;
using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Infrastructure.Integrations.Weather;

// Optional no-key provider. Configure Weather:Provider and Weather:BaseAddress to enable.
public sealed class OpenMeteoAdapter : IWeatherProviderAdapter
{
    public HttpRequestMessage CreateForecastRequest(WeatherLocation location)
    {
        var lat = location.Latitude.ToString(CultureInfo.InvariantCulture);
        var lon = location.Longitude.ToString(CultureInfo.InvariantCulture);
        var path = $"v1/forecast?latitude={lat}&longitude={lon}" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum" +
            "&timezone=auto&forecast_days=4";
        return new HttpRequestMessage(HttpMethod.Get, path);
    }

    public async Task<WeatherForecastResult> ReadForecastAsync(
        HttpResponseMessage response, CancellationToken cancellationToken)
    {
        await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
        using var json = await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
        var daily = json.RootElement.GetProperty("daily");
        var dates = daily.GetProperty("time");
        var lows = daily.GetProperty("temperature_2m_min");
        var highs = daily.GetProperty("temperature_2m_max");
        var rain = daily.GetProperty("precipitation_sum");

        if (dates.GetArrayLength() != lows.GetArrayLength() ||
            dates.GetArrayLength() != highs.GetArrayLength() ||
            dates.GetArrayLength() != rain.GetArrayLength())
            throw new JsonException("Inconsistent daily weather data.");

        var days = new List<DailyWeatherForecast>();
        for (var i = 0; i < dates.GetArrayLength(); i++)
        {
            var date = DateOnly.ParseExact(dates[i].GetString()!, "yyyy-MM-dd", CultureInfo.InvariantCulture);
            days.Add(new DailyWeatherForecast(
                date, Number(lows[i]), Number(highs[i]), Number(rain[i])));
        }
        return new WeatherForecastResult("Open-Meteo", DateTimeOffset.UtcNow, days);
    }

    private static decimal? Number(JsonElement value) =>
        value.ValueKind == JsonValueKind.Null ? null : value.GetDecimal();
}
