using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;

namespace SmartFarm.Web.Clients;

public sealed class FarmApiService(SmartFarmApiClient api) : IFarmManagementService
{
    private static string Farm(Guid farmId) => $"api/farms/{farmId}";

    public async Task<IReadOnlyList<FarmSummary>> ListAsync(Guid actor, CancellationToken ct = default) =>
        await api.ReadAsync<List<FarmSummary>>("api/farms", actor, ct);

    public Task<FarmDetails?> GetAsync(Guid farmId, Guid actor, CancellationToken ct = default) =>
        api.ReadOptionalAsync<FarmDetails>(Farm(farmId), actor, ct);

    public Task<FarmDetails> CreateAsync(Guid actor, FarmInput input, CancellationToken ct = default) =>
        api.WriteAsync<FarmDetails>(HttpMethod.Post, "api/farms", actor, input, ct);

    public Task<FarmDetails> UpdateAsync(Guid farmId, Guid actor, FarmInput input, CancellationToken ct = default) =>
        api.WriteAsync<FarmDetails>(HttpMethod.Put, Farm(farmId), actor, input, ct);

    public Task ArchiveAsync(Guid farmId, Guid actor, string rowVersion, CancellationToken ct = default) =>
        api.WriteAsync(HttpMethod.Delete, Farm(farmId), actor, new RowVersionInput { RowVersion = rowVersion }, ct);

    public async Task<IReadOnlyList<PlotDetails>> ListPlotsAsync(Guid farmId, Guid actor, CancellationToken ct = default) =>
        await api.ReadAsync<List<PlotDetails>>($"{Farm(farmId)}/plots", actor, ct);

    public Task<PlotDetails?> GetPlotAsync(Guid farmId, Guid plotId, Guid actor, CancellationToken ct = default) =>
        api.ReadOptionalAsync<PlotDetails>($"{Farm(farmId)}/plots/{plotId}", actor, ct);

    public Task<PlotDetails> CreatePlotAsync(Guid farmId, Guid actor, PlotInput input, CancellationToken ct = default) =>
        api.WriteAsync<PlotDetails>(HttpMethod.Post, $"{Farm(farmId)}/plots", actor, input, ct);

    public Task<PlotDetails> UpdatePlotAsync(Guid farmId, Guid plotId, Guid actor, PlotInput input, CancellationToken ct = default) =>
        api.WriteAsync<PlotDetails>(HttpMethod.Put, $"{Farm(farmId)}/plots/{plotId}", actor, input, ct);

    public Task ArchivePlotAsync(Guid farmId, Guid plotId, Guid actor, string rowVersion, CancellationToken ct = default) =>
        api.WriteAsync(HttpMethod.Delete, $"{Farm(farmId)}/plots/{plotId}", actor,
            new RowVersionInput { RowVersion = rowVersion }, ct);

    public async Task<IReadOnlyList<FarmMemberDetails>> MembersAsync(Guid farmId, Guid actor, CancellationToken ct = default) =>
        await api.ReadAsync<List<FarmMemberDetails>>($"{Farm(farmId)}/members", actor, ct);

    public Task<InvitationDetails> CreateInvitationAsync(Guid farmId, Guid actor, InviteMemberInput input,
        CancellationToken ct = default) =>
        api.WriteAsync<InvitationDetails>(HttpMethod.Post, $"{Farm(farmId)}/members/invitations", actor, input, ct);

    public async Task<Guid> AcceptInvitationAsync(Guid actor, string token, CancellationToken ct = default)
    {
        var result = await api.WriteAsync<InvitationResult>(
            HttpMethod.Post, "api/farm-invitations/accept", actor, new InvitationAcceptance { Token = token }, ct);
        return result.FarmId;
    }

    public Task ChangeMemberRoleAsync(Guid farmId, Guid memberId, Guid actor, byte role,
        CancellationToken ct = default) =>
        api.WriteAsync(HttpMethod.Put, $"{Farm(farmId)}/members/{memberId}",
            actor, new UpdateMemberInput { Role = role }, ct);

    public Task RemoveMemberAsync(Guid farmId, Guid memberId, Guid actor, CancellationToken ct = default) =>
        api.WriteAsync(HttpMethod.Delete, $"{Farm(farmId)}/members/{memberId}", actor, null, ct);

    private sealed record InvitationResult(Guid FarmId);
}
