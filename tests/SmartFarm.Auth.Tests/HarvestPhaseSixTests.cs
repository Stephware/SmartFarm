using System.ComponentModel.DataAnnotations;
using System.Globalization;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Services.Harvests;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class HarvestPhaseSixTests
{
    [Theory]
    [InlineData("0", false)]
    [InlineData("-1", false)]
    [InlineData("0.001", true)]
    [InlineData("1.235", true)]
    [InlineData("2.0000", true)]
    [InlineData("0.0001", false)]
    [InlineData("999999999999999.999", true)]
    [InlineData("1000000000000000", false)]
    public void HarvestQuantityMustFitDecimal18Scale3(string value, bool valid)
    {
        var quantity = decimal.Parse(value, CultureInfo.InvariantCulture);
        Assert.Equal(valid, HarvestRules.IsValidQuantity(quantity));
    }

    [Theory]
    [InlineData((byte)0, false)]
    [InlineData((byte)1, true)]
    [InlineData((byte)2, false)]
    [InlineData((byte)3, false)]
    [InlineData((byte)4, false)]
    public void OnlyActiveCropCyclesAcceptHarvests(byte status, bool allowed) =>
        Assert.Equal(allowed, HarvestRules.CanHarvest(status));

    [Theory]
    [InlineData("", false)]
    [InlineData(" a ", false)]
    [InlineData("Oops", true)]
    [InlineData("Incorrect quantity", true)]
    public void VoidRequiresMeaningfulReason(string reason, bool valid) =>
        Assert.Equal(valid, HarvestRules.IsValidVoidReason(reason));

    [Fact]
    public void HarvestRecordRequiresPositiveQuantity()
    {
        var input = new RecordHarvestInput { Quantity = 0m };
        var failures = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(
            input, new ValidationContext(input), failures, validateAllProperties: true));
    }

    [Fact]
    public void VoidRecordRequiresReason()
    {
        var input = new VoidHarvestInput { Reason = "" };
        var failures = new List<ValidationResult>();
        Assert.False(Validator.TryValidateObject(
            input, new ValidationContext(input), failures, validateAllProperties: true));
    }
}

