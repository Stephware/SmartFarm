namespace SmartFarm.Core.Entities;

// A manual cash ledger, independent from inventory and harvested quantities.
public sealed class FarmFinanceEntry
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public Guid? CropCycleId { get; set; }
    public Guid RecordedByUserId { get; set; }
    public DateOnly EntryDate { get; set; }
    public string Kind { get; set; } = string.Empty; // Expense or Income
    public string Category { get; set; } = string.Empty;
    public decimal AmountPhp { get; set; }
    public string? Notes { get; set; }
    public DateTime RecordedAtUtc { get; set; }
    public DateTime? VoidedAtUtc { get; set; }
    public Guid? VoidedByUserId { get; set; }
    public string? VoidReason { get; set; }
}
