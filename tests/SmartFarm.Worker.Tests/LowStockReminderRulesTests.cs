using SmartFarm.Worker.Services;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class LowStockReminderRulesTests
{
    [Fact]
    public void WeekKeyIsStableWithinFarmLocalWeekAndChangesNextMonday()
    {
        var farm = Guid.NewGuid();
        var monday = new DateOnly(2026, 10, 26);
        var key = LowStockReminderRules.WeeklyKey(farm, monday);
        Assert.Equal(key, LowStockReminderRules.WeeklyKey(farm, monday.AddDays(6)));
        Assert.NotEqual(key, LowStockReminderRules.WeeklyKey(farm, monday.AddDays(7)));
        Assert.NotEqual(key, LowStockReminderRules.WeeklyKey(Guid.NewGuid(), monday));
        Assert.StartsWith("stock:", key);
        Assert.True(key.Length < 180);
    }
}
