using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Services.Weather;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class WeatherController(
    UserManager<SmartFarmUser> users, IFarmWeatherService weather,
    ICropManagementService crops, WeatherGuidanceService guidance) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try
        {
            var overview = await weather.GetAsync(farmId, actor, ct);
            // Read-only, permission-checked options; no crop data is sent to Gemini
            // until a farmer deliberately selects one and requests guidance.
            ViewData["ActiveCrops"] = (await crops.ListCyclesAsync(farmId, actor, ct))
                .Where(c => c.Status == CropCycleStatus.Active).ToList();
            return View(overview);
        }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        {
            return NotFound();
        }
    }

    [HttpGet, ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
    public async Task<IActionResult> Guidance(Guid farmId, Guid advisoryId, Guid? cropCycleId,
        CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try
        {
            ViewData["FarmId"] = farmId;
            return View(await guidance.GetAsync(farmId, advisoryId, actor, cropCycleId, ct));
        }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        {
            return NotFound();
        }
    }
}
