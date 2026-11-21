using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Contracts.Inventory;

namespace SmartFarm.Core.Contracts.Monitoring;

// A single field entry with stock consumption; not two independent writes.
// InventoryItemId identifies the stock, while the server supplies CropActivityId.
public sealed class RecordActivityWithStockInput
{
    [Required]
    public RecordActivityInput? Activity { get; set; }

    [Required]
    public Guid InventoryItemId { get; set; }

    [Range(typeof(decimal), "0.001", "999999999999999.999")]
    public decimal QuantityUsed { get; set; }

    [Required]
    public string RowVersion { get; set; } = string.Empty;

    [StringLength(300)]
    public string? Reason { get; set; }
}

public sealed record ActivityWithStockDetails(
    ActivityDetails Activity, InventoryItemDetails Inventory);
