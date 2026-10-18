using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;

namespace SmartFarm.Web.ViewModels.Inventory;

public sealed record InventoryPage(FarmDetails Farm, InventoryWorkspace Workspace);

public sealed record InventoryItemPage(
    FarmDetails Farm, InventoryItemDetails Item,
    IReadOnlyList<InventoryTransactionDetails> Transactions,
    IReadOnlyList<InventoryActivityOption> ActivityOptions);

public sealed record InventoryEditPage(
    FarmDetails Farm, Guid ItemId, InventoryItemInput Input);

