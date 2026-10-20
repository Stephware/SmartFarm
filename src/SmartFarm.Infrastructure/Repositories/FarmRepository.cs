using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class FarmRepository(SmartFarmDbContext db) : IFarmRepository
{
public Task<Farm?> GetActiveAsync(Guid farmId, CancellationToken ct = default) =>
        db.Farms.SingleOrDefaultAsync(f => f.Id == farmId && f.ArchivedAtUtc == null, ct);

    public async Task<IReadOnlyList<Farm>> ListAccessibleAsync(Guid userId, CancellationToken ct = default) =>
        await db.Farms.AsNoTracking()
            .Where(f => f.ArchivedAtUtc == null &&
                (f.OwnerUserId == userId || db.FarmMembers.Any(m =>
                    m.FarmId == f.Id && m.UserId == userId && m.RemovedAtUtc == null)))
            .OrderBy(f => f.Name).ToListAsync(ct);

    public void Add(Farm farm) => db.Farms.Add(farm);

    public void SetOriginalRowVersion(Farm farm, byte[] rowVersion) =>
        db.Entry(farm).Property(f => f.RowVersion).OriginalValue = rowVersion;
    public async Task<IReadOnlyList<SmartFarm.Core.Contracts.Farms.FarmSummary>> ListSummariesAsync(Guid actor,
        CancellationToken ct = default) =>
        await db.Farms.AsNoTracking().Where(f => f.ArchivedAtUtc == null &&
            (f.OwnerUserId == actor || db.FarmMembers.Any(m =>
                m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null)))
            .OrderBy(f => f.Name).Select(f => new SmartFarm.Core.Contracts.Farms.FarmSummary(
                f.Id, f.Name, f.LocationText,
                f.OwnerUserId == actor ? "Owner" :
                db.FarmMembers.Where(m => m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null)
                    .Select(m => m.Role).FirstOrDefault() == 1 ? "Manager" : "FieldWorker",
                f.TimeZoneId)).ToListAsync(ct);

    public Task<Guid?> GetOwnerIdAsync(Guid farmId, CancellationToken ct = default) =>
        db.Farms.AsNoTracking().Where(f => f.Id == farmId && f.ArchivedAtUtc == null)
            .Select(f => (Guid?)f.OwnerUserId).SingleOrDefaultAsync(ct);

}
