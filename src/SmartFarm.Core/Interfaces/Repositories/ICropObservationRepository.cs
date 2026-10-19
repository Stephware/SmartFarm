using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface ICropObservationRepository
{
Task<IReadOnlyList<CropObservation>> ListForCycleAsync(Guid cycleId, CancellationToken ct = default);
    Task<CropObservation?> GetByIdAsync(Guid cycleId, Guid observationId, CancellationToken ct = default);
    void Add(CropObservation observation);
}
