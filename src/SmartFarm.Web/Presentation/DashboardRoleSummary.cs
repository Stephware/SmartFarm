using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;

namespace SmartFarm.Web.Presentation;

// Phase 12C: derives role-specific display data from records already authorized
// for the selected farm. It never changes access rights or mutates those records.
public sealed record DashboardRoleSummary(
    bool IsOwner,
    bool IsManager,
    bool IsFieldWorker,
    DashboardWorkSummary Work,
    IReadOnlyList<CropCycleDetails> ActiveCycles,
    IReadOnlySet<Guid> VisibleOpenTaskIds)
{
    public static DashboardRoleSummary Build(
        string? role, IEnumerable<FarmTaskDetails> tasks,
        IEnumerable<CropCycleDetails> cycles, Guid actorId, DateOnly farmToday)
    {
        var owner = role == "Owner";
        var manager = role == "Manager";
        var worker = !owner && !manager;
        var visible = (worker
            ? tasks.Where(t => t.AssignedToUserId == actorId)
            : tasks).ToArray();
        var active = cycles.Where(c => c.Status == CropCycleStatus.Active)
            .OrderBy(c => c.PlotName, StringComparer.OrdinalIgnoreCase)
            .ThenBy(c => c.CropTypeName, StringComparer.OrdinalIgnoreCase)
            .ThenBy(c => c.Id)
            .ToArray();

        return new DashboardRoleSummary(owner, manager, worker,
            DashboardWorkSummary.Build(visible, farmToday), active,
            visible.Where(t => t.Status == 0).Select(t => t.Id).ToHashSet());
    }
}
