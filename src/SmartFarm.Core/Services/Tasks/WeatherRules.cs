using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Core.Services.Tasks;

public sealed record WeatherWarning(byte Type, byte Severity, string Message);

public static class WeatherRules
{
    // Reject implausible provider values before writing decimal SQL columns.
    public static bool IsPlausible(DailyWeatherForecast day) =>
        (day.MinimumTemperatureC is null or >= -100m and <= 100m) &&
        (day.MaximumTemperatureC is null or >= -100m and <= 100m) &&
        (day.MinimumTemperatureC is null || day.MaximumTemperatureC is null ||
            day.MinimumTemperatureC <= day.MaximumTemperatureC) &&
        (day.RainfallMm is null or >= 0m and <= 10000m) &&
        (day.MaxPrecipitationProbability is null or >= 0m and <= 100m) &&
        (day.MaxWindKph is null or >= 0m and <= 300m);

    // Missing provider readings are not evidence that an existing warning has cleared.
    public static bool HasSignal(DailyWeatherForecast day, byte warningType) => warningType switch
    {
        1 => day.RainfallMm.HasValue,
        2 => day.MaxWindKph.HasValue,
        3 => day.MaximumTemperatureC.HasValue,
        _ => false
    };

    public static IReadOnlyList<WeatherWarning> Evaluate(DailyWeatherForecast day)
    {
        var results = new List<WeatherWarning>();
        if (day.RainfallMm is >= 20m)
            results.Add(new WeatherWarning(1, day.RainfallMm >= 50m ? (byte)3 : (byte)2,
                $"Heavy rain is forecast ({day.RainfallMm:0.#} mm). Check drainage and outdoor farm work."));
        if (day.MaxWindKph is >= 50m)
            results.Add(new WeatherWarning(2, day.MaxWindKph >= 80m ? (byte)3 : (byte)2,
                $"Strong wind is forecast ({day.MaxWindKph:0.#} km/h). Secure equipment and assess outdoor work."));
        if (day.MaximumTemperatureC is >= 35m)
            results.Add(new WeatherWarning(3, day.MaximumTemperatureC >= 40m ? (byte)3 : (byte)2,
                $"High temperature is forecast ({day.MaximumTemperatureC:0.#}°C). Monitor crop heat stress."));
        return results;
    }
}
