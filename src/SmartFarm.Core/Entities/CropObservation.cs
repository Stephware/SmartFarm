namespace SmartFarm.Core.Entities;

public sealed class CropObservation
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CropCycleId { get; set; }
    public byte HealthStatus { get; set; }
    public string? GrowthStage { get; set; }
    public DateTime ObservedAtUtc { get; set; }
    public DateTime RecordedAtUtc { get; set; }
    public Guid RecordedByUserId { get; set; }
    public string? Notes { get; set; }
    // 0 None, 1 Pest, 2 Disease, 3 Nutrient, 4 Environmental, 5 Other.
    public byte IssueCategory { get; set; }
    // 0 None, 1 Low, 2 Moderate, 3 High.
    public byte IssueSeverity { get; set; }
    // 0 None, 1 Open, 2 Resolved. Resolution is confirmed by a manager.
    public byte FollowUpStatus { get; set; }
    public Guid? FollowUpTaskId { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
