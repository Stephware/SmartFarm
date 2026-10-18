using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Monitoring;

namespace SmartFarm.Web.ViewModels.Monitoring;

public sealed record MonitoringWorkspaceViewModel(
    CropCycleDetails Cycle, MonitoringDetails Monitoring);

