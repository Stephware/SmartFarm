namespace SmartFarm.Web.ViewModels.Admin;

// Only aggregate metrics: no individual farmer, location, stock, or crop financial records.
public sealed record AdminReportDay(DateOnly Date, int Farms,
    int HarvestEvents, int SupportRequests, int SuccessfulSyncReceipts);

public sealed record AdminPlatformReportPage(
    int Days, DateOnly StartDate, DateOnly EndDate, DateTime GeneratedAtUtc,
    int RegisteredUsers, int DisabledUsers,
    int TotalFarms, int ActiveFarms, int ActivePlots, int ActiveCropCycles,
    int OpenSupportCases, int NewFarms, int HarvestEvents, int NewSupportCases,
    int ResolvedSupportCases, int SuccessfulSyncReceipts,
    IReadOnlyList<AdminReportDay> Daily)
{
    public int MaxDaily => Math.Max(1, Daily.Select(d => Math.Max(
        Math.Max(d.Farms, d.HarvestEvents),
        Math.Max(d.SupportRequests, d.SuccessfulSyncReceipts)))
        .DefaultIfEmpty(0).Max());
}

public static class AdminReportRules
{
    public static int NormalizeDays(int? days) => days is 7 or 30 or 90 ? days.Value : 30;
}
