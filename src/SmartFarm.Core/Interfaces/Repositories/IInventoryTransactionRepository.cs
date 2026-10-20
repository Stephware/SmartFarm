using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IInventoryTransactionRepository
{
Task<IReadOnlyList<InventoryTransaction>> ListForItemAsync(Guid itemId, CancellationToken ct = default);
    Task<InventoryTransaction?> GetByIdAsync(Guid itemId, Guid transactionId,
        CancellationToken ct = default);
    Task<bool> AnyForItemAsync(Guid itemId, CancellationToken ct = default);
    Task<bool> HasReversalAsync(Guid transactionId, CancellationToken ct = default);
    void Add(InventoryTransaction transaction);
}
