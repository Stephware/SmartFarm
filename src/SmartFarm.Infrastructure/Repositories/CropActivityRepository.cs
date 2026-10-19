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
    public async Task<IReadOnlyList<InventoryActivityRow>> ListActiveFarmOptionsAsync(Guid farmId, CancellationToken ct = default) =>
        await (from a in db.CropActivities.AsNoTracking()
            join c in db.CropCycles.AsNoTracking() on a.CropCycleId equals c.Id
            join p in db.Plots.AsNoTracking() on c.PlotId equals p.Id
            where p.FarmId == farmId && p.ArchivedAtUtc == null && c.Status == 1
            orderby a.OccurredAtUtc descending
            select new InventoryActivityRow(a.Id, a.ActivityType, a.OccurredAtUtc, p.Name))
            .Take(100).ToListAsync(ct);

    public Task<bool> BelongsToActiveFarmCycleAsync(Guid farmId, Guid activityId, CancellationToken ct = default) =>
        db.CropActivities.AsNoTracking().AnyAsync(a => a.Id == activityId &&
            db.CropCycles.Any(c => c.Id == a.CropCycleId && c.Status == 1 &&
                db.Plots.Any(p => p.Id == c.PlotId && p.FarmId == farmId && p.ArchivedAtUtc == null)), ct);

}
