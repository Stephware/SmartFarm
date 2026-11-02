using SmartFarm.Core.Contracts.Farms;

namespace SmartFarm.Web.ViewModels.Farms;

public sealed record FarmWorkspaceViewModel(
    FarmDetails Farm, IReadOnlyList<PlotDetails> Plots,
    IReadOnlyList<FarmMemberDetails> Members, FarmEnrollmentPanel? Enrollment = null);

public sealed record FarmEnrollmentPanel(Guid FarmId, FarmEnrollmentOverview? Overview,
    bool Dashboard = false, string? Error = null);

public sealed record FarmIndexViewModel(IReadOnlyList<FarmSummary> Farms,
    IReadOnlyList<FarmJoinRequestDetails> JoinRequests, FarmJoinRequestInput Join,
    string? RequestError = null);

public sealed record FarmDashboardViewModel(SmartFarm.Core.Contracts.Reports.FarmDashboard Report,
    FarmEnrollmentPanel? Enrollment);

public sealed record FarmEditViewModel(Guid FarmId, FarmInput Input);
public sealed record PlotEditViewModel(Guid FarmId, Guid PlotId, PlotInput Input);

