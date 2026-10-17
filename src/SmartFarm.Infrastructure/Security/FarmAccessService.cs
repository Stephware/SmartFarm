using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Security;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Security;

public sealed class FarmAccessService(SmartFarmDbContext database) : IFarmAccessService
{
    public async Task<FarmAccessLevel> GetAccessLevelAsync(
        Guid farmId, Guid userId, CancellationToken cancellationToken = default)
    {
        if (farmId == Guid.Empty || userId == Guid.Empty ||
            !await database.Users.AsNoTracking().AnyAsync(
                user => user.Id == userId && user.DisabledAtUtc == null, cancellationToken))
            return FarmAccessLevel.None;

        var ownerId = await database.Farms.AsNoTracking()
            .Where(farm => farm.Id == farmId && farm.ArchivedAtUtc == null)
            .Select(farm => (Guid?)farm.OwnerUserId)
            .SingleOrDefaultAsync(cancellationToken);

        if (ownerId is null) return FarmAccessLevel.None;
        if (ownerId == userId) return FarmAccessLevel.Owner;

        var role = await database.FarmMembers.AsNoTracking()
            .Where(member => member.FarmId == farmId && member.UserId == userId && member.RemovedAtUtc == null)
            .Select(member => (byte?)member.Role)
            .SingleOrDefaultAsync(cancellationToken);

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

