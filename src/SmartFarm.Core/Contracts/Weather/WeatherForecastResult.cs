namespace SmartFarm.Core.Contracts.Weather;

public sealed record WeatherLocation(double Latitude, double Longitude);

public sealed record DailyWeatherForecast(
    DateOnly Date,
    decimal? MinimumTemperatureC,
    decimal? MaximumTemperatureC,
    decimal? RainfallMm);

public sealed record WeatherForecastResult(
    string Provider,
    DateTimeOffset RetrievedAt,
    IReadOnlyList<DailyWeatherForecast> Days);
