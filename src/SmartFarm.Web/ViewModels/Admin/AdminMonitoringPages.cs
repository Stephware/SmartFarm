namespace SmartFarm.Web.ViewModels.Admin;

// These are read-only summaries. They never contain job arguments, farm payloads,
// device identifiers, request hashes, auth tokens or connection strings.
public sealed record AdminServiceStatus(string State, string Detail);

public sealed record AdminJobFailure(string JobType, DateTime CreatedAtUtc);

public sealed record AdminWorkerStatus(
    string State, string Detail, DateTime? LastHeartbeatUtc,
    int FailedJobsLast7Days, int WaitingJobs,
    IReadOnlyList<AdminJobFailure> RecentFailures);

public sealed record AdminSyncSummary(
    bool Available, int CompletedLast7Days, int RecordedFailuresLast7Days,
    DateTime? LastReceiptUtc, string Message);

public sealed record AdminMonitoringPage(
    DateTime CheckedAtUtc, AdminServiceStatus Database, AdminServiceStatus Api,
    AdminWorkerStatus Worker, AdminSyncSummary Sync);

public sealed record AdminSyncRow(
    Guid OperationId, string FarmName, string OperationType,
    string Outcome, string? ErrorCode, DateTime ReceivedAtUtc);

public sealed record AdminSyncPage(
    string Search, string Status, string Type, int Page, int Total,
    bool Available, IReadOnlyList<AdminSyncRow> Operations)
{
    public const int PageSize = 25;
    public bool HasPrevious => Page > 1;
    public bool HasNext => Page * PageSize < Total;
}

public static class AdminMonitoringRules
{
    public static readonly string[] Types =
        ["activity", "activity_stock", "observation", "inventory", "harvest"];

    public static string WorkerState(DateTime? heartbeatUtc, DateTime nowUtc) =>
        heartbeatUtc is null ? "No workers" :
        heartbeatUtc > nowUtc.AddMinutes(-5) && heartbeatUtc <= nowUtc.AddMinutes(1)
            ? "Active" : "Stale";

    public static string SyncOutcome(byte status) => status switch
    {
        1 => "Succeeded",
        2 => "Recorded failure",
        _ => "Unknown"
    };

    public static string FilterStatus(string? status) => status switch
    {
        "succeeded" or "failed" => status,
        _ => "all"
    };
}
