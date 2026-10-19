using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class PlotRepository(SmartFarmDbContext db) : IPlotRepository
{
public Task<Plot?> GetActiveAsync(Guid farmId, Guid plotId, CancellationToken ct = default) =>
        db.Plots.SingleOrDefaultAsync(p =>
            p.Id == plotId && p.FarmId == farmId && p.ArchivedAtUtc == null, ct);

    public Task<Plot?> GetByIdAsync(Guid farmId, Guid plotId, CancellationToken ct = default) =>
        db.Plots.SingleOrDefaultAsync(p => p.Id == plotId && p.FarmId == farmId, ct);

    public async Task<IReadOnlyList<Plot>> ListActiveAsync(Guid farmId, CancellationToken ct = default) =>
        await db.Plots.AsNoTracking().Where(p =>
            p.FarmId == farmId && p.ArchivedAtUtc == null).OrderBy(p => p.Name).ToListAsync(ct);

    public void Add(Plot plot) => db.Plots.Add(plot);

    public void SetOriginalRowVersion(Plot plot, byte[] rowVersion) =>
        db.Entry(plot).Property(p => p.RowVersion).OriginalValue = rowVersion;
}
