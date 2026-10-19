using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IObservationPhotoRepository
{
Task<ObservationPhoto?> GetByIdAsync(Guid observationId, Guid photoId, CancellationToken ct = default);
    Task<IReadOnlyList<ObservationPhoto>> ListForObservationAsync(Guid observationId,
        CancellationToken ct = default);
    void Add(ObservationPhoto photo);
}
