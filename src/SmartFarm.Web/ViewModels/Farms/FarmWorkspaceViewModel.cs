using SmartFarm.Core.Contracts.Farms;

namespace SmartFarm.Web.ViewModels.Farms;

public sealed record FarmWorkspaceViewModel(
    FarmDetails Farm, IReadOnlyList<PlotDetails> Plots,
    IReadOnlyList<FarmMemberDetails> Members);

public sealed record FarmEditViewModel(Guid FarmId, FarmInput Input);
public sealed record PlotEditViewModel(Guid FarmId, Guid PlotId, PlotInput Input);

