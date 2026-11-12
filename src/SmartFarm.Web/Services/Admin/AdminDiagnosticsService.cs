using System.Data;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.ViewModels.Admin;

namespace SmartFarm.Web.Services.Admin;

// This service only reads existing operational data; it never retries jobs,
// changes farm records, or exposes raw job/payload/error text.
public sealed class AdminDiagnosticsService(
    SmartFarmDbContext db, IConfiguration configuration, HttpClient client,
    ILogger<AdminDiagnosticsService> logger)
{
    public async Task<AdminMonitoringPage> OverviewAsync(CancellationToken ct)
    {
        var now = DateTime.UtcNow;
        var database = await DatabaseStatusAsync(ct);
        // Run optional network probes independently so a missing service does not
        // prevent the admin from seeing the others.
        var api = await ApiStatusAsync(ct);
        var worker = await WorkerStatusAsync(now, ct);
        var sync = database.State == "Available"
            ? await SyncSummaryAsync(now, ct)
            : new AdminSyncSummary(false, 0, 0, null,
                "Sync receipt history is unavailable while the application database cannot be reached.");
        return new AdminMonitoringPage(now, database, api, worker, sync);
    }

    public async Task<AdminSyncPage> SyncHistoryAsync(
        string? search, string? status, string? type, int page, CancellationToken ct)
    {
        var term = (search ?? "").Trim();
        if (term.Length > 100) term = term[..100];
        var state = AdminMonitoringRules.FilterStatus(status);
        var operationType = AdminMonitoringRules.Types.Contains(type) ? type! : "all";
        page = Math.Clamp(page, 1, 100000);
        try
        {
            // Do not read ResultJson or RequestHash: they can contain field data.
            var query = db.SyncOperations.AsNoTracking().AsQueryable();
            if (term.Length > 0)
                query = query.Where(o => db.Farms.Any(f =>
                    f.Id == o.FarmId && f.Name.Contains(term)));
            if (state == "failed") query = query.Where(o => o.Status == 2);
            if (state == "succeeded") query = query.Where(o => o.Status == 1);
            if (operationType != "all") query = query.Where(o => o.OperationType == operationType);

            var total = await query.CountAsync(ct);
            var operations = await query.OrderByDescending(o => o.ReceivedAtUtc)
                .ThenByDescending(o => o.OperationId)
                .Skip((page - 1) * AdminSyncPage.PageSize)
                .Take(AdminSyncPage.PageSize)
                .Select(o => new
                {
                    o.OperationId, o.FarmId, o.OperationType, o.Status,
                    o.ErrorCode, o.ReceivedAtUtc
                }).ToListAsync(ct);

            var farmIds = operations.Select(o => o.FarmId).Distinct().ToArray();
            var farms = await db.Farms.AsNoTracking()
                .Where(f => farmIds.Contains(f.Id))
                .Select(f => new { f.Id, f.Name })
                .ToDictionaryAsync(f => f.Id, f => f.Name, ct);

            return new AdminSyncPage(term, state, operationType, page, total, true,
                operations.Select(o => new AdminSyncRow(o.OperationId,
                    farms.GetValueOrDefault(o.FarmId, "Unknown farm"), o.OperationType,
                    AdminMonitoringRules.SyncOutcome(o.Status),
                    // Only a short server error code, never raw messages or payloads.
                    o.Status == 2 ? o.ErrorCode : null,
                    o.ReceivedAtUtc)).ToArray());
        }
        catch (Exception error) when (!ct.IsCancellationRequested &&
            error is not OutOfMemoryException)
        {
            logger.LogWarning("Admin sync history query unavailable.");
            return new AdminSyncPage(term, state, operationType, page, 0, false, []);
        }
    }

    private async Task<AdminServiceStatus> DatabaseStatusAsync(CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(db.Database.GetConnectionString()))
            return new("Not configured", "Set ConnectionStrings:SmartFarm on the Web host.");

        try
        {
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
            timeout.CancelAfter(TimeSpan.FromSeconds(4));
            if (await db.Database.CanConnectAsync(timeout.Token))
                return new("Available", "Application database accepts connections.");
        }
        catch (Exception error) when (!ct.IsCancellationRequested &&
            error is not OutOfMemoryException)
        {
            logger.LogWarning("Admin application database probe unavailable.");
        }
        return new("Unavailable", "Cannot reach the application database. Check database service and connection settings.");
    }

    private async Task<AdminServiceStatus> ApiStatusAsync(CancellationToken ct)
    {
        var configured = configuration["Monitoring:ApiStatusUrl"];
        if (string.IsNullOrWhiteSpace(configured))
            return new("Not configured",
                "Optional API process probe not configured. Set Monitoring:ApiStatusUrl to the trusted /api/status address.");

        if (!Uri.TryCreate(configured, UriKind.Absolute, out var url) ||
            url.Scheme is not ("http" or "https") || url.UserInfo.Length > 0 ||
            url.AbsolutePath != "/api/status")
            return new("Not configured", "The configured API status address must end in /api/status.");

        try
        {
            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
            timeout.CancelAfter(TimeSpan.FromSeconds(4));
            using var response = await client.GetAsync(url, HttpCompletionOption.ResponseHeadersRead, timeout.Token);
            if (response.IsSuccessStatusCode)
            {
                // The endpoint is an anonymous process smoke test, NOT database readiness.
                using var document = await System.Text.Json.JsonDocument.ParseAsync(
                    await response.Content.ReadAsStreamAsync(timeout.Token), cancellationToken: timeout.Token);
                var root = document.RootElement;
                if (root.TryGetProperty("app", out var app) && app.GetString() == "SmartFarm" &&
                    root.TryGetProperty("status", out var state) && state.GetString() == "ok")
                    return new("Available", "API process responded. This does not verify the API database.");
            }
        }
        catch (Exception error) when (!ct.IsCancellationRequested &&
            error is not OutOfMemoryException)
        {
            logger.LogWarning("Admin API status probe unavailable.");
        }
        return new("Unavailable", "API process did not return its expected status response.");
    }

    private async Task<AdminSyncSummary> SyncSummaryAsync(DateTime now, CancellationToken ct)
    {
        try
        {
            var since = now.AddDays(-7);
            var completed = await db.SyncOperations.AsNoTracking()
                .CountAsync(o => o.ReceivedAtUtc >= since && o.Status == 1, ct);
            var failures = await db.SyncOperations.AsNoTracking()
                .CountAsync(o => o.ReceivedAtUtc >= since && o.Status == 2, ct);
            var last = await db.SyncOperations.AsNoTracking()
                .OrderByDescending(o => o.ReceivedAtUtc)
                .Select(o => (DateTime?)o.ReceivedAtUtc).FirstOrDefaultAsync(ct);
            return new(true, completed, failures, last,
                "Only server-recorded sync receipts are visible. Pending, offline, and review-needed Android Room records cannot be observed centrally.");
        }
        catch (Exception error) when (!ct.IsCancellationRequested &&
            error is not OutOfMemoryException)
        {
            logger.LogWarning("Admin sync summary unavailable.");
            return new(false, 0, 0, null, "Sync receipt history could not be loaded.");
        }
    }

    private async Task<AdminWorkerStatus> WorkerStatusAsync(DateTime now, CancellationToken ct)
    {
        var configured = configuration.GetConnectionString("Hangfire");
        if (string.IsNullOrWhiteSpace(configured))
            return new("Not configured",
                "Set ConnectionStrings:Hangfire on the Web host to view the separate jobs database.",
                null, 0, 0, []);
        try
        {
            var jobs = new SqlConnectionStringBuilder(configured)
            {
                ConnectTimeout = 4,
                ApplicationName = "SmartFarm.Admin.Diagnostics"
            };
            var appString = configuration.GetConnectionString("SmartFarm");
            var appCatalog = string.IsNullOrWhiteSpace(appString)
                ? null : new SqlConnectionStringBuilder(appString).InitialCatalog;
            if (string.IsNullOrWhiteSpace(jobs.DataSource) ||
                string.IsNullOrWhiteSpace(jobs.InitialCatalog) ||
                jobs.InitialCatalog.Equals(appCatalog, StringComparison.OrdinalIgnoreCase) ||
                jobs.InitialCatalog.Equals("SmartFarmDB", StringComparison.OrdinalIgnoreCase))
                return new("Not configured",
                    "Hangfire diagnostics require a separate jobs database (not SmartFarmDB).",
                    null, 0, 0, []);

            using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
            timeout.CancelAfter(TimeSpan.FromSeconds(6));
            await using var connection = new SqlConnection(jobs.ConnectionString);
            await connection.OpenAsync(timeout.Token);

            // Fixed read-only queries; never show Hangfire InvocationData, arguments,
            // exception details, SQL messages, or worker machine names.
            await using var heartbeatCommand = new SqlCommand(
                "SELECT MAX([LastHeartbeat]) FROM [HangFire].[Server]", connection)
                { CommandTimeout = 4 };
            var value = await heartbeatCommand.ExecuteScalarAsync(timeout.Token);
            DateTime? last = value is DateTime heartbeat
                ? DateTime.SpecifyKind(heartbeat, DateTimeKind.Utc) : null;

            await using var countsCommand = new SqlCommand("""
                SELECT
                    SUM(CASE WHEN [StateName] = N'Failed'
                        AND [CreatedAt] >= DATEADD(day, -7, SYSUTCDATETIME()) THEN 1 ELSE 0 END),
                    SUM(CASE WHEN [StateName] IN (N'Enqueued', N'Processing', N'Scheduled') THEN 1 ELSE 0 END)
                FROM [HangFire].[Job]
                WHERE [InvocationData] LIKE N'%SmartFarm.Worker.Jobs.%'
                """, connection) { CommandTimeout = 4 };
            int failed = 0, waiting = 0;
            await using (var counts = await countsCommand.ExecuteReaderAsync(timeout.Token))
            {
                if (await counts.ReadAsync(timeout.Token))
                {
                    failed = counts.IsDBNull(0) ? 0 : Convert.ToInt32(counts.GetValue(0));
                    waiting = counts.IsDBNull(1) ? 0 : Convert.ToInt32(counts.GetValue(1));
                }
            }

            await using var failuresCommand = new SqlCommand("""
                SELECT TOP (12)
                    CASE
                        WHEN [InvocationData] LIKE N'%WorkerHeartbeatJob%' THEN N'Worker heartbeat'
                        WHEN [InvocationData] LIKE N'%TaskReminderJob%' THEN N'Tasks and reminders'
                        WHEN [InvocationData] LIKE N'%WeatherCheckJob%' THEN N'Weather check'
                        ELSE N'Other SmartFarm job'
                    END,
                    [CreatedAt]
                FROM [HangFire].[Job]
                WHERE [StateName] = N'Failed'
                  AND [InvocationData] LIKE N'%SmartFarm.Worker.Jobs.%'
                ORDER BY [Id] DESC
                """, connection) { CommandTimeout = 4 };
            var recent = new List<AdminJobFailure>();
            await using (var reader = await failuresCommand.ExecuteReaderAsync(timeout.Token))
            {
                while (await reader.ReadAsync(timeout.Token))
                    recent.Add(new(reader.GetString(0),
                        DateTime.SpecifyKind(reader.GetDateTime(1), DateTimeKind.Utc)));
            }
            var state = AdminMonitoringRules.WorkerState(last, now);
            var detail = state switch
            {
                "Active" => "At least one worker has a recent Hangfire heartbeat.",
                "Stale" => "No fresh heartbeat detected in the past five minutes.",
                _ => "No Hangfire worker heartbeat is recorded."
            };
            return new(state, detail, last, failed, waiting, recent);
        }
        catch (Exception error) when (!ct.IsCancellationRequested &&
            error is not OutOfMemoryException)
        {
            logger.LogWarning("Admin Hangfire diagnostics unavailable.");
            return new("Unavailable",
                "Could not read the separate jobs database or its HangFire schema.",
                null, 0, 0, []);
        }
    }
}
