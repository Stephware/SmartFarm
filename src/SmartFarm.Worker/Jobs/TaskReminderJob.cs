using Hangfire;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

public sealed class TaskReminderJob(IFarmAutomationService service)
{
    [AutomaticRetry(Attempts = 3)]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public Task ExecuteAsync() => service.GenerateTasksAndRemindersAsync();
}
