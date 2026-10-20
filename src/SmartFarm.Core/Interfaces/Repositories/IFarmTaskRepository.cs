using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IFarmTaskRepository
{
    Task<IReadOnlyList<FarmTask>> ListAsync(Guid farmId, CancellationToken ct = default);
    Task<FarmTask?> FindAsync(Guid farmId, Guid taskId, CancellationToken ct = default);
    Task<bool> CropBelongsToFarmAsync(Guid farmId, Guid cycleId, CancellationToken ct = default);
    Task<bool> IsAssignableAsync(Guid farmId, Guid userId, CancellationToken ct = default);
    Task<IReadOnlyList<FarmTask>> ListPendingForCycleAsync(Guid cycleId, CancellationToken ct = default);
    Task<FarmTask?> FindDueAsync(Guid farmId, Guid cycleId, byte taskType, DateOnly today,
        Guid actor, bool mayManage, CancellationToken ct = default);
    Task<string?> GetFarmTimeZoneAsync(Guid farmId, CancellationToken ct = default);
    Task DismissRemindersAsync(Guid taskId, DateTime closedAtUtc, CancellationToken ct = default);
    void Add(FarmTask task);
    void SetOriginalVersion(FarmTask task, byte[] version);
    Task<IReadOnlyList<Notification>> ListNotificationsAsync(Guid userId, CancellationToken ct = default);
    Task<Notification?> FindNotificationAsync(Guid userId, Guid notificationId, CancellationToken ct = default);
}
