using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

// Keep jobs small: the service contains the actual operation.
public sealed class WorkerHeartbeatJob
{
    private readonly IWorkerHealthService _service;

    public WorkerHeartbeatJob(IWorkerHealthService service)
    {
        _service = service;
    }

    public Task ExecuteAsync() => _service.CheckAsync();
}
