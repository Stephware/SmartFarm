using SmartFarm.Core.Services.Monitoring;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class MonitoringPhaseFourTests
{
    [Theory]
    [InlineData((byte)0, false)]
    [InlineData((byte)1, true)]
    [InlineData((byte)3, true)]
    [InlineData((byte)5, true)]
    [InlineData((byte)6, false)]
    public void ActivityTypesMatchSqlCheck(byte value, bool accepted) =>
        Assert.Equal(accepted, MonitoringRules.ValidActivity(value));

    [Theory]
    [InlineData((byte)0, true)]
    [InlineData((byte)3, true)]
    [InlineData((byte)4, false)]
    public void ObservationHealthMatchesSqlCheck(byte value, bool accepted) =>
        Assert.Equal(accepted, MonitoringRules.ValidHealth(value));

    [Theory]
    [InlineData((byte)0, false)]
    [InlineData((byte)1, true)]
    [InlineData((byte)2, false)]
    [InlineData((byte)3, false)]
    [InlineData((byte)4, false)]
    public void OnlyActiveCyclesPermitNewRecords(byte status, bool allowed) =>
        Assert.Equal(allowed, MonitoringRules.CanRecord(status));

    [Fact]
    public void PhotoSignaturesMustMatchClaimedImageMime()
    {
        var png = new byte[] { 137,80,78,71,13,10,26,10, 0,0,0 };
        var jpeg = new byte[] { 0xff,0xd8,0xff,0xe0, 0,0 };
        var webp = new byte[] { 82,73,70,70,0,0,0,0,87,69,66,80 };
        Assert.Equal(".png", MonitoringRules.PhotoExtension(png, "image/png"));
        Assert.Equal(".jpg", MonitoringRules.PhotoExtension(jpeg, "image/jpeg"));
        Assert.Equal(".webp", MonitoringRules.PhotoExtension(webp, "image/webp"));
        Assert.Null(MonitoringRules.PhotoExtension(png, "image/jpeg"));
        Assert.Null(MonitoringRules.PhotoExtension(new byte[] {1,2,3}, "image/png"));
        Assert.Null(MonitoringRules.PhotoExtension(jpeg, "application/octet-stream"));
    }
}
