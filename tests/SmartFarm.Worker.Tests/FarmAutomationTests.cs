using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Services.Tasks;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Services;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class FarmAutomationTests
{
    [Theory]
    [InlineData(0)]
    [InlineData(7)]
    public void TaskTypeOutsideDatabaseRangeIsRejected(byte taskType)
    {
        var input = new FarmTaskInput(taskType, new DateOnly(2026, 10, 20), null, null, null);
        Assert.NotNull(TaskRules.Validate(input));
    }

    [Fact]
    public void TasksRequireValidDatesAndLimitDescriptions()
    {
        Assert.NotNull(TaskRules.Validate(new FarmTaskInput(1, default, null, null, null)));
        Assert.NotNull(TaskRules.Validate(new FarmTaskInput(2,
            new DateOnly(2026, 10, 20), new string('x', 501), null, null)));
        Assert.Null(TaskRules.Validate(new FarmTaskInput(2,
            new DateOnly(2026, 10, 20), "Water crops", null, null)));
    }

    [Fact]
    public void ScheduledTaskKeyIsStableAndSpecificToDate()
    {
        var cropId = Guid.NewGuid();
        var date = new DateOnly(2026, 10, 20);
        Assert.Equal(TaskRules.ScheduleKey(cropId, 2, date),
            TaskRules.ScheduleKey(cropId, 2, date));
        Assert.NotEqual(TaskRules.ScheduleKey(cropId, 2, date),
            TaskRules.ScheduleKey(cropId, 2, date.AddDays(1)));
        Assert.NotEqual(TaskRules.ScheduleKey(cropId, 2, date),
            TaskRules.ScheduleKey(cropId, 5, date));
    }

    [Theory]
    [InlineData(0, 34, 0)]
    [InlineData(20, 30, 1)]
    [InlineData(50, 40, 2)]
    public void WeatherWarningsUseConfiguredThresholds(int rainfall, int maximum, int count)
    {
        var date = new DateOnly(2026, 10, 20);
        var forecast = new DailyWeatherForecast(date, 20, maximum, rainfall);
        Assert.Equal(count, WeatherRules.Evaluate(forecast).Count);
    }

    [Fact]
    public async Task JobsCallTheAutomationService()
    {
        var service = new FakeAutomationService();
        await new TaskReminderJob(service).ExecuteAsync();
        await new WeatherCheckJob(service).ExecuteAsync();

        Assert.Equal(1, service.TasksCalled);
        Assert.Equal(1, service.WeatherCalled);
    }

    private sealed class FakeAutomationService : IFarmAutomationService
    {
        public int TasksCalled { get; private set; }
        public int WeatherCalled { get; private set; }

        public Task GenerateTasksAndRemindersAsync(CancellationToken ct = default)
        {
            TasksCalled++;
            return Task.CompletedTask;
        }

        public Task CheckWeatherAsync(CancellationToken ct = default)
        {
            WeatherCalled++;
            return Task.CompletedTask;
        }
    }
}
