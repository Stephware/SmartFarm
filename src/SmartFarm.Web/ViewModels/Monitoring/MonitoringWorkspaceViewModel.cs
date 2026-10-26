using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Tasks;

namespace SmartFarm.Web.ViewModels.Monitoring;

public sealed record MonitoringWorkspaceViewModel(
    CropCycleDetails Cycle, MonitoringDetails Monitoring,
    IReadOnlyList<FarmTaskDetails> Tasks)
{
    public RecordActivityInput Activity { get; init; } = new() { ActivityType = 1 };
    public RecordObservationInput Observation { get; init; } = new();
}

