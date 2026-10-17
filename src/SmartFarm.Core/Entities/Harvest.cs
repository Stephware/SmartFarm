namespace SmartFarm.Core.Entities;

public sealed class Harvest
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CropCycleId { get; set; }
    public DateTime HarvestedAtUtc { get; set; }
    public decimal Quantity { get; set; }
    public string Unit { get; set; } = string.Empty;
    public Guid RecordedByUserId { get; set; }
    public DateTime RecordedAtUtc { get; set; }
    public string? Notes { get; set; }
    public DateTime? VoidedAtUtc { get; set; }
    public Guid? VoidedByUserId { get; set; }
    public string? VoidReason { get; set; }
}
