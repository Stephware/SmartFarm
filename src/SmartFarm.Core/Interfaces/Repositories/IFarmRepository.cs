using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IFarmRepository
{
Task<Farm?> GetActiveAsync(Guid farmId, CancellationToken ct = default);
    Task<IReadOnlyList<Farm>> ListAccessibleAsync(Guid userId, CancellationToken ct = default);
    void Add(Farm farm);
    void SetOriginalRowVersion(Farm farm, byte[] rowVersion);
    Task<IReadOnlyList<SmartFarm.Core.Contracts.Farms.FarmSummary>> ListSummariesAsync(Guid actor, CancellationToken ct = default);
    Task<Guid?> GetOwnerIdAsync(Guid farmId, CancellationToken ct = default);

}
