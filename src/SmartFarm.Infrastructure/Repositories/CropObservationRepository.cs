using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class CropObservationRepository(SmartFarmDbContext db) : ICropObservationRepository
{
public async Task<IReadOnlyList<CropObservation>> ListForCycleAsync(Guid cycleId, CancellationToken ct = default) =>
        await db.CropObservations.AsNoTracking()
            .Where(o => o.CropCycleId == cycleId)
            .OrderByDescending(o => o.ObservedAtUtc).ToListAsync(ct);

    public Task<CropObservation?> GetByIdAsync(Guid cycleId, Guid observationId, CancellationToken ct = default) =>
        db.CropObservations.SingleOrDefaultAsync(o =>
            o.Id == observationId && o.CropCycleId == cycleId, ct);

    public void Add(CropObservation observation) => db.CropObservations.Add(observation);
}
