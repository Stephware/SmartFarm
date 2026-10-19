using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class CropCycleRepository(SmartFarmDbContext db) : ICropCycleRepository
{
public Task<CropCycle?> GetForFarmAsync(Guid farmId, Guid cycleId, bool requireActivePlot,
        CancellationToken ct = default) =>
        db.CropCycles.SingleOrDefaultAsync(c =>
            c.Id == cycleId && db.Plots.Any(p => p.Id == c.PlotId &&
                p.FarmId == farmId && (!requireActivePlot || p.ArchivedAtUtc == null)), ct);

    public async Task<IReadOnlyList<CropCycle>> ListForFarmAsync(Guid farmId, CancellationToken ct = default) =>
        await db.CropCycles.AsNoTracking()
            .Where(c => db.Plots.Any(p => p.Id == c.PlotId && p.FarmId == farmId))
            .OrderByDescending(c => c.PlannedPlantingDate).ToListAsync(ct);

    public Task<bool> HasStatusOnPlotAsync(Guid plotId, byte status, CancellationToken ct = default) =>
        db.CropCycles.AnyAsync(c => c.PlotId == plotId && c.Status == status, ct);

    public void Add(CropCycle cycle) => db.CropCycles.Add(cycle);

    public void SetOriginalRowVersion(CropCycle cycle, byte[] rowVersion) =>
        db.Entry(cycle).Property(c => c.RowVersion).OriginalValue = rowVersion;
    public Task<bool> HasActiveForFarmAsync(Guid farmId, CancellationToken ct = default) =>
        db.CropCycles.AnyAsync(c => c.Status == 1 && db.Plots.Any(p =>
            p.Id == c.PlotId && p.FarmId == farmId), ct);

}
