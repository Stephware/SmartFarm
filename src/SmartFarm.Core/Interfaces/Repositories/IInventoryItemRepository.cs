using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IInventoryItemRepository
{
Task<InventoryItem?> GetActiveAsync(Guid farmId, Guid itemId, CancellationToken ct = default);
    Task<InventoryItem?> GetByIdAsync(Guid farmId, Guid itemId, CancellationToken ct = default);
    Task<IReadOnlyList<InventoryItem>> ListActiveAsync(Guid farmId, CancellationToken ct = default);
    Task<bool> HasPositiveStockAsync(Guid farmId, CancellationToken ct = default);
    void Add(InventoryItem item);
    void SetOriginalRowVersion(InventoryItem item, byte[] rowVersion);
}
