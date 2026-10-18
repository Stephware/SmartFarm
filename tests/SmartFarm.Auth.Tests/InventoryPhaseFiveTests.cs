using SmartFarm.Core.Enums;
using SmartFarm.Core.Services.Inventory;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class InventoryPhaseFiveTests
{
    [Theory]
    [InlineData("0.000", false)]
    [InlineData("0.001", true)]
    [InlineData("12.345", true)]
    [InlineData("1.0001", false)]
    [InlineData("-1.000", false)]
    public void StockQuantityMustMatchSqlPrecision(string quantity, bool valid) =>
        Assert.Equal(valid, StockRules.IsValidQuantity(decimal.Parse(quantity, System.Globalization.CultureInfo.InvariantCulture)));

    [Theory]
    [InlineData(StockTransactionType.Receipt, 2.000, true)]
    [InlineData(StockTransactionType.Receipt, -2.000, false)]
    [InlineData(StockTransactionType.Adjustment, -2.000, true)]
    [InlineData(StockTransactionType.Adjustment, 0.000, false)]
    [InlineData(StockTransactionType.Consumption, -1.000, false)]
    [InlineData(StockTransactionType.Opening, 1.000, false)]
    [InlineData(StockTransactionType.Reversal, -1.000, false)]
    public void AllowedMovementsFollowLedgerRules(
        StockTransactionType type, string amount, bool allowed) =>
        Assert.Equal(allowed, StockRules.CanPost(type, decimal.Parse(amount, System.Globalization.CultureInfo.InvariantCulture), null));

    [Fact]
    public void ConsumptionRequiresAnActivityAndNegativeChange()
    {
        Assert.True(StockRules.CanPost(StockTransactionType.Consumption, -1.250m, Guid.NewGuid()));
        Assert.False(StockRules.CanPost(StockTransactionType.Consumption, 1.250m, Guid.NewGuid()));
    }

    [Fact]
    public void StockNeverBecomesNegative()
    {
        Assert.False(StockRules.TryBalance(1.000m, -2.000m, out _));
        Assert.True(StockRules.TryBalance(1.000m, -1.000m, out var final));
        Assert.Equal(0m, final);
    }

    [Fact]
    public void ThreeDecimalScaleIsRequired()
    {
        Assert.False(StockRules.CanPost(StockTransactionType.Receipt, 0.0001m, null));
        Assert.False(StockRules.TryBalance(1.000m, 0.0001m, out _));
    }
}
