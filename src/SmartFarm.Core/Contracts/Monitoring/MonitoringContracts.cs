using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts.Monitoring;

// CropActivity type: 1 Watering, 2 Fertilizing, 3 Pesticide,
// 4 Weeding, 5 Other. Inspections are CropObservations.
public sealed class RecordActivityInput
{
    [Range(1, 5)]
    public byte ActivityType { get; set; }

    [StringLength(1000)]
    public string? Notes { get; set; }

    // Optional: link a field entry to a specific scheduled watering/fertilizing task.
    public Guid? FarmTaskId { get; set; }
}

public sealed class RecordObservationInput
{
    [Range(0, 3)]
    public byte HealthStatus { get; set; }

    [StringLength(40)]
    public string? GrowthStage { get; set; }

    [StringLength(1000)]
    public string? Notes { get; set; }
}

public sealed record ActivityDetails(
    Guid Id, byte ActivityType, DateTime OccurredAtUtc,
    DateTime RecordedAtUtc, Guid RecordedByUserId, string? Notes,
    Guid? FarmTaskId = null)
{
    public string ActivityTypeName => FarmCodeLabels.Activity(ActivityType);
}

public sealed record ObservationDetails(
    Guid Id, byte HealthStatus, string? GrowthStage,
    DateTime ObservedAtUtc, DateTime RecordedAtUtc,
    Guid RecordedByUserId, string? Notes,
    IReadOnlyList<PhotoDetails> Photos)
{
    public string HealthStatusName => FarmCodeLabels.Health(HealthStatus);
}

public sealed record PhotoDetails(
    Guid Id, Guid ObservationId, string ContentType,
    long FileSizeBytes, DateTime UploadedAtUtc);

public sealed record MonitoringDetails(
    Guid FarmId, Guid CycleId, bool CanRecord,
    IReadOnlyList<ActivityDetails> Activities,
    IReadOnlyList<ObservationDetails> Observations);

