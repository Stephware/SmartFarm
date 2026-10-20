namespace SmartFarm.Worker.Services;

public interface IFarmAutomationService
{
    Task GenerateTasksAndRemindersAsync(CancellationToken ct = default);
    Task CheckWeatherAsync(CancellationToken ct = default);
}
