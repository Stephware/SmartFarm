using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Web.Clients;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class HarvestCsvTests
{
    [Theory]
    [InlineData("Tomato, red", "\"Tomato, red\"")]
    [InlineData("Text \"quoted\"", "\"Text \"\"quoted\"\"\"")]
    [InlineData("=SUM(1,2)", "\"'=SUM(1,2)\"")]
    [InlineData("+cmd", "\"'+cmd\"")]
    [InlineData("@formula", "\"'@formula\"")]
    [InlineData("  =SUM(1,2)", "\"'  =SUM(1,2)\"")]
    public void CsvEscapesCellsAndPreventsFormulas(string input, string expected) =>
        Assert.Equal(expected, HarvestCsv.Cell(input));

    [Fact]
    public void CsvIncludesVoidedHistoryWithoutChangingReportTotals()
    {
        var farm = Guid.NewGuid();
        var crop = Guid.NewGuid();
        var report = new HarvestReport(farm, "Farm", "Asia/Manila",
            null, null, null, null, 1, 1,
            new[] { new HarvestUnitTotal("kg", 2m, 1) },
            Array.Empty<HarvestCropPlotTotal>(),
            Array.Empty<HarvestMonthlyTotal>(),
            new[]
            {
                new HarvestReportEntry(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(),
                    "North", crop, "Tomatoes", 2m, "kg",
                    DateTime.UtcNow, new DateOnly(2026, 10, 20), false, "Healthy"),
                new HarvestReportEntry(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(),
                    "South", crop, "Tomatoes", 5m, "kg",
                    DateTime.UtcNow, new DateOnly(2026, 10, 20), true, "=FORMULA()")
            });
        var output = HarvestCsv.Build(report);
        Assert.Contains("2.000", output);
        Assert.Contains("5.000", output);
        Assert.Contains("\"Voided\"", output);
        Assert.Contains("\"'=FORMULA()\"", output);
        Assert.Equal(3, output.Split('\n').Length - 1);
    }
}
