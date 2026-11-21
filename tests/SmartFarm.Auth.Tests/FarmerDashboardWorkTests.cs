using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Web.Presentation;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FarmerDashboardWorkTests
{
    private static readonly Guid FarmId = Guid.NewGuid();
    private static FarmTaskDetails Task(int offset, byte status = 0) => new(
        Guid.NewGuid(), FarmId, null, null, 1,
        new DateOnly(2026, 11, 12).AddDays(offset),
        status, null, DateTime.UtcNow, null, "v");

    [Fact]
    public void PrioritiesCountOverdueTodayAndFutureWithoutClosedTasks()
    {
        var overdue = Task(-3);
        var today = Task(0);
        var future = Task(2);
        var summary = DashboardWorkSummary.Build(
            [future, today, Task(-5, status: 1), overdue],
            new DateOnly(2026, 11, 12));
        Assert.Equal(1, summary.OverdueCount);
        Assert.Equal(1, summary.DueTodayCount);
        Assert.Equal(1, summary.UpcomingCount);
        Assert.Equal(3, summary.OpenCount);
        Assert.Equal([overdue.Id, today.Id], summary.DueTasks.Select(t => t.Id));
        Assert.Equal(future.Id, Assert.Single(summary.UpcomingTasks).Id);
    }

    [Fact]
    public void LimitsAffectPreviewsNotActualFarmTaskCounts()
    {
        var tasks = Enumerable.Range(0, 8).Select(i => Task(i - 4)).ToArray();
        var summary = DashboardWorkSummary.Build(tasks, new DateOnly(2026, 11, 12),
            dueLimit: 2, upcomingLimit: 1);
        Assert.Equal(4, summary.OverdueCount);
        Assert.Equal(1, summary.DueTodayCount);
        Assert.Equal(3, summary.UpcomingCount);
        Assert.Equal(8, summary.OpenCount);
        Assert.Equal(2, summary.DueTasks.Count);
        Assert.Single(summary.UpcomingTasks);
    }

    [Fact]
    public void EmptyOrAllClosedTasksShowNoFalseUrgency()
    {
        var today = new DateOnly(2026, 11, 12);
        var empty = DashboardWorkSummary.Build([], today);
        var closed = DashboardWorkSummary.Build([Task(-1, 1)], today);
        Assert.Equal(0, empty.OpenCount);
        Assert.Equal(0, closed.OverdueCount);
        Assert.Empty(empty.DueTasks);
        Assert.Empty(closed.UpcomingTasks);
    }
}
