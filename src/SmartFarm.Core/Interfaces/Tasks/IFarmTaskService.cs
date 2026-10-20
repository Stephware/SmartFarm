using SmartFarm.Core.Contracts.Tasks;

namespace SmartFarm.Core.Interfaces.Tasks;

public interface IFarmTaskService
{
    Task<IReadOnlyList<FarmTaskDetails>> ListAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<FarmTaskDetails> CreateAsync(Guid farmId, Guid actor, FarmTaskInput input, CancellationToken ct = default);
    Task<FarmTaskDetails> UpdateAsync(Guid farmId, Guid taskId, Guid actor, FarmTaskUpdateInput input, CancellationToken ct = default);
    Task<FarmTaskDetails> CloseAsync(Guid farmId, Guid taskId, Guid actor, FarmTaskCloseInput input, CancellationToken ct = default);
    Task<IReadOnlyList<NotificationDetails>> NotificationsAsync(Guid actor, CancellationToken ct = default);
    Task MarkNotificationReadAsync(Guid actor, Guid notificationId, CancellationToken ct = default);
}
