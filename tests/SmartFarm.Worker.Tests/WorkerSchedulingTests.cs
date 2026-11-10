using System.Reflection;
using Hangfire;
using Hangfire.Common;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging.Abstractions;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Scheduling;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class WorkerSchedulingTests
{
    [Theory]
    [InlineData(false, 1)]
    [InlineData(true, 3)]
    public async Task WorkerSchedulesAllRecurringJobsAndQueuesStartupChecks(
        bool runFarmJobsOnStartup, int expectedStartupJobs)
    {
        var recurring = DispatchProxy.Create<IRecurringJobManager, RecordingProxy>();
        var queued = DispatchProxy.Create<IBackgroundJobClient, RecordingProxy>();
        var config = new ConfigurationBuilder().AddInMemoryCollection(
            new Dictionary<string, string?>
            {
                ["Hangfire:RunJobsOnStartup"] = runFarmJobsOnStartup.ToString()
            }).Build();
        var scheduler = new WorkerJobScheduler(recurring, queued, config,
            NullLogger<WorkerJobScheduler>.Instance);

        await scheduler.StartAsync(CancellationToken.None);

        var scheduleCalls = ((RecordingProxy)recurring).Calls
            .Where(x => x.Method == "AddOrUpdate").ToList();
        Assert.Equal(new[]
        {
            WorkerJobScheduler.HeartbeatJobId,
            WorkerJobScheduler.TaskJobId,
            WorkerJobScheduler.WeatherJobId
        }, scheduleCalls.Select(x => (string)x.Args[0]!).ToArray());
        Assert.Equal(3, scheduleCalls.Count);
        // These cadences must match the app's reminder and forecast freshness.
        Assert.Equal("0 * * * *", Assert.IsType<string>(scheduleCalls[0].Args[2]));
        Assert.Equal("0 * * * *", Assert.IsType<string>(scheduleCalls[1].Args[2]));
        Assert.Equal("0 */6 * * *", Assert.IsType<string>(scheduleCalls[2].Args[2]));
        Assert.All(scheduleCalls, call =>
            Assert.Equal(TimeZoneInfo.Utc.Id,
                Assert.IsType<RecurringJobOptions>(call.Args[3]).TimeZone.Id));

        var startupJobs = ((RecordingProxy)queued).Calls
            .Where(x => x.Method == "Create")
            .Select(x => Assert.IsType<Job>(x.Args[0])).ToList();
        Assert.Equal(expectedStartupJobs, startupJobs.Count);
        Assert.Equal(typeof(WorkerHeartbeatJob), startupJobs[0].Type);
        if (runFarmJobsOnStartup)
        {
            Assert.Equal(typeof(TaskReminderJob), startupJobs[1].Type);
            Assert.Equal(typeof(WeatherCheckJob), startupJobs[2].Type);
        }
    }

    public class RecordingProxy : DispatchProxy
    {
        public List<(string Method, object?[] Args)> Calls { get; } = [];

        protected override object? Invoke(MethodInfo? targetMethod, object?[]? args)
        {
            Calls.Add((targetMethod?.Name ?? "", args ?? []));
            return targetMethod?.ReturnType == typeof(string) ? "test-job-id" : null;
        }
    }
}
