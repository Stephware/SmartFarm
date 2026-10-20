using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Core.Services.Reports;

public static class HarvestReportRules
{
    public static HarvestReport Build(Guid farmId, string farmName, string timeZoneId,
        DateOnly? from, DateOnly? to, Guid? plotId, Guid? cropTypeId,
        IEnumerable<HarvestReportEntry> harvests)
    {
        var entries = harvests.OrderByDescending(h => h.HarvestedAtUtc)
            .ThenByDescending(h => h.Id).ToList();
        var valid = entries.Where(h => !h.IsVoided).ToList();
        var units = valid.GroupBy(h => h.Unit)
            .Select(g => new HarvestUnitTotal(g.Key, g.Sum(h => h.Quantity), g.Count()))
            .OrderBy(g => g.Unit).ToList();
        var crops = valid.GroupBy(h => new
            { h.CropTypeId, h.CropTypeName, h.PlotId, h.PlotName, h.Unit })
            .Select(g => new HarvestCropPlotTotal(g.Key.CropTypeId, g.Key.CropTypeName,
                g.Key.PlotId, g.Key.PlotName, g.Key.Unit,
                g.Sum(h => h.Quantity), g.Count()))
            .OrderBy(g => g.CropTypeName).ThenBy(g => g.PlotName)
            .ThenBy(g => g.Unit).ToList();
        var monthly = valid.GroupBy(h => new
            { Month = h.FarmDate.ToString("yyyy-MM"), h.Unit })
            .Select(g => new HarvestMonthlyTotal(g.Key.Month, g.Key.Unit,
                g.Sum(h => h.Quantity), g.Count()))
            .OrderByDescending(g => g.Month).ThenBy(g => g.Unit).ToList();
        return new HarvestReport(farmId, farmName, timeZoneId,
            from, to, plotId, cropTypeId, valid.Count, entries.Count - valid.Count,
            units, crops, monthly, entries);
    }
}
