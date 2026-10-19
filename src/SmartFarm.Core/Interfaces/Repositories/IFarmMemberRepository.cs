using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IFarmMemberRepository
{
Task<FarmMember?> GetActiveAsync(Guid farmId, Guid userId, CancellationToken ct = default);
    Task<IReadOnlyList<FarmMember>> ListActiveAsync(Guid farmId, CancellationToken ct = default);
    void Add(FarmMember member);
}
