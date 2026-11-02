using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/weather")]
public sealed class FarmWeatherController(
    UserManager<SmartFarmUser> users, IFarmWeatherService weather) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Get(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await weather.GetAsync(farmId, ActorId, ct)));
}
