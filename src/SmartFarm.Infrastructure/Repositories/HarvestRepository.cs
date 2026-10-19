using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class HarvestRepository(SmartFarmDbContext db) : IHarvestRepository
{
public Task<Harvest?> GetByIdAsync(Guid cycleId, Guid harvestId, CancellationToken ct = default) =>
        db.Harvests.AsNoTracking()
            .SingleOrDefaultAsync(h => h.Id == harvestId && h.CropCycleId == cycleId, ct);

    public async Task<IReadOnlyList<Harvest>> ListForCycleAsync(Guid cycleId, CancellationToken ct = default) =>
        await db.Harvests.AsNoTracking()
            .Where(h => h.CropCycleId == cycleId)
            .OrderByDescending(h => h.HarvestedAtUtc)
            .ThenByDescending(h => h.Id).ToListAsync(ct);

    // This is one conditional SQL UPDATE, not read-modify-write. It prevents double voids.
    public async Task<bool> TryVoidAsync(Guid cycleId, Guid harvestId, Guid actorId,
        DateTime voidedAtUtc, string reason, CancellationToken ct = default) =>
        await db.Harvests.Where(h => h.CropCycleId == cycleId &&
                h.Id == harvestId && h.VoidedAtUtc == null)
            .ExecuteUpdateAsync(setters => setters
                .SetProperty(h => h.VoidedAtUtc, voidedAtUtc)
                .SetProperty(h => h.VoidedByUserId, actorId)
                .SetProperty(h => h.VoidReason, reason), ct) == 1;

    public void Add(Harvest harvest) => db.Harvests.Add(harvest);
}
