using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class PlotLayoutController(UserManager<SmartFarmUser> users,
    IFarmManagementService farms, ICropManagementService crops) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try
        {
            var farm = await farms.GetAsync(farmId, actor, ct);
            if (farm is null) return NotFound();
            var plots = await farms.ListPlotsAsync(farmId, actor, ct);
            var cycles = await crops.ListCyclesAsync(farmId, actor, ct);
            return View(new SmartFarm.Web.ViewModels.Farms.PlotLayoutPage(farm, plots, cycles));
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound)
        { return NotFound(); }
    }
}
