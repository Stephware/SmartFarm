using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Web.ViewModels.Reports;

public sealed record HarvestChartBar(string Label, decimal Quantity, decimal WidthPercent,
    int HarvestCount, decimal? YieldPerSquareMeter = null);
public sealed record HarvestChart(string Unit, IReadOnlyList<HarvestChartBar> Bars, int TotalRows);

public static class HarvestAnalyticsBuilder
{
    // Never compare unrelated units (kg, bundles, pieces, etc.) on one axis.
    private static IReadOnlyList<HarvestChart> MakeCharts(
        IEnumerable<(string Unit, string Label, decimal Quantity, int Count, decimal? Yield)> values,
        int limit)
    {
        return values.GroupBy(v => v.Unit, StringComparer.OrdinalIgnoreCase)
            .OrderBy(g => g.Key, StringComparer.OrdinalIgnoreCase)
            .Select(group =>
            {
                var rows = group.Take(limit).ToList();
                var max = rows.Count == 0 ? 0 : rows.Max(r => r.Quantity);
                return new HarvestChart(group.Key,
                    rows.Select(r => new HarvestChartBar(r.Label, r.Quantity,
                        max <= 0 ? 0 : Math.Clamp(r.Quantity * 100m / max, 0, 100),
                        r.Count, r.Yield)).ToList(), group.Count());
            }).ToList();
    }

    public static IReadOnlyList<HarvestChart> Monthly(HarvestReport report) =>
        MakeCharts(report.MonthlyTotals
            .OrderByDescending(m => m.Month)
            .Select(m => (m.Unit, m.Month, m.Quantity, m.HarvestCount, (decimal?)null)), 12);

    public static IReadOnlyList<HarvestChart> CropAndPlot(HarvestReport report,
        IReadOnlyList<PlotDetails> plots)
    {
        var area = plots.ToDictionary(p => p.Id, p => p.AreaSqMeters);
        return MakeCharts(report.CropPlotTotals
            .OrderByDescending(p => p.Quantity)
            .Select(p => (p.Unit, p.CropTypeName + " · " + p.PlotName, p.Quantity,
                p.HarvestCount, area.TryGetValue(p.PlotId, out var squareMeters) && squareMeters > 0
                    ? (decimal?)(p.Quantity / squareMeters) : null)), 10);
    }
}
