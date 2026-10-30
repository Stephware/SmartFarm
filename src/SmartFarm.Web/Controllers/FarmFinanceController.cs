using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Finance;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class FarmFinanceController(
    UserManager<SmartFarmUser> users, IFarmFinanceService finance,
    IFarmManagementService farms, ICropManagementService crops) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        try
        {
            var farm = await farms.GetAsync(farmId, Actor, ct);
            if (farm is null) return NotFound();
            return View(new FarmFinancePage(farm,
                await finance.ListAsync(farmId, Actor, ct),
                await crops.ListCyclesAsync(farmId, Actor, ct)));
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound)
        { return NotFound(); }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Record(Guid farmId, FarmFinanceInput input,
        CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Check the entry date, category and amount.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try
        {
            await finance.RecordAsync(farmId, Actor, input, ct);
            TempData["Message"] = "Financial entry recorded.";
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Void(Guid farmId, Guid entryId,
        FarmFinanceVoidInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Provide a reason to void this entry.";
            return RedirectToAction(nameof(Index), new { farmId });
        }
        try
        {
            await finance.VoidAsync(farmId, entryId, Actor, input, ct);
            TempData["Message"] = "Entry voided; history retained.";
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Index), new { farmId });
    }
}
