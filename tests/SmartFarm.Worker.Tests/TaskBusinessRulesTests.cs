using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Services.Tasks;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class TaskBusinessRulesTests
{
    [Theory]
    [InlineData(1, 2)]
    [InlineData(2, 3)]
    public void CropActivitiesMatchTheRightTaskType(byte activity, byte taskType) =>
        Assert.Equal((byte?)taskType, TaskRules.ForActivity(activity));

    [Theory]
    [InlineData(3)]
    [InlineData(4)]
    [InlineData(5)]
    public void UnrelatedCropActivitiesDoNotAutoCloseTasks(byte activity) =>
        Assert.Null(TaskRules.ForActivity(activity));

    [Fact]
    public void TaskCanOnlyBeClosedOnceAndPreservesActor()
    {
        var task = new FarmTask { Status = 0 };
        var actor = Guid.NewGuid();
        var now = new DateTime(2026, 10, 20, 10, 0, 0, DateTimeKind.Utc);
        Assert.True(TaskRules.Close(task, 1, actor, now));
        Assert.Equal((byte)1, task.Status);
        Assert.Equal(actor, task.ClosedByUserId);
        Assert.Equal(now, task.ClosedAtUtc);
        Assert.False(TaskRules.Close(task, 3, Guid.NewGuid(), now.AddMinutes(1)));
        Assert.Equal((byte)1, task.Status);
        Assert.Equal(actor, task.ClosedByUserId);
    }

    [Fact]
    public void PendingTaskRemindersExcludeEndedCropsAndStalePlanting()
    {
        var due = new DateOnly(2026, 10, 20);
        var task = new FarmTask { CropCycleId = Guid.NewGuid(), TaskType = 1, DueDate = due };
        Assert.True(TaskRules.IsReminderRelevant(task, 0));
        Assert.False(TaskRules.IsReminderRelevant(task, 1));
        Assert.False(TaskRules.IsReminderRelevant(task, 2));
        Assert.False(TaskRules.IsReminderRelevant(task, 3));
        Assert.False(TaskRules.IsReminderRelevant(task, 4));
        Assert.False(TaskRules.IsReminderRelevant(task, null));

        task.TaskType = 2;
        Assert.True(TaskRules.IsReminderRelevant(task, 1));
        task.Status = 1;
        Assert.False(TaskRules.IsReminderRelevant(task, 1));
        task.CropCycleId = null;
        task.Status = 0;
        Assert.True(TaskRules.IsReminderRelevant(task, null));
    }

    [Fact]
    public void ChangedDueDateHasADifferentReminderKey()
    {
        var id = Guid.NewGuid();
        var today = new DateOnly(2026, 10, 20);
        Assert.Equal(TaskRules.ReminderKey(id, today), TaskRules.ReminderKey(id, today));
        Assert.NotEqual(TaskRules.ReminderKey(id, today),
            TaskRules.ReminderKey(id, today.AddDays(2)));
    }

    [Fact]
    public void TaskUpdatesRequireValidDatesAssigneesAndDescriptions()
    {
        var version = Convert.ToBase64String(new byte[8]);
        Assert.Null(TaskRules.ValidateUpdate(new FarmTaskUpdateInput(
            new DateOnly(2026, 10, 20), "Water plants", null, version)));
        Assert.NotNull(TaskRules.ValidateUpdate(new FarmTaskUpdateInput(
            default, null, null, version)));
        Assert.NotNull(TaskRules.ValidateUpdate(new FarmTaskUpdateInput(
            new DateOnly(2026, 10, 20), new string('x', 501), null, version)));
        Assert.NotNull(TaskRules.ValidateUpdate(new FarmTaskUpdateInput(
            new DateOnly(2026, 10, 20), null, Guid.Empty, version)));
    }

    [Fact]
    public void FarmDateUsesConfiguredFarmTimeZone()
    {
        var utc = new DateTime(2026, 10, 20, 17, 0, 0, DateTimeKind.Utc);
        Assert.Equal(new DateOnly(2026, 10, 21), TaskRules.FarmDate("Asia/Manila", utc));
    }
}
