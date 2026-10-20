namespace SmartFarm.Worker.Services;

public interface IWorkerHealthService
{
    Task CheckAsync();
}
