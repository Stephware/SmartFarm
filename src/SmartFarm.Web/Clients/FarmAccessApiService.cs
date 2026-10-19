using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Security;

namespace SmartFarm.Web.Clients;

public sealed class FarmAccessApiService : IFarmAccessService
{
    private readonly SmartFarmApiClient _api;

    public FarmAccessApiService(SmartFarmApiClient api)
    {
        _api = api;
    }

    public async Task<FarmAccessLevel> GetAccessLevelAsync(Guid farmId, Guid userId,
        CancellationToken cancellationToken = default)
    {
        var access = await _api.ReadOptionalAsync<FarmAccessResult>(
            $"api/farms/{farmId}/access", userId, cancellationToken);
        return Enum.TryParse<FarmAccessLevel>(access?.Role, out var role)
            ? role : FarmAccessLevel.None;
    }

    public async Task<bool> CanAsync(Guid farmId, Guid userId, FarmAction action,
        CancellationToken cancellationToken = default) =>
        FarmAccessRules.Allows(await GetAccessLevelAsync(farmId, userId, cancellationToken), action);

    private sealed record FarmAccessResult(string Role);
}
