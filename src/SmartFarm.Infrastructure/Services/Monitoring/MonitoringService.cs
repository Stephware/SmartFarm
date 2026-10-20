using System.Data;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Monitoring;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Services.Monitoring;

public sealed class MonitoringService(
    ICropCycleRepository cycles,
    ICropActivityRepository activities,
    IFarmTaskRepository tasks,
    ICropObservationRepository observations,
    IObservationPhotoRepository photos,
    IUnitOfWork unit,
    IFarmAccessService access,
    IObservationPhotoStore photoStore) : IMonitoringService
{
    private static FarmOperationException Missing() =>
        new(FarmError.NotFound, "Resource not found.");

    private static FarmOperationException Invalid(string msg) =>
        new(FarmError.InvalidInput, msg);
    private static FarmOperationException Conflict() =>
        new(FarmError.Conflict, "This farm task changed. Reload before recording the activity.");

    private static ActivityDetails Map(CropActivity a) =>
        new(a.Id, a.ActivityType, a.OccurredAtUtc, a.RecordedAtUtc,
            a.RecordedByUserId, a.Notes, a.FarmTaskId);

    private static PhotoDetails Map(ObservationPhoto p) =>
        new(p.Id, p.CropObservationId, p.ContentType, p.FileSizeBytes, p.UploadedAtUtc);

    private static ObservationDetails Map(CropObservation o, IReadOnlyList<PhotoDetails> photos) =>
        new(o.Id, o.HealthStatus, o.GrowthStage, o.ObservedAtUtc, o.RecordedAtUtc,
            o.RecordedByUserId, o.Notes, photos);

    private async Task<CropCycle> RequireCycle(Guid farmId, Guid cycleId, Guid actor,
        bool recording, CancellationToken ct)
    {
        // Worker recording is intentionally allowed on active crops,
        // unlike planning/status changes, which require Owner/Manager.
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct))
            throw Missing();

        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, recording, ct)
            ?? throw Missing();

        if (recording && !MonitoringRules.CanRecord(cycle.Status))
            throw Invalid("Monitoring records can only be added to an active crop cycle.");
        return cycle;
    }

    public async Task<MonitoringDetails> GetAsync(
        Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default)
    {
        var cycle = await RequireCycle(farmId, cycleId, actor, false, ct);
        var activityRows = await activities.ListForCycleAsync(cycleId, ct);
        var observationRows = await observations.ListForCycleAsync(cycleId, ct);

        var observationIds = observationRows.Select(o => o.Id).ToArray();
        var photoRows = await photos.ListForObservationsAsync(observationIds, ct);

        var mappedPhotos = photoRows.GroupBy(MapObservationId)
            .ToDictionary(g => g.Key, g => (IReadOnlyList<PhotoDetails>)g.Select(Map).ToList());
        return new MonitoringDetails(farmId, cycleId, MonitoringRules.CanRecord(cycle.Status),
            activityRows.Select(Map).ToList(),
            observationRows.Select(o => Map(o,
                mappedPhotos.GetValueOrDefault(o.Id) ?? [])).ToList());
    }

    private static Guid MapObservationId(ObservationPhoto photo) => photo.CropObservationId;

    public async Task<ActivityDetails> RecordActivityAsync(
        Guid farmId, Guid cycleId, Guid actor, RecordActivityInput input, CancellationToken ct = default)
    {
        // Keep the active-crop check, task completion and activity insert together.
        // Offline sync already provides its own transaction and must not nest another.
        var transaction = !unit.HasActiveTransaction
            ? await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct)
            : null;
        try
        {
        await RequireCycle(farmId, cycleId, actor, true, ct);
        if (!MonitoringRules.ValidActivity(input.ActivityType) || input.Notes?.Length > 1000 ||
            input.FarmTaskId == Guid.Empty)
            throw Invalid("Choose a valid activity, task and notes up to 1000 characters.");

        var now = DateTime.UtcNow;
        var level = await access.GetAccessLevelAsync(farmId, actor, ct);
        var mayManage = level is FarmAccessLevel.Owner or FarmAccessLevel.Manager;
        var taskType = TaskRules.ForActivity(input.ActivityType);
        FarmTask? linked = null;
        if (input.FarmTaskId is { } selectedId)
        {
            linked = await tasks.FindAsync(farmId, selectedId, ct);
            if (linked is null || taskType is null || linked.CropCycleId != cycleId ||
                linked.TaskType != taskType.Value ||
                !TaskRules.CanFulfillFromRecord(linked, actor, mayManage))
                throw Invalid("The selected task does not match this crop activity.");
            if (linked.Status != 0) throw Conflict();
        }
        else if (taskType is { } dueType)
        {
            var timezone = await tasks.GetFarmTimeZoneAsync(farmId, ct);
            if (timezone is not null)
            {
                DateOnly today;
                try { today = TaskRules.FarmDate(timezone, now); }
                catch (TimeZoneNotFoundException)
                {
                    today = DateOnly.FromDateTime(now);
                }
                linked = await tasks.FindDueAsync(farmId, cycleId, dueType, today, actor, mayManage, ct);
            }
        }

        var activity = new CropActivity
        {
            CropCycleId = cycleId, FarmTaskId = linked?.Id,
            ActivityType = input.ActivityType, OccurredAtUtc = now,
            RecordedByUserId = actor,
            Notes = string.IsNullOrWhiteSpace(input.Notes) ? null : input.Notes.Trim()
        };
        activities.Add(activity);
        if (linked is not null && TaskRules.Close(linked, 1, actor, now))
            await tasks.DismissRemindersAsync(linked.Id, now, ct);
        try { await unit.SaveChangesAsync(ct); }
        catch (DbUpdateConcurrencyException) { throw Conflict(); }
        if (transaction is not null)
            await transaction.CommitAsync(ct);
        return Map(activity);
        }
        finally
        {
            if (transaction is not null)
                await transaction.DisposeAsync();
        }
    }

    public async Task<ObservationDetails> RecordObservationAsync(
        Guid farmId, Guid cycleId, Guid actor, RecordObservationInput input, CancellationToken ct = default)
    {
        await RequireCycle(farmId, cycleId, actor, true, ct);
        if (!MonitoringRules.ValidHealth(input.HealthStatus) ||
            input.GrowthStage?.Trim().Length > 40 || input.Notes?.Length > 1000)
            throw Invalid("Choose valid health, growth stage and notes.");
        var observation = new CropObservation
        {
            CropCycleId = cycleId, HealthStatus = input.HealthStatus,
            GrowthStage = string.IsNullOrWhiteSpace(input.GrowthStage) ? null : input.GrowthStage.Trim(),
            ObservedAtUtc = DateTime.UtcNow,
            RecordedByUserId = actor,
            Notes = string.IsNullOrWhiteSpace(input.Notes) ? null : input.Notes.Trim()
        };
        observations.Add(observation);
        await unit.SaveChangesAsync(ct);
        return Map(observation, []);
    }

    public async Task<PhotoDetails> AddPhotoAsync(Guid farmId, Guid cycleId,
        Guid observationId, Guid actor, Stream stream, long length,
        string? claimedContentType, CancellationToken ct = default)
    {
        await RequireCycle(farmId, cycleId, actor, true, ct);
        if (await observations.GetByIdAsync(cycleId, observationId, ct) is null)
            throw Missing();
        if (length is <= 0 or > MonitoringRules.MaxPhotoBytes)
            throw Invalid("Photo must be 5 MB or smaller.");

        using var buffer = new MemoryStream();
        var block = new byte[81920];
        while (true)
        {
            var read = await stream.ReadAsync(block.AsMemory(), ct);
            if (read == 0) break;
            if (buffer.Length + read > MonitoringRules.MaxPhotoBytes || buffer.Length + read > length)
                throw Invalid("Invalid photo size.");
            await buffer.WriteAsync(block.AsMemory(0, read), ct);
        }
        if (buffer.Length != length)
            throw Invalid("Invalid photo size.");
        var bytes = buffer.ToArray();
        var ext = MonitoringRules.PhotoExtension(bytes, claimedContentType);
        if (ext is null)
            throw Invalid("Only genuine JPEG, PNG, and WebP photos are accepted.");

        var key = await photoStore.SaveAsync(bytes, ext, ct);
        var mediaType = ext switch
        {
            ".jpg" => "image/jpeg", ".png" => "image/png", _ => "image/webp"
        };
        var photo = new ObservationPhoto
        {
            CropObservationId = observationId, StorageKey = key,
            ContentType = mediaType, FileSizeBytes = bytes.LongLength,
            UploadedByUserId = actor
        };
        try
        {
            photos.Add(photo);
            await unit.SaveChangesAsync(ct);
        }
        catch
        {
            await photoStore.DeleteAsync(key);
            throw;
        }
        return Map(photo);
    }

    public async Task<(Stream Stream, string ContentType)> OpenPhotoAsync(
        Guid farmId, Guid cycleId, Guid photoId, Guid actor, CancellationToken ct = default)
    {
        await RequireCycle(farmId, cycleId, actor, false, ct);
        var photo = await photos.GetForCycleAsync(cycleId, photoId, ct) ?? throw Missing();
        try
        {
            return (await photoStore.OpenAsync(photo.StorageKey, ct), photo.ContentType);
        }
        catch (FileNotFoundException)
        {
            throw Missing();
        }
    }
}

