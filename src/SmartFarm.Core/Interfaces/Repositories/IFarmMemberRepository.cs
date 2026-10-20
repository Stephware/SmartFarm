using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IFarmMemberRepository
{
Task<FarmMember?> GetActiveAsync(Guid farmId, Guid userId, CancellationToken ct = default);
    Task<IReadOnlyList<FarmMember>> ListActiveAsync(Guid farmId, CancellationToken ct = default);
    void Add(FarmMember member);
    Task<IReadOnlyList<SmartFarm.Core.Contracts.Farms.FarmMemberDetails>> ListDetailsAsync(Guid farmId, CancellationToken ct = default);
    Task<IReadOnlyList<FarmMember>> ListForUserAsync(Guid farmId, Guid userId, CancellationToken ct = default);
    Task<FarmMember?> GetByMemberIdAsync(Guid farmId, Guid memberId, CancellationToken ct = default);
    Task<byte?> GetActiveRoleAsync(Guid farmId, Guid userId, CancellationToken ct = default);

}
