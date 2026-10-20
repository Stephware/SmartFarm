using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using SmartFarm.Worker;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Scheduling;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class WorkerJobTests
{
    [Fact]
    public async Task WorkerCheckWritesToTheLog()
    {
        var logger = new RecordingLogger<WorkerHeartbeatJob>();
        var job = new WorkerHeartbeatJob(logger);

        await job.ExecuteAsync();

        Assert.Contains(logger.Messages, message =>
            message.Contains("SmartFarm worker check completed"));
    }

    [Fact]
    public void EnabledWorkerRegistersTheJobAndScheduler()
    {
        var services = new ServiceCollection();
        services.AddSmartFarmBackgroundProcessing(Configuration(enabled: true));

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

    private sealed class RecordingLogger<T> : ILogger<T>
    {
        public List<string> Messages { get; } = [];

        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;

        public bool IsEnabled(LogLevel logLevel) => true;

        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state,
            Exception? exception, Func<TState, Exception?, string> formatter)
        {
            Messages.Add(formatter(state, exception));
        }
    }
}
