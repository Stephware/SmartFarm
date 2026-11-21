using SmartFarm.Core.Contracts.Query;
namespace SmartFarm.Core.Contracts.Weather;

public sealed record WeatherRecord(Guid Id, DateOnly Date, string Category, byte? Type, byte? Severity,
    byte? Status, string? MessageKey, string? Message, string? Provider, DateTime? RetrievedAtUtc,
    bool IsStale, bool IsSynthetic, bool IsOfficialWarning, decimal? MinTemperatureC,
    decimal? MaxTemperatureC, decimal? RainfallMm, decimal? MaxWindKph, DateTime? ResolvedAtUtc);
public sealed record WeatherFeed(Guid FarmId, string TimeZoneId, DateOnly FarmToday, bool HasCoordinates,
    string Category, string CautionKey, PageResult<WeatherRecord> Records);
public sealed record AniAlert(Guid Id, Guid? FarmId, byte Type, string Category, string Priority,
    string Title, string Message, string MessageKey, string LocalizedMessage, string Source,
    DateTime CreatedAtUtc, DateTime? ReadAtUtc, DateTime? AcknowledgedAtUtc, string Destination,
    Guid? TaskId, Guid? AdvisoryId, byte? Severity, DateTime? SourceRetrievedAtUtc, bool IsStale, bool ShouldInterrupt);
