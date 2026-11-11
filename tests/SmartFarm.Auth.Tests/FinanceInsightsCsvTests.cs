using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Web.Clients;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FinanceInsightsCsvTests
{
    [Fact]
    public void ExportEscapesUntrustedCategoriesAndIncludesAllBreakdowns()
    {
        var result = new FarmFinanceInsights(Guid.NewGuid(),
            new DateOnly(2026, 10, 1), new DateOnly(2026, 10, 31),
            150m, 70m, 80m, 2,
            [new FarmFinanceCategoryTotal("Expense", "=HYPERLINK(\"evil\")", 70m, 1)],
            [new FarmFinanceMonthlyTotal("2026-10", 150m, 70m, 80m)],
            [new FarmFinanceCropTotal(null, "@untrusted plot", 150m, 70m, 80m, 2)]);
        var csv = FinanceInsightsCsv.Build(result);
        Assert.Contains("2026-10", csv);
        Assert.Contains("150.00", csv);
        Assert.Contains("70.00", csv);
        Assert.Contains("\"'=HYPERLINK(\"\"evil\"\")\"", csv);
        Assert.Contains("\"'@untrusted plot\"", csv);
        Assert.Equal(5, csv.Split('\n').Length - 1);
    }
}
