using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Interfaces.Security;

public interface IFarmAccessService
{
    Task<FarmAccessLevel> GetAccessLevelAsync(Guid farmId, Guid userId, CancellationToken cancellationToken = default);
    Task<bool> CanAsync(Guid farmId, Guid userId, FarmAction action, CancellationToken cancellationToken = default);
}

