using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class ObservationPhotoRepository(SmartFarmDbContext db) : IObservationPhotoRepository
{
public Task<ObservationPhoto?> GetByIdAsync(Guid observationId, Guid photoId, CancellationToken ct = default) =>
        db.ObservationPhotos.AsNoTracking()
            .SingleOrDefaultAsync(p => p.Id == photoId && p.CropObservationId == observationId, ct);

    public async Task<IReadOnlyList<ObservationPhoto>> ListForObservationAsync(Guid observationId,
        CancellationToken ct = default) =>
        await db.ObservationPhotos.AsNoTracking()
            .Where(p => p.CropObservationId == observationId)
            .OrderBy(p => p.UploadedAtUtc).ToListAsync(ct);

    public void Add(ObservationPhoto photo) => db.ObservationPhotos.Add(photo);
    public async Task<IReadOnlyList<ObservationPhoto>> ListForObservationsAsync(Guid[] observationIds,
        CancellationToken ct = default) =>
        await db.ObservationPhotos.AsNoTracking()
            .Where(p => observationIds.Contains(p.CropObservationId))
            .OrderBy(p => p.UploadedAtUtc).ToListAsync(ct);

    public Task<ObservationPhoto?> GetForCycleAsync(Guid cycleId, Guid photoId, CancellationToken ct = default) =>
        db.ObservationPhotos.AsNoTracking().SingleOrDefaultAsync(p =>
            p.Id == photoId && db.CropObservations.Any(o =>
                o.Id == p.CropObservationId && o.CropCycleId == cycleId), ct);

}
