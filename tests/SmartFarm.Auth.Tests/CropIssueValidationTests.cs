using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Services.Monitoring;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class CropIssueValidationTests
{
    [Theory]
    [InlineData(0, 0, 0, true)]
    [InlineData(0, 0, 1, true)]
    [InlineData(0, 0, 2, true)]
    [InlineData(1, 1, 2, true)]
    [InlineData(2, 2, 3, true)]
    [InlineData(5, 3, 2, true)]
    [InlineData(1, 0, 2, false)]
    [InlineData(0, 1, 2, false)]
    [InlineData(1, 1, 1, false)]
    [InlineData(1, 1, 0, false)]
    [InlineData(1, 4, 3, false)]
    [InlineData(6, 1, 2, false)]
    public void IssueRequiresAppropriateHealthAndBothCodes(
        byte category, byte severity, byte health, bool expected)
    {
        Assert.Equal(expected, MonitoringRules.ValidIssue(category, severity, health));
    }

    [Theory]
    [InlineData(0, false)]
    [InlineData(1, true)]
    [InlineData(2, true)]
    [InlineData(3, false)]
    public void FollowUpCanOnlyBeOpenOrResolved(byte status, bool valid) =>
        Assert.Equal(valid, MonitoringRules.ValidFollowUp(status));

    [Fact]
    public void OldObservationPayloadStillDefaultsToNoIssue()
    {
        var input = new RecordObservationInput { HealthStatus = 1 };
        Assert.Equal(0, input.IssueCategory);
        Assert.Equal(0, input.IssueSeverity);
        Assert.True(MonitoringRules.ValidIssue(input.IssueCategory,
            input.IssueSeverity, input.HealthStatus));
    }
}
