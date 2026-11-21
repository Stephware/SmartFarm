namespace SmartFarm.Core.Services.Tasks;

public static class AlertRules
{
    public static bool IsFresh(DateTime? retrieved, DateTime now) => retrieved is { } at &&
        FarmTime.Utc(at) >= FarmTime.Utc(now).AddHours(-12) && FarmTime.Utc(at) <= FarmTime.Utc(now).AddMinutes(5);
    public static bool CanInterrupt(byte severity, byte status, DateTime? readAt, DateTime? retrieved,
        DateOnly date, DateOnly today, string? provider, DateTime now) => severity >= 2 && status == 0 && readAt is null &&
        IsFresh(retrieved, now) && date >= today && date.DayNumber <= today.DayNumber + 4 &&
        !string.IsNullOrWhiteSpace(provider) && provider != "SyntheticDemo";
}
