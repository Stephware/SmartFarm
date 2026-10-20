using Hangfire;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

public sealed class WeatherCheckJob(IFarmAutomationService service)
{
    [AutomaticRetry(Attempts = 3)]
    public Task ExecuteAsync() => service.CheckWeatherAsync();
}
