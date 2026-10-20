using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Interfaces.Tasks;

namespace SmartFarm.Web.Clients;

public sealed class FarmTaskApiService(SmartFarmApiClient api) : IFarmTaskService
{
    private static string Tasks(Guid farmId) => $"api/farms/{farmId}/tasks";

    public async Task<IReadOnlyList<FarmTaskDetails>> ListAsync(
        Guid farmId, Guid actor, CancellationToken ct = default) =>
        await api.ReadAsync<List<FarmTaskDetails>>(Tasks(farmId), actor, ct);

    public Task<FarmTaskDetails> CreateAsync(Guid farmId, Guid actor, FarmTaskInput input,
        CancellationToken ct = default) =>
        api.WriteAsync<FarmTaskDetails>(HttpMethod.Post, Tasks(farmId), actor, input, ct);

    public Task<FarmTaskDetails> CloseAsync(Guid farmId, Guid taskId, Guid actor,
        FarmTaskCloseInput input, CancellationToken ct = default) =>
        api.WriteAsync<FarmTaskDetails>(HttpMethod.Put, $"{Tasks(farmId)}/{taskId}/close", actor, input, ct);

    public async Task<IReadOnlyList<NotificationDetails>> NotificationsAsync(
        Guid actor, CancellationToken ct = default) =>
        await api.ReadAsync<List<NotificationDetails>>("api/notifications", actor, ct);

    public Task MarkNotificationReadAsync(Guid actor, Guid notificationId,
        CancellationToken ct = default) =>
        api.WriteAsync(HttpMethod.Put, $"api/notifications/{notificationId}/read", actor, null, ct);
}
