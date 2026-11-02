using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;

namespace SmartFarm.Web.ViewModels.Crops;

public sealed record CropWorkspaceViewModel(
    FarmDetails Farm,
    IReadOnlyList<PlotDetails> Plots,
    IReadOnlyList<CropTypeDetails> CropTypes,
    IReadOnlyList<CropCycleDetails> Cycles)
{
    public CropAgendaMonth? Agenda { get; init; }
    public CropTypeInput CropType { get; init; } = new();
    public CropCyclePlanInput Plan { get; init; } = new();
    public Guid? PlanPlotId { get; init; }
    public CropCycleTransitionInput Transition { get; init; } = new();
    public Guid? TransitionCycleId { get; init; }
}

public sealed record CropPlanEditViewModel(
    FarmDetails Farm,
    CropCycleDetails Cycle,
    CropCyclePlanInput Input,
    IReadOnlyList<CropTypeDetails> CropTypes);

