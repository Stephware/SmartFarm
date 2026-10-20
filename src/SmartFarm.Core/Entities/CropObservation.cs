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
}
