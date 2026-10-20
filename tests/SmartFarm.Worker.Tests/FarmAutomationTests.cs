using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Entities;
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

    [Fact]
    public void ActiveCropsGenerateRegularWateringAndHarvestDates()
    {
        var start = new DateOnly(2026, 10, 20);
        var crop = new CropCycle
        {
            Status = 1,
            PlantedDate = start,
            WateringIntervalDays = 2,
            ExpectedHarvestDate = start.AddDays(5)
        };

        var tasks = TaskRules.ScheduledTasks(crop, start, start.AddDays(6)).ToList();

        Assert.Contains(tasks, t => t.Type == 2 && t.Due == start.AddDays(2));
        Assert.Contains(tasks, t => t.Type == 2 && t.Due == start.AddDays(4));
        Assert.Contains(tasks, t => t.Type == 2 && t.Due == start.AddDays(6));
        Assert.Contains(tasks, t => t.Type == 5 && t.Due == start.AddDays(5));
        Assert.DoesNotContain(tasks, t => t.Type == 1);
    }

    [Fact]
    public void PlannedCropsOnlyGeneratePlantingTasks()
    {
        var day = new DateOnly(2026, 10, 20);
        var crop = new CropCycle
        {
            Status = 0,
            PlannedPlantingDate = day.AddDays(3),
            WateringIntervalDays = 2,
            ExpectedHarvestDate = day.AddDays(5)
        };

        var tasks = TaskRules.ScheduledTasks(crop, day, day.AddDays(7)).ToList();

        var planting = Assert.Single(tasks);
        Assert.Equal((byte)1, planting.Type);
        Assert.Equal(day.AddDays(3), planting.Due);
    }

    [Fact]
    public void OverdueCropDeadlinesAreStillGenerated()
    {
        var today = new DateOnly(2026, 10, 20);
        var planned = new CropCycle
        {
            Status = 0,
            PlannedPlantingDate = today.AddDays(-3)
        };
        var active = new CropCycle
        {
            Status = 1,
            PlantedDate = today.AddDays(-10),
            ExpectedHarvestDate = today.AddDays(-1)
        };

        Assert.Contains(TaskRules.ScheduledTasks(planned, today, today.AddDays(7)),
            task => task.Type == 1 && task.Due == today.AddDays(-3));
        Assert.Contains(TaskRules.ScheduledTasks(active, today, today.AddDays(7)),
            task => task.Type == 5 && task.Due == today.AddDays(-1));
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
    public void BadWeatherReadingsAreRejectedBeforeDatabaseWrites()
    {
        var date = new DateOnly(2026, 10, 20);
        Assert.True(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 18, 32, 4)));
        Assert.True(WeatherRules.IsPlausible(new DailyWeatherForecast(date, null, null, null)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 35, 20, 3)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 20, 28, -1)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, -120, 28, 1)));
        Assert.False(WeatherRules.IsPlausible(new DailyWeatherForecast(date, 20, 120, 1)));
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
