using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Interfaces.Monitoring;

namespace SmartFarm.Web.Clients;

public sealed class MonitoringApiService : IMonitoringService
{
    private readonly SmartFarmApiClient _api;

    public MonitoringApiService(SmartFarmApiClient api)
    {
        _api = api;
    }

    private static string Cycle(Guid farmId, Guid cycleId) =>
        $"api/farms/{farmId}/crop-cycles/{cycleId}/monitoring";

    public Task<MonitoringDetails> GetAsync(Guid farmId, Guid cycleId, Guid actor,
        CancellationToken ct = default) =>
        _api.ReadAsync<MonitoringDetails>(Cycle(farmId, cycleId), actor, ct);

    public Task<ActivityDetails> RecordActivityAsync(Guid farmId, Guid cycleId, Guid actor,
        RecordActivityInput input, CancellationToken ct = default) =>
        _api.WriteAsync<ActivityDetails>(HttpMethod.Post,
            $"{Cycle(farmId, cycleId)}/activities", actor, input, ct);

    public Task<ObservationDetails> RecordObservationAsync(Guid farmId, Guid cycleId, Guid actor,
        RecordObservationInput input, CancellationToken ct = default) =>
        _api.WriteAsync<ObservationDetails>(HttpMethod.Post,
            $"{Cycle(farmId, cycleId)}/observations", actor, input, ct);

    public Task<PhotoDetails> AddPhotoAsync(Guid farmId, Guid cycleId, Guid observationId, Guid actor,
        Stream stream, long length, string? claimedContentType, CancellationToken ct = default) =>
        _api.UploadAsync<PhotoDetails>(
            $"{Cycle(farmId, cycleId)}/observations/{observationId}/photos",
            actor, stream, length, claimedContentType, ct);

    public Task<PhotoDetails> AddPhotoWithIdAsync(Guid farmId, Guid cycleId,
        Guid observationId, Guid photoId, Guid actor, Stream stream, long length,
        string? claimedContentType, CancellationToken ct = default) =>
        _api.UploadAsync<PhotoDetails>(HttpMethod.Put,
            $"{Cycle(farmId, cycleId)}/observations/{observationId}/photos/{photoId}",
            actor, stream, length, claimedContentType, ct);

    public Task<(Stream Stream, string ContentType)> OpenPhotoAsync(
        Guid farmId, Guid cycleId, Guid photoId, Guid actor, CancellationToken ct = default) =>
        _api.DownloadAsync($"{Cycle(farmId, cycleId)}/photos/{photoId}", actor, ct);
}
