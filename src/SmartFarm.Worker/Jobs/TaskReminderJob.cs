using Hangfire;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

public sealed class TaskReminderJob(IFarmAutomationService service)
{
    [AutomaticRetry(Attempts = 3)]
    public Task ExecuteAsync() => service.GenerateTasksAndRemindersAsync();
}
