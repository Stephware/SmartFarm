using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Interfaces.Inventory;

namespace SmartFarm.Web.Clients;

public sealed class InventoryApiService : IInventoryService
{
    private readonly SmartFarmApiClient _api;

    public InventoryApiService(SmartFarmApiClient api)
    {
        _api = api;
    }

    private static string Items(Guid farmId) => $"api/farms/{farmId}/inventory";
    private static string Item(Guid farmId, Guid itemId) => $"{Items(farmId)}/{itemId}";

    public async Task<IReadOnlyList<InventoryActivityOption>> ActivityOptionsAsync(Guid farmId, Guid actor,
        CancellationToken ct = default) =>
        await _api.ReadAsync<List<InventoryActivityOption>>($"{Items(farmId)}/activities", actor, ct);

    public Task<InventoryWorkspace> ListAsync(Guid farmId, Guid actor, CancellationToken ct = default) =>
        _api.ReadAsync<InventoryWorkspace>(Items(farmId), actor, ct);

    public Task<InventoryItemDetails?> GetAsync(Guid farmId, Guid itemId, Guid actor,
        CancellationToken ct = default) =>
        _api.ReadOptionalAsync<InventoryItemDetails>(Item(farmId, itemId), actor, ct);

    public Task<InventoryItemDetails> CreateAsync(Guid farmId, Guid actor, InventoryItemInput input,
        CancellationToken ct = default) =>
        _api.WriteAsync<InventoryItemDetails>(HttpMethod.Post, Items(farmId), actor, input, ct);

    public Task<InventoryItemDetails> UpdateAsync(Guid farmId, Guid itemId, Guid actor, InventoryItemInput input,
        CancellationToken ct = default) =>
        _api.WriteAsync<InventoryItemDetails>(HttpMethod.Put, Item(farmId, itemId), actor, input, ct);

    public Task ArchiveAsync(Guid farmId, Guid itemId, Guid actor, string rowVersion,
        CancellationToken ct = default) =>
        _api.WriteAsync(HttpMethod.Delete, Item(farmId, itemId), actor,
            new RowVersionInput { RowVersion = rowVersion }, ct);

    public async Task<IReadOnlyList<InventoryTransactionDetails>> HistoryAsync(Guid farmId, Guid itemId,
        Guid actor, CancellationToken ct = default) =>
        await _api.ReadAsync<List<InventoryTransactionDetails>>(
            $"{Item(farmId, itemId)}/transactions", actor, ct);

    public Task<InventoryItemDetails> PostAsync(Guid farmId, Guid itemId, Guid actor,
        InventoryMovementInput input, CancellationToken ct = default) =>
        _api.WriteAsync<InventoryItemDetails>(HttpMethod.Post,
            $"{Item(farmId, itemId)}/transactions", actor, input, ct);

    public Task<InventoryItemDetails> ReverseAsync(Guid farmId, Guid itemId, Guid transactionId,
        Guid actor, InventoryReversalInput input, CancellationToken ct = default) =>
        _api.WriteAsync<InventoryItemDetails>(HttpMethod.Post,
            $"{Item(farmId, itemId)}/transactions/{transactionId}/reverse", actor, input, ct);
}
