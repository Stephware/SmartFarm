using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Services.Weather;
namespace SmartFarm.Api.Controllers;
[ApiController, Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/weather/advisories/{advisoryId:guid}/guidance")]
public sealed class WeatherGuidanceController(UserManager<SmartFarmUser> users, WeatherGuidanceService guidance) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Get(Guid farmId, Guid advisoryId, [FromQuery] Guid? cropCycleId, CancellationToken ct) =>
        Result(async () => Ok(await guidance.GetAsync(farmId, advisoryId, ActorId, cropCycleId, ct)));
}
