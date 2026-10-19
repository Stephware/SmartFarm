using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Inventory;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Inventory;

public sealed class InventoryService(
    SmartFarmDbContext db,
    IFarmAccessService access) : IInventoryService
{
    private static FarmOperationException Missing() =>
        new(FarmError.NotFound, "Inventory resource not found.");
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);
    private static FarmOperationException Conflict(string message) =>
        new(FarmError.Conflict, message);

    private async Task Require(Guid farmId, Guid actor, FarmAction permission, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, permission, ct)) throw Missing();
    }

    private static byte[] Version(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) throw Invalid("A current rowVersion is required.");
        try
        {
            var bytes = Convert.FromBase64String(value);
            if (bytes.Length == 8) return bytes;
        }
        catch (FormatException) { }
        throw Invalid("Invalid rowVersion.");
    }

    private static string? Optional(string? text) =>
        string.IsNullOrWhiteSpace(text) ? null : text.Trim();

    private static void ValidateItem(InventoryItemInput input, bool creating)
    {
        if (input.Name?.Trim().Length is not (>= 2 and <= 120) ||
            input.Unit?.Trim().Length is not (>= 1 and <= 20) ||
            !Enum.IsDefined(input.Category) ||
            input.Description?.Length > 500 ||
            input.ReorderLevel is { } reorder && !StockRules.IsValidQuantity(reorder, allowZero: true) ||
            creating && !StockRules.IsValidQuantity(input.InitialQuantity, allowZero: true))
            throw Invalid("Invalid inventory name, category, unit, or stock settings.");

        if (!creating && input.InitialQuantity != 0)
            throw Invalid("Update stock through a transaction, not item details.");
    }

    private static InventoryItemDetails Map(InventoryItem item) => new(
        item.Id, item.FarmId, item.Name, (InventoryCategory)item.Category,
        item.Unit, item.Description, item.QuantityOnHand, item.ReorderLevel,
        item.ArchivedAtUtc is null && item.ReorderLevel is { } level &&
            item.QuantityOnHand <= level,
        item.ArchivedAtUtc,
        Convert.ToBase64String(item.RowVersion));

    private static InventoryTransactionDetails Map(InventoryTransaction item) => new(
        item.Id, item.InventoryItemId, item.CropActivityId,
        item.ReversesTransactionId, (StockTransactionType)item.TransactionType,
        item.QuantityChange, item.OccurredAtUtc, item.RecordedAtUtc,
        item.RecordedByUserId, item.Reason);

    private static async Task Save(CancellationToken ct, SmartFarmDbContext db)
    {
        try { await db.SaveChangesAsync(ct); }
        catch (DbUpdateConcurrencyException)
        {
            throw Conflict("Inventory changed. Reload and retry with the latest rowVersion.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number is 2601 or 2627)
        {
            throw Conflict("This inventory operation is already recorded.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number == 547)
        {
            throw Invalid("The inventory change violates an existing database rule.");
        }
    }

    private async Task<InventoryItem> ActiveItem(Guid farmId, Guid itemId, CancellationToken ct) =>
        await db.InventoryItems.SingleOrDefaultAsync(x =>
            x.Id == itemId && x.FarmId == farmId && x.ArchivedAtUtc == null, ct) ?? throw Missing();


    public async Task<IReadOnlyList<InventoryActivityOption>> ActivityOptionsAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var activities = await (
            from activity in db.CropActivities.AsNoTracking()
            join cycle in db.CropCycles.AsNoTracking() on activity.CropCycleId equals cycle.Id
            join plot in db.Plots.AsNoTracking() on cycle.PlotId equals plot.Id
            where plot.FarmId == farmId && plot.ArchivedAtUtc == null && cycle.Status == 1
            orderby activity.OccurredAtUtc descending
            select new { activity.Id, activity.ActivityType, activity.OccurredAtUtc, PlotName = plot.Name }
        ).Take(100).ToListAsync(ct);

        return activities.Select(a => new InventoryActivityOption(a.Id,
            a.PlotName + " · Activity " + a.ActivityType + " · " +
            a.OccurredAtUtc.ToString("yyyy-MM-dd HH:mm") + " UTC")).ToList();
    }

    public async Task<InventoryWorkspace> ListAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var items = await db.InventoryItems.AsNoTracking().Where(i =>
            i.FarmId == farmId && i.ArchivedAtUtc == null)
            .OrderBy(i => i.Name).ToListAsync(ct);
        var mapped = items.Select(Map).ToList();
        return new InventoryWorkspace(mapped, mapped.Count(x => x.IsLowStock));
    }

    public async Task<InventoryItemDetails?> GetAsync(
        Guid farmId, Guid itemId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var item = await db.InventoryItems.AsNoTracking().SingleOrDefaultAsync(x =>
            x.Id == itemId && x.FarmId == farmId, ct);
        return item is null ? null : Map(item);
    }

    public async Task<InventoryItemDetails> CreateAsync(
        Guid farmId, Guid actor, InventoryItemInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidateItem(input, creating: true);
        var item = new InventoryItem
        {
            FarmId = farmId,
            Name = input.Name.Trim(), Category = (byte)input.Category,
            Unit = input.Unit.Trim(), Description = Optional(input.Description),
            ReorderLevel = input.ReorderLevel, QuantityOnHand = input.InitialQuantity,
            MetadataVersion = Guid.NewGuid()
        };
        db.InventoryItems.Add(item);
        if (input.InitialQuantity > 0)
        {
            db.InventoryTransactions.Add(new InventoryTransaction
            {
                InventoryItemId = item.Id,
                RecordedByUserId = actor, OccurredAtUtc = DateTime.UtcNow,
                QuantityChange = input.InitialQuantity,
                TransactionType = (byte)StockTransactionType.Opening,
                Reason = "Initial stock"
            });
        }
        // EF Core SaveChanges is transactional for the item and opening entry.
        await Save(ct, db);
        return Map(item);
    }

    public async Task<InventoryItemDetails> UpdateAsync(
        Guid farmId, Guid itemId, Guid actor, InventoryItemInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidateItem(input, creating: false);
        var item = await ActiveItem(farmId, itemId, ct);
        if (!string.Equals(item.Unit, input.Unit.Trim(), StringComparison.OrdinalIgnoreCase) &&
            await db.InventoryTransactions.AnyAsync(t => t.InventoryItemId == itemId, ct))
            throw Conflict("Units cannot be changed once stock transactions exist.");
        db.Entry(item).Property(i => i.RowVersion).OriginalValue = Version(input.RowVersion);
        item.Name = input.Name.Trim();
        item.Category = (byte)input.Category;
        item.Unit = input.Unit.Trim();
        item.Description = Optional(input.Description);
        item.ReorderLevel = input.ReorderLevel;
        item.MetadataVersion = Guid.NewGuid();
        await Save(ct, db);
        return Map(item);
    }

    public async Task ArchiveAsync(
        Guid farmId, Guid itemId, Guid actor, string rowVersion, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var item = await ActiveItem(farmId, itemId, ct);
        if (item.QuantityOnHand != 0)
            throw Conflict("Reduce stock to zero before archiving an inventory item.");
        db.Entry(item).Property(i => i.RowVersion).OriginalValue = Version(rowVersion);
        item.ArchivedAtUtc = DateTime.UtcNow;
        item.MetadataVersion = Guid.NewGuid();
        await Save(ct, db);
    }

    public async Task<IReadOnlyList<InventoryTransactionDetails>> HistoryAsync(
        Guid farmId, Guid itemId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        if (!await db.InventoryItems.AnyAsync(i => i.Id == itemId && i.FarmId == farmId, ct))
            throw Missing();
        return (await db.InventoryTransactions.AsNoTracking()
            .Where(t => t.InventoryItemId == itemId)
            .OrderByDescending(t => t.OccurredAtUtc).ThenByDescending(t => t.Id)
            .ToListAsync(ct)).Select(Map).ToList();
    }

    public async Task<InventoryItemDetails> PostAsync(
        Guid farmId, Guid itemId, Guid actor, InventoryMovementInput input, CancellationToken ct = default)
    {
        // Field Workers can record consumption when linked to an activity in
        // their farm; receipts and adjustments require Owner/Manager.
        var type = input.TransactionType;
        await Require(farmId, actor, type == StockTransactionType.Consumption
            ? FarmAction.View : FarmAction.Manage, ct);
        if (!StockRules.CanPost(type, input.QuantityChange, input.CropActivityId) ||
            input.Reason?.Length > 300)
            throw Invalid("Invalid stock movement, quantity, activity, or reason.");

        var item = await ActiveItem(farmId, itemId, ct);
        if (type == StockTransactionType.Consumption)
        {
            var activityId = input.CropActivityId!.Value;
            var isOwnActivity = await db.CropActivities.AsNoTracking().AnyAsync(a =>
                a.Id == activityId && db.CropCycles.Any(c =>
                    c.Id == a.CropCycleId && c.Status == 1 &&
                    db.Plots.Any(p => p.Id == c.PlotId &&
                        p.FarmId == farmId && p.ArchivedAtUtc == null)), ct);
            if (!isOwnActivity) throw Invalid("Consumption must refer to an active-crop activity from this farm.");
        }
        else if (type == StockTransactionType.Adjustment &&
            string.IsNullOrWhiteSpace(input.Reason))
            throw Invalid("Stock adjustments require a reason.");

        if (!StockRules.TryBalance(item.QuantityOnHand, input.QuantityChange, out var next))
            throw Conflict("Insufficient stock or quantity exceeds SQL precision.");

        db.Entry(item).Property(i => i.RowVersion).OriginalValue = Version(input.RowVersion);
        item.QuantityOnHand = next;
        db.InventoryTransactions.Add(new InventoryTransaction
        {
            InventoryItemId = itemId, CropActivityId = input.CropActivityId,
            RecordedByUserId = actor, TransactionType = (byte)type,
            QuantityChange = input.QuantityChange, OccurredAtUtc = DateTime.UtcNow,
            Reason = Optional(input.Reason)
        });
        await Save(ct, db);
        return Map(item);
    }

    public async Task<InventoryItemDetails> ReverseAsync(
        Guid farmId, Guid itemId, Guid transactionId, Guid actor,
        InventoryReversalInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (input.Reason?.Trim().Length is not (>= 3 and <= 300))
            throw Invalid("Provide a reversal reason of 3–300 characters.");

        var item = await ActiveItem(farmId, itemId, ct);
        var original = await db.InventoryTransactions.AsNoTracking()
            .SingleOrDefaultAsync(t => t.Id == transactionId &&
                t.InventoryItemId == itemId, ct) ?? throw Missing();
        if (original.TransactionType == (byte)StockTransactionType.Reversal)
            throw Conflict("Cannot reverse a reversal.");
        if (await db.InventoryTransactions.AnyAsync(t => t.ReversesTransactionId == transactionId, ct))
            throw Conflict("This transaction has already been reversed.");
        if (!StockRules.TryBalance(item.QuantityOnHand, -original.QuantityChange, out var next))
            throw Conflict("Reversal would produce invalid or insufficient stock.");

        db.Entry(item).Property(i => i.RowVersion).OriginalValue = Version(input.RowVersion);
        item.QuantityOnHand = next;
        db.InventoryTransactions.Add(new InventoryTransaction
        {
            InventoryItemId = itemId, TransactionType = (byte)StockTransactionType.Reversal,
            ReversesTransactionId = original.Id, QuantityChange = -original.QuantityChange,
            RecordedByUserId = actor, OccurredAtUtc = DateTime.UtcNow,
            Reason = input.Reason.Trim()
        });
        await Save(ct, db);
        return Map(item);
    }
}


