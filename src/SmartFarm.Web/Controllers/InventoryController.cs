using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Inventory;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class InventoryController(
    UserManager<SmartFarmUser> users,
    IFarmManagementService farms,
    IInventoryService inventory) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        var farm = await farms.GetAsync(farmId, Actor, ct);
        if (farm is null) return NotFound();
        return View(new InventoryPage(farm, await inventory.ListAsync(farmId, Actor, ct)));
    }

    [HttpGet]
    public async Task<IActionResult> Details(Guid farmId, Guid itemId, CancellationToken ct)
    {
        var farm = await farms.GetAsync(farmId, Actor, ct);
        if (farm is null) return NotFound();
        var item = await inventory.GetAsync(farmId, itemId, Actor, ct);
        if (item is null) return NotFound();
        return View(new InventoryItemPage(farm, item,
            await inventory.HistoryAsync(farmId, itemId, Actor, ct),
            await inventory.ActivityOptionsAsync(farmId, Actor, ct)));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(Guid farmId, InventoryItemInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Enter a valid inventory item, unit and opening stock.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try { await inventory.CreateAsync(farmId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpGet]
    public async Task<IActionResult> Edit(Guid farmId, Guid itemId, CancellationToken ct)
    {
        var farm = await farms.GetAsync(farmId, Actor, ct);
        if (farm?.Role is not ("Owner" or "Manager")) return NotFound();
        var item = await inventory.GetAsync(farmId, itemId, Actor, ct);
        if (item is null || item.ArchivedAtUtc is not null) return NotFound();
        return View(new InventoryEditPage(farm, itemId, new InventoryItemInput
        {
            Name = item.Name, Category = item.Category, Unit = item.Unit,
            Description = item.Description, ReorderLevel = item.ReorderLevel,
            RowVersion = item.RowVersion
        }));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(Guid farmId, Guid itemId,
        [Bind(Prefix = "Input")] InventoryItemInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Check item name, category, unit and reorder level.";
            return RedirectToAction(nameof(Details), new { farmId, itemId });
        }
        try { await inventory.UpdateAsync(farmId, itemId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { farmId, itemId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Archive(Guid farmId, Guid itemId,
        string rowVersion, CancellationToken ct)
    {
        try { await inventory.ArchiveAsync(farmId, itemId, Actor, rowVersion, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
            return RedirectToAction(nameof(Details), new { farmId, itemId });
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Post(Guid farmId, Guid itemId,
        InventoryMovementInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Enter a valid quantity, type and row version.";
            return RedirectToAction(nameof(Details), new { farmId, itemId });
        }
        try { await inventory.PostAsync(farmId, itemId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { farmId, itemId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Reverse(Guid farmId, Guid itemId, Guid transactionId,
        InventoryReversalInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Provide a reversal reason and current item version.";
            return RedirectToAction(nameof(Details), new { farmId, itemId });
        }
        try { await inventory.ReverseAsync(farmId, itemId, transactionId, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { farmId, itemId });
    }
}

