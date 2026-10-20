namespace SmartFarm.Core.Contracts.Weather;

// Saved forecasts and alerts, not a live request to the provider.
public sealed record WeatherDayDetails(
    DateOnly Date, decimal? MinTemperatureC, decimal? MaxTemperatureC,
    decimal? RainfallMm, DateTime RetrievedAtUtc, string Provider);

public sealed record WeatherAdvisoryDetails(
    Guid Id, DateOnly Date, byte Type, byte Severity, byte Status,
    string Message, DateTime CreatedAtUtc);

public sealed record WeatherOverview(
    Guid FarmId, bool HasCoordinates, string TimeZoneId,
    IReadOnlyList<WeatherDayDetails> Days,
    IReadOnlyList<WeatherAdvisoryDetails> Advisories);
