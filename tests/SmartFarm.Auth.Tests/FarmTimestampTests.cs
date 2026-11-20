using System.Globalization;
using SmartFarm.Web.Presentation;
using Xunit;
namespace SmartFarm.Auth.Tests;
public class FarmTimestampTests
{
    [Theory]
    [InlineData(null, "Nov 21, 2026 · 12:05 AM")]
    [InlineData("Asia/Manila", "Nov 21, 2026 · 12:05 AM")]
    [InlineData("America/New_York", "Nov 20, 2026 · 11:05 AM")]
    public void SqlUnspecifiedUtcUsesFarmZoneAcrossMidnight(string? zone, string expected) =>
        Assert.Equal(expected, FarmTimestamp.Display(new DateTime(2026,11,20,16,5,0), zone, CultureInfo.GetCultureInfo("en")));
}
