using System.Globalization;
using System.Text;
using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Web.Clients;

// Export only data visible to the signed-in farm member.
public static class HarvestCsv
{
    // Escape quotes, separators and spreadsheet formulas in untrusted names/notes.
    public static string Cell(string? value)
    {
        var text = value ?? "";
        if (text.Length > 0 && (text[0] is '=' or '+' or '-' or '@' or '\t' or '\r' or '\n'))
            text = "'" + text;
        return "\"" + text.Replace("\"", "\"\"") + "\"";
    }

    public static string Build(HarvestReport report)
    {
        var csv = new StringBuilder();
        csv.AppendLine("Farm date,Crop,Plot,Quantity,Unit,Status,Recorded UTC,Notes");
        foreach (var entry in report.Entries)
        {
            csv.Append(Cell(entry.FarmDate.ToString("yyyy-MM-dd"))).Append(',')
                .Append(Cell(entry.CropTypeName)).Append(',')
                .Append(Cell(entry.PlotName)).Append(',')
                .Append(entry.Quantity.ToString("0.000", CultureInfo.InvariantCulture)).Append(',')
                .Append(Cell(entry.Unit)).Append(',')
                .Append(Cell(entry.IsVoided ? "Voided" : "Valid")).Append(',')
                .Append(Cell(entry.HarvestedAtUtc.ToString("yyyy-MM-dd HH:mm:ss",
                    CultureInfo.InvariantCulture))).Append(',')
                .AppendLine(Cell(entry.Notes));
        }
        return csv.ToString();
    }
}
