namespace SmartFarm.Core.Entities;

public sealed class InventoryItem
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public string Name { get; set; } = string.Empty;
    public byte Category { get; set; }
    public string Unit { get; set; } = string.Empty;
    public string? Description { get; set; }
    public decimal QuantityOnHand { get; set; }
    public decimal? ReorderLevel { get; set; }
    public DateTime CreatedAtUtc { get; set; }
    public DateTime? ArchivedAtUtc { get; set; }
    public Guid MetadataVersion { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
