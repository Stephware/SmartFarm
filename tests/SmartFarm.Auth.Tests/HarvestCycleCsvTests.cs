using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Web.Clients;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class HarvestCycleCsvTests
{
    [Fact]
    public void ExportUsesFilteredCycleTotalsAndEscapesSpreadsheetFormulas()
    {
        var cycleId = Guid.NewGuid();
        var report = new HarvestReport(Guid.NewGuid(), "Farm", "Asia/Manila",
            new DateOnly(2026, 10, 1), new DateOnly(2026, 10, 31),
            null, null, 2, 1, [], [], [], [])
        {
            CycleTotals =
            [
                new HarvestCycleTotal(cycleId, Guid.NewGuid(), "=SUM(1,2)",
                    Guid.NewGuid(), "@North", "kg", 3.125m, 2,
                    new DateOnly(2026, 10, 12), new DateOnly(2026, 10, 20)),
                new HarvestCycleTotal(Guid.NewGuid(), Guid.NewGuid(), "Rice",
                    Guid.NewGuid(), "South", "bundles", 7m, 1,
                    new DateOnly(2026, 10, 21), new DateOnly(2026, 10, 21))
            ]
        };
        var output = HarvestCycleCsv.Build(report);
        Assert.StartsWith("Crop cycle ID,Crop,Plot,Unit,", output);
        Assert.Contains(cycleId.ToString("D"), output);
        Assert.Contains("\"'=SUM(1,2)\"", output);
        Assert.Contains("\"'@North\"", output);
        Assert.Contains("3.125", output);
        Assert.Contains("7.000", output);
        Assert.Contains("2026-10-01", output);
        Assert.Contains("2026-10-31", output);
        Assert.Equal(3, output.Split('\n').Length - 1);
    }

    [Fact]
    public void EmptyCycleExportHasHeaderButNoMadeUpData()
    {
        var report = new HarvestReport(Guid.NewGuid(), "Farm", "Asia/Manila",
            null, null, null, null, 0, 0, [], [], [], []);
        var output = HarvestCycleCsv.Build(report);
        Assert.Equal(1, output.Split('\n').Length - 1);
        Assert.DoesNotContain("0.000", output);
    }
}
