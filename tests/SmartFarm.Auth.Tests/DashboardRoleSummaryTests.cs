using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using SmartFarm.Web.Presentation;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class DashboardRoleSummaryTests
{
    private static readonly Guid FarmId = Guid.NewGuid();
    private static readonly Guid WorkerId = Guid.NewGuid();
    private static readonly DateOnly Today = new(2026, 11, 13);

    private static FarmTaskDetails Task(int dayOffset, Guid? assignee, byte status = 0) =>
        new(Guid.NewGuid(), FarmId, null, assignee, 1,
            Today.AddDays(dayOffset), status, "Check the field",
            DateTime.UtcNow, null, "v");

    private static CropCycleDetails Cycle(string plot, string crop, CropCycleStatus status) =>
        new(Guid.NewGuid(), FarmId, Guid.NewGuid(), plot, Guid.NewGuid(),
            crop, status, Today, Today, Today.AddDays(60), "kg", 2,
            null, null, "v");

    [Fact]
    public void WorkerDashboardCountsOnlyTheirOpenAssignments()
    {
        var mineLate = Task(-2, WorkerId);
        var mineToday = Task(0, WorkerId);
        var otherLate = Task(-1, Guid.NewGuid());
        var closed = Task(-3, WorkerId, 1);
        var summary = DashboardRoleSummary.Build("FieldWorker",
            [otherLate, closed, mineToday, mineLate], [], WorkerId, Today);
        Assert.True(summary.IsFieldWorker);
        Assert.Equal(1, summary.Work.OverdueCount);
        Assert.Equal(1, summary.Work.DueTodayCount);
        Assert.Equal(2, summary.Work.OpenCount);
        Assert.Equal([mineLate.Id, mineToday.Id], summary.Work.DueTasks.Select(t => t.Id));
        Assert.Contains(mineToday.Id, summary.VisibleOpenTaskIds);
        Assert.DoesNotContain(otherLate.Id, summary.VisibleOpenTaskIds);
        Assert.DoesNotContain(closed.Id, summary.VisibleOpenTaskIds);
    }

    [Theory]
    [InlineData("Owner")]
    [InlineData("Manager")]
    public void ManagersUseAllAuthorizedFarmTasks(string role)
    {
        var late = Task(-1, WorkerId);
        var notAssigned = Task(0, Guid.NewGuid());
        var summary = DashboardRoleSummary.Build(role,
            [late, notAssigned], [], WorkerId, Today);
        Assert.False(summary.IsFieldWorker);
        Assert.Equal(1, summary.Work.OverdueCount);
        Assert.Equal(1, summary.Work.DueTodayCount);
        Assert.Equal(2, summary.Work.OpenCount);
        Assert.Equal(role == "Owner", summary.IsOwner);
        Assert.Equal(role == "Manager", summary.IsManager);
    }

    [Fact]
    public void CropChoicesAreExplicitSortedAndExcludeNonActiveCycles()
    {
        var north = Cycle("North plot", "Tomato", CropCycleStatus.Active);
        var south = Cycle("South plot", "Rice", CropCycleStatus.Active);
        var planted = Cycle("East plot", "Corn", CropCycleStatus.Planned);
        var cancelled = Cycle("West plot", "Eggplant", CropCycleStatus.Cancelled);
        var summary = DashboardRoleSummary.Build("Manager", [],
            [south, cancelled, planted, north], WorkerId, Today);
        Assert.Equal([north.Id, south.Id], summary.ActiveCycles.Select(c => c.Id));
        Assert.DoesNotContain(summary.ActiveCycles, c => c.Id == planted.Id || c.Id == cancelled.Id);
    }

    [Fact]
    public void EmptyFarmMakesNoFalseWorkOrCropClaims()
    {
        var summary = DashboardRoleSummary.Build("FieldWorker", [], [], WorkerId, Today);
        Assert.Empty(summary.ActiveCycles);
        Assert.Empty(summary.VisibleOpenTaskIds);
        Assert.Equal(0, summary.Work.OverdueCount);
        Assert.Equal(0, summary.Work.OpenCount);
    }
}
