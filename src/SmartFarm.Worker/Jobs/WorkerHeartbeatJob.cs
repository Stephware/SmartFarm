using Microsoft.Extensions.Logging;

namespace SmartFarm.Worker.Jobs;

public sealed class WorkerHeartbeatJob
{
    private readonly ILogger<WorkerHeartbeatJob> _logger;

    public WorkerHeartbeatJob(ILogger<WorkerHeartbeatJob> logger)
    {
        _logger = logger;
    }

    public Task ExecuteAsync()
    {
        _logger.LogInformation("SmartFarm worker check completed at {UtcTime}.", DateTimeOffset.UtcNow);
        return Task.CompletedTask;
    }
}
