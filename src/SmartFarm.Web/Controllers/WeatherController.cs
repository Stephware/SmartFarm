using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.Clients;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class WeatherController(
    UserManager<SmartFarmUser> users, SmartFarmApiClient api) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try
        {
            var overview = await api.ReadAsync<WeatherOverview>(
                $"api/farms/{farmId}/weather", actor, ct);
            return View(overview);
        }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        {
            return NotFound();
        }
    }
}
