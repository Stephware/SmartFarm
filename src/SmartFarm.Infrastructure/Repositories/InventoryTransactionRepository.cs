using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class InventoryTransactionRepository(SmartFarmDbContext db) : IInventoryTransactionRepository
{
public async Task<IReadOnlyList<InventoryTransaction>> ListForItemAsync(Guid itemId,
        CancellationToken ct = default) =>
        await db.InventoryTransactions.AsNoTracking()
            .Where(t => t.InventoryItemId == itemId)
            .OrderByDescending(t => t.OccurredAtUtc).ThenByDescending(t => t.Id).ToListAsync(ct);

    public Task<InventoryTransaction?> GetByIdAsync(Guid itemId, Guid transactionId,
        CancellationToken ct = default) =>
        db.InventoryTransactions.AsNoTracking()
            .SingleOrDefaultAsync(t => t.Id == transactionId && t.InventoryItemId == itemId, ct);

    public Task<bool> AnyForItemAsync(Guid itemId, CancellationToken ct = default) =>
        db.InventoryTransactions.AnyAsync(t => t.InventoryItemId == itemId, ct);

    public Task<bool> HasReversalAsync(Guid transactionId, CancellationToken ct = default) =>
        db.InventoryTransactions.AnyAsync(t => t.ReversesTransactionId == transactionId, ct);

    public void Add(InventoryTransaction transaction) => db.InventoryTransactions.Add(transaction);
}
