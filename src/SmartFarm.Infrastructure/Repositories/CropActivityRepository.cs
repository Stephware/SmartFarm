using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class CropActivityRepository(SmartFarmDbContext db) : ICropActivityRepository
{
public async Task<IReadOnlyList<CropActivity>> ListForCycleAsync(Guid cycleId, CancellationToken ct = default) =>
        await db.CropActivities.AsNoTracking()
            .Where(a => a.CropCycleId == cycleId)
            .OrderByDescending(a => a.OccurredAtUtc).ToListAsync(ct);

    public Task<CropActivity?> GetByIdAsync(Guid cycleId, Guid activityId, CancellationToken ct = default) =>
        db.CropActivities.SingleOrDefaultAsync(a => a.Id == activityId && a.CropCycleId == cycleId, ct);

    public void Add(CropActivity activity) => db.CropActivities.Add(activity);
}
