using SmartFarm.Core.Contracts.Farms;

namespace SmartFarm.Core.Interfaces.Farms;

public interface IFarmManagementService
{
    Task<IReadOnlyList<FarmSummary>> ListAsync(Guid actor, CancellationToken ct = default);
    Task<FarmDetails?> GetAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<FarmDetails> CreateAsync(Guid actor, FarmInput input, CancellationToken ct = default);
    Task<FarmDetails> UpdateAsync(Guid farmId, Guid actor, FarmInput input, CancellationToken ct = default);
    Task ArchiveAsync(Guid farmId, Guid actor, string rowVersion, CancellationToken ct = default);

    Task<IReadOnlyList<PlotDetails>> ListPlotsAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<PlotDetails?> GetPlotAsync(Guid farmId, Guid plotId, Guid actor, CancellationToken ct = default);
    Task<PlotDetails> CreatePlotAsync(Guid farmId, Guid actor, PlotInput input, CancellationToken ct = default);
    Task<PlotDetails> UpdatePlotAsync(Guid farmId, Guid plotId, Guid actor, PlotInput input, CancellationToken ct = default);
    Task ArchivePlotAsync(Guid farmId, Guid plotId, Guid actor, string rowVersion, CancellationToken ct = default);

    Task<IReadOnlyList<FarmMemberDetails>> MembersAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<InvitationDetails> CreateInvitationAsync(Guid farmId, Guid actor, InviteMemberInput input, CancellationToken ct = default);
    Task<Guid> AcceptInvitationAsync(Guid actor, string token, CancellationToken ct = default);
    Task ChangeMemberRoleAsync(Guid farmId, Guid memberId, Guid actor, byte role, CancellationToken ct = default);
    Task RemoveMemberAsync(Guid farmId, Guid memberId, Guid actor, CancellationToken ct = default);
}

