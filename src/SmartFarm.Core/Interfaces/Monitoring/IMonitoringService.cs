using SmartFarm.Core.Contracts.Monitoring;

namespace SmartFarm.Core.Interfaces.Monitoring;

public interface IMonitoringService
{
    Task<MonitoringDetails> GetAsync(Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default);
    Task<ActivityDetails> RecordActivityAsync(Guid farmId, Guid cycleId, Guid actor,
        RecordActivityInput input, CancellationToken ct = default);
    Task<ObservationDetails> RecordObservationAsync(Guid farmId, Guid cycleId, Guid actor,
        RecordObservationInput input, CancellationToken ct = default);
    Task<PhotoDetails> AddPhotoAsync(Guid farmId, Guid cycleId, Guid observationId, Guid actor,
        Stream stream, long length, string? claimedContentType, CancellationToken ct = default);
    Task<PhotoDetails> AddPhotoWithIdAsync(Guid farmId, Guid cycleId,
        Guid observationId, Guid photoId, Guid actor, Stream stream, long length,
        string? claimedContentType, CancellationToken ct = default);
    Task<(Stream Stream, string ContentType)> OpenPhotoAsync(
        Guid farmId, Guid cycleId, Guid photoId, Guid actor, CancellationToken ct = default);
}

