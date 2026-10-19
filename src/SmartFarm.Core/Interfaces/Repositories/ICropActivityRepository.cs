using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface ICropActivityRepository
{
Task<IReadOnlyList<CropActivity>> ListForCycleAsync(Guid cycleId, CancellationToken ct = default);
    Task<CropActivity?> GetByIdAsync(Guid cycleId, Guid activityId, CancellationToken ct = default);
    void Add(CropActivity activity);
}
