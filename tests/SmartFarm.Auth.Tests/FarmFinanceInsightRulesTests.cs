using SmartFarm.Core.Services.Finance;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FarmFinanceInsightRulesTests
{
    [Fact]
    public void PeriodAggregatesPreserveMonthlyCategoriesAndUnassignedCash()
    {
        var farmId = Guid.NewGuid();
        var crop = Guid.NewGuid();
        var rows = new[]
        {
            new FarmFinanceAggregate(2026, 10, "Income", "Crop sale", crop, 1400m, 2),
            new FarmFinanceAggregate(2026, 10, "Expense", "Fertilizer", crop, 230m, 1),
            new FarmFinanceAggregate(2026, 10, "Expense", "Labor", null, 150m, 2),
            new FarmFinanceAggregate(2026, 11, "Income", "Other", null, 50m, 1),
            new FarmFinanceAggregate(2026, 11, "Expense", "Fertilizer", crop, 20m, 1)
        };
        var report = FarmFinanceInsightRules.Build(farmId, null, null,
            rows, new Dictionary<Guid, string> { [crop] = "Rice · North plot" });
        Assert.Equal(1450m, report.IncomePhp);
        Assert.Equal(400m, report.ExpensesPhp);
        Assert.Equal(1050m, report.NetPhp);
        Assert.Equal(7, report.EntryCount);
        Assert.Equal(250m, report.Categories.Single(x => x.Category == "Fertilizer")
            .AmountPhp);
        Assert.Equal(1400m, report.Months.Single(x => x.Month == "2026-10").IncomePhp);
        Assert.Equal(30m, report.Months.Single(x => x.Month == "2026-11").NetPhp);
        var assigned = report.CropTotals.Single(x => x.CycleId == crop);
        Assert.Equal(1400m, assigned.IncomePhp);
        Assert.Equal(250m, assigned.ExpensesPhp);
        Assert.Equal(1150m, assigned.NetPhp);
        var unassigned = report.CropTotals.Single(x => x.CycleId is null);
        Assert.Equal("Farm-wide / unassigned", unassigned.Label);
        Assert.Equal(-100m, unassigned.NetPhp);
    }

    [Fact]
    public void EmptyPeriodHasZeroTotals()
    {
        var insight = FarmFinanceInsightRules.Build(Guid.NewGuid(),
            new DateOnly(2026, 1, 1), new DateOnly(2026, 1, 31), []);
        Assert.Equal(0m, insight.NetPhp);
        Assert.Equal(0, insight.EntryCount);
        Assert.Empty(insight.Months);
        Assert.Empty(insight.Categories);
        Assert.Empty(insight.CropTotals);
    }
}
