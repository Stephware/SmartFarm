using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;
using SmartFarm.Core.Contracts;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts.Monitoring;

// CropActivity type: 1 Watering, 2 Fertilizing, 3 Pesticide,
// 4 Weeding, 5 Other. Inspections are CropObservations.
public sealed class RecordActivityInput
{
    [Range(1, 5)]
    [JsonConverter(typeof(ActivityTypeJsonConverter))]
    public byte ActivityType { get; set; }

    [StringLength(1000)]
    public string? Notes { get; set; }

    // Optional: link a field entry to a specific scheduled watering/fertilizing task.
    public Guid? FarmTaskId { get; set; }
}

public sealed class RecordObservationInput
{
    [Range(0, 3)]
    [JsonConverter(typeof(HealthStatusJsonConverter))]
    public byte HealthStatus { get; set; }

    [StringLength(40)]
    public string? GrowthStage { get; set; }

    [StringLength(1000)]
    public string? Notes { get; set; }

    [Range(0, 5)]
    public byte IssueCategory { get; set; }

    [Range(0, 3)]
    public byte IssueSeverity { get; set; }
}

public sealed class UpdateObservationFollowUpInput
{
    [Range(1, 2)]
    public byte FollowUpStatus { get; set; }

    public Guid? FollowUpTaskId { get; set; }

    [Required]
    public string RowVersion { get; set; } = string.Empty;
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
    IReadOnlyList<PhotoDetails> Photos,
    byte IssueCategory = 0, byte IssueSeverity = 0,
    byte FollowUpStatus = 0, Guid? FollowUpTaskId = null,
    string RowVersion = "")
{
    public string HealthStatusName => FarmCodeLabels.Health(HealthStatus);
    public string IssueCategoryName => IssueCategory switch
    {
        1 => "Pest", 2 => "Disease", 3 => "Nutrient deficiency",
        4 => "Environmental stress", 5 => "Other", _ => "None"
    };
    public string IssueSeverityName => IssueSeverity switch
    {
        1 => "Low", 2 => "Moderate", 3 => "High", _ => "None"
    };
}

public sealed record PhotoDetails(
    Guid Id, Guid ObservationId, string ContentType,
    long FileSizeBytes, DateTime UploadedAtUtc);

public sealed record MonitoringDetails(
    Guid FarmId, Guid CycleId, bool CanRecord,
    IReadOnlyList<ActivityDetails> Activities,
    IReadOnlyList<ObservationDetails> Observations);

