using Hangfire;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using SmartFarm.Worker.Jobs;

namespace SmartFarm.Worker.Scheduling;

public sealed class WorkerJobScheduler : IHostedService
{
    public const string HeartbeatJobId = "smartfarm-worker-check";
    public const string TaskJobId = "smartfarm-tasks-and-reminders";
    public const string WeatherJobId = "smartfarm-weather-check";

    private readonly IRecurringJobManager _recurringJobs;
    private readonly IBackgroundJobClient _jobs;
    private readonly bool _runJobsOnStartup;
    private readonly ILogger<WorkerJobScheduler> _logger;

    public WorkerJobScheduler(
        IRecurringJobManager recurringJobs,
        IBackgroundJobClient jobs,
        IConfiguration configuration,
        ILogger<WorkerJobScheduler> logger)
    {
        _recurringJobs = recurringJobs;
        _jobs = jobs;
        _runJobsOnStartup = configuration.GetValue<bool>("Hangfire:RunJobsOnStartup");
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

        // Enqueue a lightweight check immediately so the server can prove it processes jobs,
        // without waiting for the first hourly recurring run.
        _jobs.Enqueue<WorkerHeartbeatJob>(job => job.ExecuteAsync());

        // Use only for initial/local verification: real farm jobs are normally cron-driven.
        if (_runJobsOnStartup)
        {
            _jobs.Enqueue<TaskReminderJob>(job => job.ExecuteAsync());
            _jobs.Enqueue<WeatherCheckJob>(job => job.ExecuteAsync());
        }

        _logger.LogInformation(
            "Scheduled heartbeat (hourly), tasks/reminders (hourly) and weather (every 6 hours). " +
            "Startup heartbeat queued. Immediate farm-job checks: {RunJobsOnStartup}.",
            _runJobsOnStartup);
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}
