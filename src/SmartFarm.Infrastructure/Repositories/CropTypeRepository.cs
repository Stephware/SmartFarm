using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class CropTypeRepository(SmartFarmDbContext db) : ICropTypeRepository
{
public Task<CropType?> GetActiveAsync(Guid cropTypeId, CancellationToken ct = default) =>
        db.CropTypes.SingleOrDefaultAsync(t => t.Id == cropTypeId && t.IsActive, ct);

    public async Task<IReadOnlyList<CropType>> ListActiveAsync(CancellationToken ct = default) =>
        await db.CropTypes.AsNoTracking()
            .Where(t => t.IsActive).OrderBy(t => t.Name).ToListAsync(ct);

    public Task<bool> NameExistsAsync(string name, CancellationToken ct = default) =>
        db.CropTypes.AnyAsync(t => t.Name == name, ct);

    public void Add(CropType cropType) => db.CropTypes.Add(cropType);
    public Task<string> GetNameAsync(Guid cropTypeId, CancellationToken ct = default) =>
        db.CropTypes.AsNoTracking().Where(t => t.Id == cropTypeId).Select(t => t.Name).SingleAsync(ct);

}
