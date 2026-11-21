using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Services.Reports;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class HarvestReportRulesTests
{
    private static HarvestReportEntry Entry(Guid plot, Guid crop, string unit,
        decimal amount, string farmDate, bool voided = false, string cropName = "Tomato") =>
        new(Guid.NewGuid(), Guid.NewGuid(), plot, "North field",
            crop, cropName, amount, unit, DateTime.SpecifyKind(
                DateTime.Parse(farmDate), DateTimeKind.Utc),
            DateOnly.Parse(farmDate), voided, null);

    [Fact]
    public void KeepsUnitsSeparateAndVoidedEntriesOutOfTotals()
    {
        var plot = Guid.NewGuid();
        var crop = Guid.NewGuid();
        var report = HarvestReportRules.Build(Guid.NewGuid(), "My farm", "Asia/Manila",
            null, null, null, null, new[]
            {
                Entry(plot, crop, "kg", 3m, "2026-10-01"),
                Entry(plot, crop, "kg", 2m, "2026-10-02"),
                Entry(plot, crop, "pcs", 4m, "2026-10-02"),
                Entry(plot, crop, "kg", 40m, "2026-10-03", voided: true)
            });

        Assert.Equal(3, report.ValidHarvestCount);
        Assert.Equal(1, report.VoidedHarvestCount);
        Assert.Equal(4, report.Entries.Count);
        Assert.Equal(5m, report.UnitTotals.Single(g => g.Unit == "kg").Quantity);
        Assert.Equal(4m, report.UnitTotals.Single(g => g.Unit == "pcs").Quantity);
        Assert.Equal(2, report.CropPlotTotals.Count);
        Assert.Equal(2, report.MonthlyTotals.Count);
        Assert.Equal(5m, report.MonthlyTotals.Single(g => g.Unit == "kg").Quantity);
    }

    [Fact]
    public void SeparatesPlotsAndUsesFarmLocalMonth()
    {
        var type = Guid.NewGuid();
        var first = Guid.NewGuid();
        var second = Guid.NewGuid();
        var entries = new[]
        {
            Entry(first, type, "kg", 3m, "2026-10-31"),
            Entry(second, type, "kg", 2m, "2026-11-01")
        };
        var report = HarvestReportRules.Build(Guid.NewGuid(), "Farm", "Asia/Manila",
            null, null, null, null, entries);
        Assert.Equal(2, report.CropPlotTotals.Count);
        Assert.Equal(2, report.MonthlyTotals.Count);
        Assert.Contains(report.MonthlyTotals, group => group.Month == "2026-10" && group.Quantity == 3m);
        Assert.Contains(report.MonthlyTotals, group => group.Month == "2026-11" && group.Quantity == 2m);
    }

    [Fact]
    public void RepeatedPartialHarvestsAreCombinedOnlyWithinTheSameCycleAndUnit()
    {
        var farm = Guid.NewGuid();
        var crop = Guid.NewGuid();
        var plot = Guid.NewGuid();
        var firstCycle = Guid.NewGuid();
        var secondCycle = Guid.NewGuid();

        HarvestReportEntry Make(Guid cycle, decimal amount, string unit,
            string date, bool voided = false) =>
            new(Guid.NewGuid(), cycle, plot, "North", crop, "Tomato", amount,
                unit, DateTime.SpecifyKind(DateTime.Parse(date), DateTimeKind.Utc),
                DateOnly.Parse(date), voided, null);

        var report = HarvestReportRules.Build(farm, "Farm", "Asia/Manila",
            new DateOnly(2026, 9, 1), new DateOnly(2026, 12, 1),
            plot, crop, new[]
            {
                Make(firstCycle, 2m, "kg", "2026-10-01"),
                Make(firstCycle, 3m, "kg", "2026-10-05"),
                Make(firstCycle, 500m, "kg", "2026-10-06", true),
                Make(secondCycle, 4m, "kg", "2026-11-01"),
                Make(secondCycle, 8m, "pieces", "2026-11-02")
            });

        Assert.Equal(3, report.CycleTotals.Count);
        var first = Assert.Single(report.CycleTotals.Where(c =>
            c.CycleId == firstCycle && c.Unit == "kg"));
        Assert.Equal(5m, first.Quantity);
        Assert.Equal(2, first.HarvestCount);
        Assert.Equal(new DateOnly(2026, 10, 1), first.FirstHarvestDate);
        Assert.Equal(new DateOnly(2026, 10, 5), first.LastHarvestDate);
        Assert.Equal(4m, Assert.Single(report.CycleTotals.Where(c =>
            c.CycleId == secondCycle && c.Unit == "kg")).Quantity);
        Assert.Equal(8m, Assert.Single(report.CycleTotals.Where(c =>
            c.CycleId == secondCycle && c.Unit == "pieces")).Quantity);
    }

    [Fact]
    public void EmptyHarvestReportShowsNoTotals()
    {
        var report = HarvestReportRules.Build(Guid.NewGuid(), "Empty farm", "UTC",
            null, null, null, null, Array.Empty<HarvestReportEntry>());
        Assert.Empty(report.UnitTotals);
        Assert.Empty(report.CropPlotTotals);
        Assert.Empty(report.MonthlyTotals);
        Assert.Empty(report.CycleTotals);
        Assert.Empty(report.Entries);
    }
}
