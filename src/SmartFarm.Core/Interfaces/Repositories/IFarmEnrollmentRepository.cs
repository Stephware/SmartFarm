using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IFarmEnrollmentRepository
{
    Task<Farm?> FindFarmByCodeForUpdateAsync(string code, CancellationToken ct);
    Task<Farm?> FindFarmForUpdateAsync(Guid farmId, CancellationToken ct);
    Task<bool> IsActiveUserAsync(Guid userId, CancellationToken ct);
    Task<FarmJoinRequest?> FindPendingAsync(Guid farmId, Guid userId, CancellationToken ct);
    Task<FarmJoinRequest?> FindAsync(Guid farmId, Guid requestId, CancellationToken ct);
    Task<FarmJoinRequestDetails> DetailsAsync(Guid requestId, CancellationToken ct);
    Task<IReadOnlyList<FarmJoinRequestDetails>> ListForUserAsync(Guid userId, CancellationToken ct);
    Task<IReadOnlyList<FarmJoinRequestDetails>> ListPendingAsync(Guid farmId, CancellationToken ct);
    void Add(FarmJoinRequest request);
}
