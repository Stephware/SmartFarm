using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public sealed record InventoryActivityRow(Guid Id, byte ActivityType, DateTime OccurredAtUtc, string PlotName);

public interface ICropActivityRepository
{
Task<IReadOnlyList<CropActivity>> ListForCycleAsync(Guid cycleId, CancellationToken ct = default);
    Task<CropActivity?> GetByIdAsync(Guid cycleId, Guid activityId, CancellationToken ct = default);
    void Add(CropActivity activity);
    Task<IReadOnlyList<InventoryActivityRow>> ListActiveFarmOptionsAsync(Guid farmId, CancellationToken ct = default);
    Task<bool> BelongsToActiveFarmCycleAsync(Guid farmId, Guid activityId, CancellationToken ct = default);

}
