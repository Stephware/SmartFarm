using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts.Inventory;

public sealed class InventoryItemInput
{
    [Required, StringLength(120, MinimumLength = 2)]
    public string Name { get; set; } = string.Empty;

    [Range(1, 4)]
    public InventoryCategory Category { get; set; }

    [Required, StringLength(20)]
    public string Unit { get; set; } = string.Empty;

    [StringLength(500)]
    public string? Description { get; set; }

    [Range(typeof(decimal), "0", "999999999999999.999")]
    public decimal? ReorderLevel { get; set; }

    // Allowed only on creation, where it becomes an Opening transaction.
    [Range(typeof(decimal), "0", "999999999999999.999")]
    public decimal InitialQuantity { get; set; }

    public string? RowVersion { get; set; }
}

public sealed class InventoryMovementInput
{
    // Only Receipt=2, Consumption=3, Adjustment=4. Reversal is a separate endpoint.
    [Range(2, 4)]
    public StockTransactionType TransactionType { get; set; }

    public decimal QuantityChange { get; set; }

    public Guid? CropActivityId { get; set; }

    [StringLength(300)]
    public string? Reason { get; set; }

    [Required]
    public string RowVersion { get; set; } = string.Empty;
}

public sealed class InventoryReversalInput
{
    [Required]
    public string RowVersion { get; set; } = string.Empty;

    [Required, StringLength(300, MinimumLength = 3)]
    public string Reason { get; set; } = string.Empty;
}

public sealed record InventoryItemDetails(
    Guid Id, Guid FarmId, string Name, InventoryCategory Category,
    string Unit, string? Description, decimal QuantityOnHand,
    decimal? ReorderLevel, bool IsLowStock, DateTime? ArchivedAtUtc,
    string RowVersion);

public sealed record InventoryTransactionDetails(
    Guid Id, Guid InventoryItemId, Guid? CropActivityId,
    Guid? ReversesTransactionId, StockTransactionType TransactionType,
    decimal QuantityChange, DateTime OccurredAtUtc,
    DateTime RecordedAtUtc, Guid RecordedByUserId, string? Reason);

public sealed record InventoryWorkspace(
    IReadOnlyList<InventoryItemDetails> Items, int LowStockCount);

