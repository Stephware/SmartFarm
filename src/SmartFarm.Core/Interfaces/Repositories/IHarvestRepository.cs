using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IHarvestRepository
{
Task<Harvest?> GetByIdAsync(Guid cycleId, Guid harvestId, CancellationToken ct = default);
    Task<IReadOnlyList<Harvest>> ListForCycleAsync(Guid cycleId, CancellationToken ct = default);
    Task<bool> TryVoidAsync(Guid cycleId, Guid harvestId, Guid actorId,
        DateTime voidedAtUtc, string reason, CancellationToken ct = default);
    void Add(Harvest harvest);
}
