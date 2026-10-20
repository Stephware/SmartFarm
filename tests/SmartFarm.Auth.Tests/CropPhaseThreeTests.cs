using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Services.Crops;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class CropPhaseThreeTests
{
    [Theory]
    [InlineData(CropCycleStatus.Planned, CropCycleStatus.Active, true)]
    [InlineData(CropCycleStatus.Planned, CropCycleStatus.Cancelled, true)]
    [InlineData(CropCycleStatus.Planned, CropCycleStatus.Completed, false)]
    [InlineData(CropCycleStatus.Planned, CropCycleStatus.Failed, false)]
    [InlineData(CropCycleStatus.Active, CropCycleStatus.Completed, true)]
    [InlineData(CropCycleStatus.Active, CropCycleStatus.Failed, true)]
    [InlineData(CropCycleStatus.Active, CropCycleStatus.Cancelled, true)]
    [InlineData(CropCycleStatus.Active, CropCycleStatus.Planned, false)]
    [InlineData(CropCycleStatus.Completed, CropCycleStatus.Active, false)]
    [InlineData(CropCycleStatus.Failed, CropCycleStatus.Active, false)]
    [InlineData(CropCycleStatus.Cancelled, CropCycleStatus.Active, false)]
    public void AllowedTransitionsMatchSqlStatusModel(
        CropCycleStatus before, CropCycleStatus after, bool allowed)
    {
        Assert.Equal(allowed, CropCycleRules.CanTransition(before, after));
    }

    [Theory]
    [InlineData(CropCycleStatus.Planned, false)]
    [InlineData(CropCycleStatus.Active, false)]
    [InlineData(CropCycleStatus.Completed, true)]
    [InlineData(CropCycleStatus.Failed, true)]
    [InlineData(CropCycleStatus.Cancelled, true)]
    public void TerminalStatusCannotBeReopened(CropCycleStatus status, bool terminal)
    {
        Assert.Equal(terminal, CropCycleRules.IsTerminal(status));
        if (terminal)
            Assert.All(Enum.GetValues<CropCycleStatus>(), next =>
                Assert.False(CropCycleRules.CanTransition(status, next)));
    }

    [Fact]
    public void CropTypeRejectsInvalidNameAndDurations()
    {
        var input = new CropTypeInput
        {
            Name = "",
            DefaultHarvestUnit = "kg",
            DefaultGrowthDays = -1
        };
        var failures = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(input, new ValidationContext(input), failures, true));
    }

    [Fact]
    public void PlanInputRejectsOutOfRangeWateringInterval()
    {
        var input = new CropCyclePlanInput
        {
            CropTypeId = Guid.NewGuid(),
            PlannedPlantingDate = new DateOnly(2026, 10, 17),
            WateringIntervalDays = -1
        };
        var failures = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(input, new ValidationContext(input), failures, true));
    }
}
