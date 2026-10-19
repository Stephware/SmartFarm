using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Services.Security;

namespace SmartFarm.Infrastructure.Security;

public sealed class FarmAccessService(IUserRepository people, IFarmRepository farms,
    IFarmMemberRepository members) : IFarmAccessService
{
    public async Task<FarmAccessLevel> GetAccessLevelAsync(
        Guid farmId, Guid userId, CancellationToken cancellationToken = default)
    {
        if (farmId == Guid.Empty || userId == Guid.Empty ||
            !await people.IsActiveAsync(userId, cancellationToken))
            return FarmAccessLevel.None;

        var ownerId = await farms.GetOwnerIdAsync(farmId, cancellationToken);

        if (ownerId is null) return FarmAccessLevel.None;
        if (ownerId == userId) return FarmAccessLevel.Owner;

        var role = await members.GetActiveRoleAsync(farmId, userId, cancellationToken);

        return role switch
        {
            1 => FarmAccessLevel.Manager,
            2 => FarmAccessLevel.FieldWorker,
            _ => FarmAccessLevel.None
        };
    }

    public async Task<bool> CanAsync(
        Guid farmId, Guid userId, FarmAction action, CancellationToken cancellationToken = default) =>
        FarmAccessRules.Allows(await GetAccessLevelAsync(farmId, userId, cancellationToken), action);
}

