using System.Data;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Services.Inventory;

namespace SmartFarm.Infrastructure.Services.Monitoring;

/// <summary>
/// A fertilizer/pesticide field entry, matching task completion, and stock
/// consumption succeed or fail together. The sync endpoint provides stable
/// operation IDs; this service also respects its ambient SQL transaction.
/// </summary>
public sealed class FieldworkStockService(
    IMonitoringService monitoring,
    IInventoryService inventory,
    IUnitOfWork unit) : IFieldworkStockService
{
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);

    public async Task<ActivityWithStockDetails> RecordAsync(Guid farmId, Guid cycleId,
        Guid actor, RecordActivityWithStockInput input, CancellationToken ct = default)
    {
        if (farmId == Guid.Empty || cycleId == Guid.Empty || input is null ||
            input.Activity is null || input.InventoryItemId == Guid.Empty ||
            input.Activity.ActivityType is not (2 or 3) ||
            !StockRules.IsValidQuantity(input.QuantityUsed) ||
            input.Reason?.Trim().Length > 300)
            throw Invalid("Select a fertilizer/pesticide activity, valid supply and quantity.");

        var transaction = !unit.HasActiveTransaction
            ? await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct)
            : null;
        try
        {
            var stock = await inventory.GetAsync(farmId, input.InventoryItemId, actor, ct);
            if (stock is null || stock.ArchivedAtUtc is not null)
                throw Invalid("The selected supply is no longer available.");
            var requiredCategory = input.Activity.ActivityType == 2
                ? InventoryCategory.Fertilizers : InventoryCategory.Pesticides;
            if (stock.Category != requiredCategory)
                throw Invalid("The selected supply category does not match the field activity.");

            // Both operations use the same DbContext and transaction. If the
            // stock rowVersion or balance is stale, this activity (including
            // any automatically fulfilled task) is rolled back as well.
            var activity = await monitoring.RecordActivityAsync(
                farmId, cycleId, actor, input.Activity, ct);
            var updated = await inventory.PostAsync(farmId, input.InventoryItemId, actor,
                new InventoryMovementInput
                {
                    TransactionType = StockTransactionType.Consumption,
                    QuantityChange = -input.QuantityUsed,
                    CropActivityId = activity.Id,
                    Reason = input.Reason,
                    RowVersion = input.RowVersion
                }, ct);
            if (transaction is not null)
                await transaction.CommitAsync(ct);
            return new ActivityWithStockDetails(activity, updated);
        }
        finally
        {
            if (transaction is not null)
                await transaction.DisposeAsync();
        }
    }
}
