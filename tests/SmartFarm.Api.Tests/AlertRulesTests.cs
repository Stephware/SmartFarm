using SmartFarm.Core.Services.Tasks;
using Xunit;
namespace SmartFarm.Api.Tests;
public sealed class AlertRulesTests
{
    [Fact]
    public void InterruptRequiresUnreadFreshActiveRealForecastWithinValidity()
    {
        var now = new DateTime(2026, 11, 20, 1, 0, 0, DateTimeKind.Utc);
        var date = new DateOnly(2026, 11, 20);
        Assert.True(AlertRules.CanInterrupt(3, 0, null, now.AddHours(-1), date, date, "OpenMeteo", now));
        Assert.False(AlertRules.CanInterrupt(3, 0, now, now, date, date, "OpenMeteo", now));
        Assert.False(AlertRules.CanInterrupt(3, 1, null, now, date, date, "OpenMeteo", now));
        Assert.False(AlertRules.CanInterrupt(3, 0, null, now.AddHours(-13), date, date, "OpenMeteo", now));
        Assert.False(AlertRules.CanInterrupt(3, 0, null, now.AddMinutes(6), date, date, "OpenMeteo", now));
        Assert.False(AlertRules.CanInterrupt(3, 0, null, null, date, date, null, now));
        Assert.False(AlertRules.CanInterrupt(3, 0, null, now, date.AddDays(-1), date, "OpenMeteo", now));
        Assert.False(AlertRules.CanInterrupt(3, 0, null, now, date, date, "SyntheticDemo", now));
        Assert.False(AlertRules.CanInterrupt(1, 0, null, now, date, date, "OpenMeteo", now));
    }
}
