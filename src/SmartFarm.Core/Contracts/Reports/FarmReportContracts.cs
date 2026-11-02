namespace SmartFarm.Core.Contracts.Reports;

// Quantities are always grouped by harvest unit; kg, pieces and boxes are not interchangeable.
public sealed record HarvestReportEntry(
    Guid Id, Guid CycleId, Guid PlotId, string PlotName,
    Guid CropTypeId, string CropTypeName, decimal Quantity, string Unit,
    DateTime HarvestedAtUtc, DateOnly FarmDate, bool IsVoided, string? Notes);

public sealed record HarvestUnitTotal(string Unit, decimal Quantity, int HarvestCount);

public sealed record HarvestCropPlotTotal(
    Guid CropTypeId, string CropTypeName, Guid PlotId, string PlotName,
    string Unit, decimal Quantity, int HarvestCount);

public sealed record HarvestMonthlyTotal(string Month, string Unit, decimal Quantity, int HarvestCount);

public sealed record HarvestReport(
    Guid FarmId, string FarmName, string TimeZoneId,
    DateOnly? FromDate, DateOnly? ToDate, Guid? PlotId, Guid? CropTypeId,
    int ValidHarvestCount, int VoidedHarvestCount,
    IReadOnlyList<HarvestUnitTotal> UnitTotals,
    IReadOnlyList<HarvestCropPlotTotal> CropPlotTotals,
    IReadOnlyList<HarvestMonthlyTotal> MonthlyTotals,
    IReadOnlyList<HarvestReportEntry> Entries);

public sealed record DashboardAttentionItem(
    string Kind, Guid TargetId, string Title, string Detail, DateOnly? DueDate = null);

public sealed record FarmDashboard(
    Guid FarmId, string FarmName, DateOnly FarmToday,
    int ActivePlots, int PlannedCrops, int ActiveCrops, int CompletedCrops,
    int OpenTasks, int OverdueTasks, int LowStockItems, int ConcerningObservations,
    IReadOnlyList<HarvestUnitTotal> HarvestLast30Days,
    IReadOnlyList<HarvestReportEntry> RecentHarvests)
{
    // Read-only drill-downs. Existing response fields remain unchanged for Android.
    public IReadOnlyList<DashboardAttentionItem> AttentionItems { get; init; } = [];
}
