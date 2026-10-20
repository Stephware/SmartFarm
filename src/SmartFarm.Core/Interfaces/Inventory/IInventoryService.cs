using SmartFarm.Core.Contracts.Inventory;

namespace SmartFarm.Core.Interfaces.Inventory;

public interface IInventoryService
{
    Task<IReadOnlyList<InventoryActivityOption>> ActivityOptionsAsync(
        Guid farmId, Guid actor, CancellationToken ct = default);

    Task<InventoryWorkspace> ListAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<InventoryItemDetails?> GetAsync(Guid farmId, Guid itemId, Guid actor, CancellationToken ct = default);
    Task<InventoryItemDetails> CreateAsync(Guid farmId, Guid actor, InventoryItemInput input, CancellationToken ct = default);
    Task<InventoryItemDetails> UpdateAsync(Guid farmId, Guid itemId, Guid actor, InventoryItemInput input, CancellationToken ct = default);
    Task ArchiveAsync(Guid farmId, Guid itemId, Guid actor, string rowVersion, CancellationToken ct = default);
    Task<IReadOnlyList<InventoryTransactionDetails>> HistoryAsync(Guid farmId, Guid itemId, Guid actor, CancellationToken ct = default);
    Task<InventoryItemDetails> PostAsync(Guid farmId, Guid itemId, Guid actor, InventoryMovementInput input, CancellationToken ct = default);
    Task<InventoryItemDetails> ReverseAsync(Guid farmId, Guid itemId, Guid transactionId, Guid actor, InventoryReversalInput input, CancellationToken ct = default);
}


