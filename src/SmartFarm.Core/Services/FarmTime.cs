using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace SmartFarm.Core.Services;

public static class FarmTime
{
    public const string DefaultZone = "Asia/Manila";
    public static TimeZoneInfo Resolve(string? id)
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById(id ?? DefaultZone); }
        catch (Exception e) when (e is TimeZoneNotFoundException or InvalidTimeZoneException)
        { return TimeZoneInfo.FindSystemTimeZoneById(DefaultZone); }
    }
    // SQL datetime2 contains UTC instants but materializes with Unspecified kind.
    public static DateTime Utc(DateTime value) => value.Kind == DateTimeKind.Local
        ? value.ToUniversalTime() : DateTime.SpecifyKind(value, DateTimeKind.Utc);
    public static DateTimeOffset Local(DateTime utc, string? zone) =>
        TimeZoneInfo.ConvertTime(new DateTimeOffset(Utc(utc)), Resolve(zone));
    public static DateOnly Today(DateTime utc, string? zone) => DateOnly.FromDateTime(Local(utc, zone).DateTime);
}

public sealed class UtcDateTimeConverter : JsonConverter<DateTime>
{
    public override DateTime Read(ref Utf8JsonReader reader, Type type, JsonSerializerOptions options)
    {
        var text = reader.GetString();
        if (text is null || !(text.EndsWith('Z') || text.Length >= 6 && (text[^6] is '+' or '-') && text[^3] == ':') ||
            !DateTimeOffset.TryParse(text, CultureInfo.InvariantCulture, DateTimeStyles.None, out var value))
            throw new JsonException("Timestamp requires UTC or an explicit offset.");
        return value.UtcDateTime;
    }
    public override void Write(Utf8JsonWriter writer, DateTime value, JsonSerializerOptions options) =>
        writer.WriteStringValue(FarmTime.Utc(value));
}
