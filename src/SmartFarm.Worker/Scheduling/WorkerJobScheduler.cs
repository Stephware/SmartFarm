using Hangfire;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using SmartFarm.Worker.Jobs;

namespace SmartFarm.Worker.Scheduling;

public sealed class WorkerJobScheduler : IHostedService
{
    public const string HeartbeatJobId = "smartfarm-worker-check";

    private readonly IRecurringJobManager _recurringJobs;
    private readonly ILogger<WorkerJobScheduler> _logger;

    public WorkerJobScheduler(
        IRecurringJobManager recurringJobs,
        ILogger<WorkerJobScheduler> logger)
    {
        _recurringJobs = recurringJobs;
        _logger = logger;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();

        _recurringJobs.AddOrUpdate<WorkerHeartbeatJob>(
            HeartbeatJobId,
            job => job.ExecuteAsync(),
            Cron.Hourly(),
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _logger.LogInformation("Scheduled the hourly SmartFarm worker check.");
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}
