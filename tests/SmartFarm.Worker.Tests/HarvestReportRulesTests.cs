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
    public void EmptyHarvestReportShowsNoTotals()
    {
        var report = HarvestReportRules.Build(Guid.NewGuid(), "Empty farm", "UTC",
            null, null, null, null, Array.Empty<HarvestReportEntry>());
        Assert.Empty(report.UnitTotals);
        Assert.Empty(report.CropPlotTotals);
        Assert.Empty(report.MonthlyTotals);
        Assert.Empty(report.Entries);
    }
}
