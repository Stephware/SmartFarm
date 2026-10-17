namespace SmartFarm.Core.Entities;

public sealed class InventoryTransaction
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid InventoryItemId { get; set; }
    public Guid? CropActivityId { get; set; }
    public Guid? ReversesTransactionId { get; set; }
    public byte TransactionType { get; set; }
    public decimal QuantityChange { get; set; }
    public DateTime OccurredAtUtc { get; set; }
    public DateTime RecordedAtUtc { get; set; }
    public Guid RecordedByUserId { get; set; }
    public string? Reason { get; set; }
}
