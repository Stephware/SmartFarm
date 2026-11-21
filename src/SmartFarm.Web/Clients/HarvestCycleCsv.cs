using System.Globalization;
using System.Text;
using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Web.Clients;

// Export the exact filtered planting-cycle totals shown on the report page.
// Keep dates and units explicit: filtered partial totals are not lifetime yields.
public static class HarvestCycleCsv
{
    public static string Build(HarvestReport report)
    {
        var csv = new StringBuilder();
        csv.AppendLine("Crop cycle ID,Crop,Plot,Unit,First harvest date,Last harvest date,Harvest entries,Recorded quantity,Filter from,Filter to");
        foreach (var cycle in report.CycleTotals)
        {
            csv.Append(HarvestCsv.Cell(cycle.CycleId.ToString("D"))).Append(',')
                .Append(HarvestCsv.Cell(cycle.CropTypeName)).Append(',')
                .Append(HarvestCsv.Cell(cycle.PlotName)).Append(',')
                .Append(HarvestCsv.Cell(cycle.Unit)).Append(',')
                .Append(HarvestCsv.Cell(cycle.FirstHarvestDate.ToString("yyyy-MM-dd"))).Append(',')
                .Append(HarvestCsv.Cell(cycle.LastHarvestDate.ToString("yyyy-MM-dd"))).Append(',')
                .Append(cycle.HarvestCount.ToString(CultureInfo.InvariantCulture)).Append(',')
                .Append(cycle.Quantity.ToString("0.000", CultureInfo.InvariantCulture)).Append(',')
                .Append(HarvestCsv.Cell(report.FromDate?.ToString("yyyy-MM-dd") ?? "All dates")).Append(',')
                .AppendLine(HarvestCsv.Cell(report.ToDate?.ToString("yyyy-MM-dd") ?? "All dates"));
        }
        return csv.ToString();
    }
}
