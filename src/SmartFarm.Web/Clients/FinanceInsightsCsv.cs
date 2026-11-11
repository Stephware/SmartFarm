using System.Globalization;
using System.Text;
using SmartFarm.Core.Contracts.Finance;

namespace SmartFarm.Web.Clients;

public static class FinanceInsightsCsv
{
    private static string Money(decimal amount) => amount.ToString("0.00", CultureInfo.InvariantCulture);
    private static string Cell(string? value) => HarvestCsv.Cell(value);

    public static string Build(FarmFinanceInsights insight)
    {
        var csv = new StringBuilder();
        csv.AppendLine("Section,Period or allocation,Type,Category,Entry count,Income PHP,Expenses PHP,Net PHP,Category amount PHP");
        csv.Append("Summary,")
            .Append(Cell((insight.FromDate?.ToString("yyyy-MM-dd") ?? "All dates") + " to " +
                (insight.ToDate?.ToString("yyyy-MM-dd") ?? "All dates")))
            .Append(",,,").Append(insight.EntryCount).Append(',')
            .Append(Money(insight.IncomePhp)).Append(',').Append(Money(insight.ExpensesPhp))
            .Append(',').AppendLine(Money(insight.NetPhp) + ",");
        foreach (var month in insight.Months.OrderBy(x => x.Month))
            csv.Append("Month,").Append(Cell(month.Month)).Append(",,,0,")
                .Append(Money(month.IncomePhp)).Append(',').Append(Money(month.ExpensesPhp))
                .Append(',').AppendLine(Money(month.NetPhp) + ",");
        foreach (var category in insight.Categories)
            csv.Append("Category,,").Append(Cell(category.Kind)).Append(',')
                .Append(Cell(category.Category)).Append(',').Append(category.EntryCount)
                .Append(",,,,").AppendLine(Money(category.AmountPhp));
        foreach (var crop in insight.CropTotals)
            csv.Append("Crop cycle,").Append(Cell(crop.Label)).Append(",,,")
                .Append(crop.EntryCount).Append(',').Append(Money(crop.IncomePhp))
                .Append(',').Append(Money(crop.ExpensesPhp)).Append(',')
                .AppendLine(Money(crop.NetPhp) + ",");
        return csv.ToString();
    }
}
