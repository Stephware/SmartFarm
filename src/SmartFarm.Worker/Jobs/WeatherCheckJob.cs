using Hangfire;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

public sealed class WeatherCheckJob(IFarmAutomationService service)
{
    [AutomaticRetry(Attempts = 3)]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public Task ExecuteAsync() => service.CheckWeatherAsync();
}
