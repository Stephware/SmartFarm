using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;

namespace SmartFarm.Web.ViewModels.Crops;

public sealed record CropWorkspaceViewModel(
    FarmDetails Farm,
    IReadOnlyList<PlotDetails> Plots,
    IReadOnlyList<CropTypeDetails> CropTypes,
    IReadOnlyList<CropCycleDetails> Cycles);

public sealed record CropPlanEditViewModel(
    FarmDetails Farm,
    CropCycleDetails Cycle,
    CropCyclePlanInput Input,
    IReadOnlyList<CropTypeDetails> CropTypes);

