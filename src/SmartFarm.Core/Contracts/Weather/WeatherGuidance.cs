namespace SmartFarm.Core.Contracts.Weather;

// Provider context deliberately excludes user names, coordinates, notes, plot names, IDs and raw growth text.
public sealed record GuidanceCropContext(string Crop, byte Lifecycle, decimal AreaSqMeters, byte? RecordedHealth,
    byte? RecordedIssue, byte? RecordedSeverity, DateTime? ObservedAtUtc = null);
public sealed record GuidanceContext(byte AdvisoryType, byte Severity, DateOnly ForecastDate, DateTime RetrievedAtUtc,
    decimal? RainfallMm, decimal? MaxTemperatureC, decimal? MaxWindKph, string Language, GuidanceCropContext? Crop);
public sealed record GuidanceSelection(string[] ExplanationKeys, string[] RecommendationKeys);
public interface IWeatherGuidanceProvider
{
    Task<GuidanceSelection?> SelectAsync(GuidanceContext context, CancellationToken ct = default);
}
public sealed record GuidanceMessage(string Key, string Text);
public sealed record WeatherGuidance(Guid AdvisoryId, Guid? CropCycleId, Guid? ForecastId, string Language,
    byte Severity, DateOnly ForecastDate, string? WeatherProvider, DateTime? RetrievedAtUtc,
    bool IsStale, bool IsSynthetic, string GenerationSource, string? Model,
    IReadOnlyList<GuidanceMessage> Explanations, IReadOnlyList<GuidanceMessage> Recommendations,
    IReadOnlyList<string> Limitations, string CautionKey = "weather.caution");
