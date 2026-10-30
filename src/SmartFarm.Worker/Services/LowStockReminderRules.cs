namespace SmartFarm.Worker.Services;

public static class LowStockReminderRules
{
    // Stable across repeated worker runs in the same farm-local calendar week.
    public static string WeeklyKey(Guid farmId, DateOnly farmToday)
    {
        var daysSinceMonday = ((int)farmToday.DayOfWeek + 6) % 7;
        var weekStart = farmToday.AddDays(-daysSinceMonday);
        return $"stock:{farmId:N}:{weekStart:yyyyMMdd}";
    }
}
