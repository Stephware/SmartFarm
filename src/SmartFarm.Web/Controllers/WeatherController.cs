using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class WeatherController(
    UserManager<SmartFarmUser> users, IFarmWeatherService weather) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try { return View(await weather.GetAsync(farmId, actor, ct)); }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        {
            return NotFound();
        }
    }
}
