using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class InventoryItemRepository(SmartFarmDbContext db) : IInventoryItemRepository
{
public Task<InventoryItem?> GetActiveAsync(Guid farmId, Guid itemId, CancellationToken ct = default) =>
        db.InventoryItems.SingleOrDefaultAsync(i =>
            i.FarmId == farmId && i.Id == itemId && i.ArchivedAtUtc == null, ct);

    public Task<InventoryItem?> GetByIdAsync(Guid farmId, Guid itemId, CancellationToken ct = default) =>
        db.InventoryItems.SingleOrDefaultAsync(i => i.FarmId == farmId && i.Id == itemId, ct);

    public async Task<IReadOnlyList<InventoryItem>> ListActiveAsync(Guid farmId, CancellationToken ct = default) =>
        await db.InventoryItems.AsNoTracking()
            .Where(i => i.FarmId == farmId && i.ArchivedAtUtc == null)
            .OrderBy(i => i.Name).ToListAsync(ct);

    public Task<bool> HasPositiveStockAsync(Guid farmId, CancellationToken ct = default) =>
        db.InventoryItems.AnyAsync(i => i.FarmId == farmId &&
            i.ArchivedAtUtc == null && i.QuantityOnHand > 0, ct);

    public void Add(InventoryItem item) => db.InventoryItems.Add(item);

    public void SetOriginalRowVersion(InventoryItem item, byte[] rowVersion) =>
        db.Entry(item).Property(i => i.RowVersion).OriginalValue = rowVersion;
}
