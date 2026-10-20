using Hangfire;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using SmartFarm.Worker.Jobs;

namespace SmartFarm.Worker.Scheduling;

public sealed class WorkerJobScheduler : IHostedService
{
    public const string HeartbeatJobId = "smartfarm-worker-check";
    public const string TaskJobId = "smartfarm-tasks-and-reminders";
    public const string WeatherJobId = "smartfarm-weather-check";

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

        _recurringJobs.AddOrUpdate<TaskReminderJob>(
            TaskJobId,
            job => job.ExecuteAsync(),
            Cron.Hourly(),
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _recurringJobs.AddOrUpdate<WeatherCheckJob>(
            WeatherJobId,
            job => job.ExecuteAsync(),
            "0 */6 * * *",
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _logger.LogInformation("Scheduled worker, task, and weather checks.");
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}
