using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Tasks;

namespace SmartFarm.Web.ViewModels.Monitoring;

public sealed record MonitoringWorkspaceViewModel(
    CropCycleDetails Cycle, MonitoringDetails Monitoring,
    IReadOnlyList<FarmTaskDetails> Tasks)
{
    public string? Quick { get; init; }
    public string? ErrorForm { get; init; }
    public IReadOnlyList<InventoryItemDetails> Supplies { get; init; } = [];
    public ActivitySupplyInput Supply { get; init; } = new();
    public string? SupplyError { get; init; }
    public bool CanManageIssues { get; init; }
    public Guid ActorId { get; init; }
    public RecordActivityInput Activity { get; init; } = new() { ActivityType = 1 };
    public RecordObservationInput Observation { get; init; } = new();
}


// The selected option carries the version displayed with its balance; never fetch a new version on POST.
public sealed class ActivitySupplyInput
{
    public bool UseStock => !string.IsNullOrEmpty(Selection);
    public string? Selection { get; set; }
    public decimal? QuantityUsed { get; set; }
    [StringLength(300)] public string? Reason { get; set; }
}
