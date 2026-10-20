namespace SmartFarm.Core.Entities;

public sealed class CropActivity
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CropCycleId { get; set; }
    public Guid? FarmTaskId { get; set; }
    public byte ActivityType { get; set; }
    public DateTime OccurredAtUtc { get; set; }
    public DateTime RecordedAtUtc { get; set; }
    public Guid RecordedByUserId { get; set; }
    public string? Notes { get; set; }
}
