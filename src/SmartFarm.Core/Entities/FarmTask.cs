namespace SmartFarm.Core.Entities;

public sealed class FarmTask
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public Guid? CropCycleId { get; set; }
    public Guid? AssignedToUserId { get; set; }
    public byte TaskType { get; set; }
    public DateOnly DueDate { get; set; }
    public byte Status { get; set; }
    public string? Description { get; set; }
    public string? DedupeKey { get; set; }
    public DateTime CreatedAtUtc { get; set; }
    public DateTime? ClosedAtUtc { get; set; }
    public Guid? ClosedByUserId { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
