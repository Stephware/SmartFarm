using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using SmartFarm.Worker;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Scheduling;
using SmartFarm.Worker.Services;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class WorkerJobTests
{
    [Fact]
    public async Task WorkerJobRunsItsService()
    {
        var service = new FakeWorkerHealthService();
        var job = new WorkerHeartbeatJob(service);

        await job.ExecuteAsync();

        Assert.Equal(1, service.CheckCount);
    }

    [Fact]
    public async Task WorkerJobLetsHangfireHandleServiceFailures()
    {
        var service = new FakeWorkerHealthService { ShouldFail = true };
        var job = new WorkerHeartbeatJob(service);

        await Assert.ThrowsAsync<InvalidOperationException>(() => job.ExecuteAsync());
        Assert.Equal(1, service.CheckCount);
    }

    [Fact]
    public void EnabledWorkerRegistersTheServiceJobAndScheduler()
    {
        var services = new ServiceCollection();
        services.AddSmartFarmBackgroundProcessing(Configuration(enabled: true));

        Assert.Contains(services, service =>
            service.ServiceType == typeof(IWorkerHealthService) &&
            service.ImplementationType == typeof(WorkerHealthService) &&
            service.Lifetime == ServiceLifetime.Scoped);
        Assert.Contains(services, service =>
            service.ServiceType == typeof(WorkerHeartbeatJob) &&
            service.Lifetime == ServiceLifetime.Scoped);
        Assert.Contains(services, service =>
            service.ServiceType == typeof(IHostedService) &&
            service.ImplementationType == typeof(WorkerJobScheduler));
        Assert.Equal("smartfarm-worker-check", WorkerJobScheduler.HeartbeatJobId);
    }

    [Fact]
    public void DisabledWorkerDoesNotRegisterJobs()
    {
        var services = new ServiceCollection();
        services.AddSmartFarmBackgroundProcessing(Configuration(enabled: false));

        Assert.DoesNotContain(services, service =>
            service.ServiceType == typeof(IWorkerHealthService));
        Assert.DoesNotContain(services, service =>
            service.ServiceType == typeof(WorkerHeartbeatJob));
        Assert.DoesNotContain(services, service =>
            service.ImplementationType == typeof(WorkerJobScheduler));
    }

    private static IConfiguration Configuration(bool enabled) =>
        new ConfigurationBuilder().AddInMemoryCollection(
            new Dictionary<string, string?>
            {
                ["Hangfire:Enabled"] = enabled.ToString(),
                ["ConnectionStrings:Hangfire"] =
                    "Server=127.0.0.1,1;Database=SmartFarmJobsDB;Integrated Security=true"
            }).Build();

    private sealed class FakeWorkerHealthService : IWorkerHealthService
    {
        public int CheckCount { get; private set; }
        public bool ShouldFail { get; set; }

        public Task CheckAsync()
        {
            CheckCount++;
            return ShouldFail
                ? Task.FromException(new InvalidOperationException("Health check failed."))
                : Task.CompletedTask;
        }
    }
}
