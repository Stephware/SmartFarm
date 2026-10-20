using Hangfire;
using Microsoft.Extensions.Logging;

namespace SmartFarm.Worker.Services;

// Check that Hangfire can read its storage and has a running server.
public sealed class WorkerHealthService : IWorkerHealthService
{
    private readonly JobStorage _storage;
    private readonly ILogger<WorkerHealthService> _logger;

    public WorkerHealthService(JobStorage storage, ILogger<WorkerHealthService> logger)
    {
        _storage = storage;
        _logger = logger;
    }

    public Task CheckAsync()
    {
        var servers = _storage.GetMonitoringApi().Servers();
        if (servers.Count == 0)
            throw new InvalidOperationException("Hangfire has no active worker servers.");

        _logger.LogInformation("SmartFarm worker check passed. Active servers: {ServerCount}.",
            servers.Count);

        return Task.CompletedTask;
    }
}
