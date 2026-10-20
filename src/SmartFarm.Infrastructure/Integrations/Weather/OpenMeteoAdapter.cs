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
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum," +
            "precipitation_probability_max,wind_speed_10m_max" +
            "&timezone=auto&forecast_days=4";
        return new HttpRequestMessage(HttpMethod.Get, path);
    }

    public async Task<WeatherForecastResult> ReadForecastAsync(
        HttpResponseMessage response, CancellationToken cancellationToken)
    {
        await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
        using var json = await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
        if (json.RootElement.ValueKind != JsonValueKind.Object ||
            !json.RootElement.TryGetProperty("daily", out var daily) ||
            daily.ValueKind != JsonValueKind.Object ||
            !daily.TryGetProperty("time", out var dates) ||
            !daily.TryGetProperty("temperature_2m_min", out var lows) ||
            !daily.TryGetProperty("temperature_2m_max", out var highs) ||
            !daily.TryGetProperty("precipitation_sum", out var rain))
            throw new JsonException("Missing required daily weather data.");
        if (dates.ValueKind != JsonValueKind.Array ||
            lows.ValueKind != JsonValueKind.Array ||
            highs.ValueKind != JsonValueKind.Array ||
            rain.ValueKind != JsonValueKind.Array)
            throw new JsonException("Daily weather fields must be arrays.");
        var probability = daily.TryGetProperty("precipitation_probability_max", out var rainChance)
            ? rainChance : (JsonElement?)null;
        var wind = daily.TryGetProperty("wind_speed_10m_max", out var windSpeed)
            ? windSpeed : (JsonElement?)null;

        if (probability is { } chanceArray &&
                (chanceArray.ValueKind != JsonValueKind.Array ||
                 chanceArray.GetArrayLength() != dates.GetArrayLength()) ||
            wind is { } windArray &&
                (windArray.ValueKind != JsonValueKind.Array ||
                 windArray.GetArrayLength() != dates.GetArrayLength()))
            throw new JsonException("Inconsistent optional daily weather data.");

        if (dates.GetArrayLength() != lows.GetArrayLength() ||
            dates.GetArrayLength() != highs.GetArrayLength() ||
            dates.GetArrayLength() != rain.GetArrayLength())
            throw new JsonException("Inconsistent daily weather data.");

        var days = new List<DailyWeatherForecast>();
        for (var i = 0; i < dates.GetArrayLength(); i++)
        {
            if (dates[i].ValueKind != JsonValueKind.String)
                throw new JsonException("Invalid daily date.");
            var date = DateOnly.ParseExact(dates[i].GetString()!, "yyyy-MM-dd", CultureInfo.InvariantCulture);
            days.Add(new DailyWeatherForecast(
                date, Number(lows[i]), Number(highs[i]), Number(rain[i]),
                probability is { } chance ? Number(chance[i]) : null,
                wind is { } speed ? Number(speed[i]) : null));
        }
        return new WeatherForecastResult("Open-Meteo", DateTimeOffset.UtcNow, days);
    }

    private static decimal? Number(JsonElement value) => value.ValueKind switch
    {
        JsonValueKind.Null => null,
        JsonValueKind.Number => value.GetDecimal(),
        _ => throw new JsonException("Invalid numeric weather value.")
    };
}
