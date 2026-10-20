using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Core.Services.Tasks;

public sealed record WeatherWarning(byte Type, byte Severity, string Message);

public static class WeatherRules
{
    public static IReadOnlyList<WeatherWarning> Evaluate(DailyWeatherForecast day)
    {
        var results = new List<WeatherWarning>();
        if (day.RainfallMm is >= 20m)
            results.Add(new WeatherWarning(1, day.RainfallMm >= 50m ? (byte)3 : (byte)2,
                $"Heavy rain is forecast ({day.RainfallMm:0.#} mm). Check drainage and outdoor farm work."));
        if (day.MaximumTemperatureC is >= 35m)
            results.Add(new WeatherWarning(3, day.MaximumTemperatureC >= 40m ? (byte)3 : (byte)2,
                $"High temperature is forecast ({day.MaximumTemperatureC:0.#}°C). Monitor crop heat stress."));
        return results;
    }
}
