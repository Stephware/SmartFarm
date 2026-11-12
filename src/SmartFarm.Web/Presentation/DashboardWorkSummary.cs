using SmartFarm.Core.Contracts.Tasks;

namespace SmartFarm.Web.Presentation;

// The farm's local date is provided by the saved dashboard report. Do not
// calculate it in the browser or combine records across farms.
public sealed record DashboardWorkSummary(
    int OverdueCount,
    int DueTodayCount,
    int UpcomingCount,
    int OpenCount,
    IReadOnlyList<FarmTaskDetails> DueTasks,
    IReadOnlyList<FarmTaskDetails> UpcomingTasks)
{
    public static DashboardWorkSummary Build(
        IEnumerable<FarmTaskDetails> tasks, DateOnly farmToday,
        int dueLimit = 5, int upcomingLimit = 3)
    {
        ArgumentOutOfRangeException.ThrowIfNegative(dueLimit);
        ArgumentOutOfRangeException.ThrowIfNegative(upcomingLimit);

        var open = tasks.Where(t => t.Status == 0)
            .OrderBy(t => t.DueDate).ThenBy(t => t.Id).ToArray();

        return new DashboardWorkSummary(
            open.Count(t => t.DueDate < farmToday),
            open.Count(t => t.DueDate == farmToday),
            open.Count(t => t.DueDate > farmToday),
            open.Length,
            open.Where(t => t.DueDate <= farmToday).Take(dueLimit).ToArray(),
            open.Where(t => t.DueDate > farmToday).Take(upcomingLimit).ToArray());
    }
}
