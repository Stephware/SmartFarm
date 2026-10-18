using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/crop-cycles/{cycleId:guid}/harvests")]
public sealed class HarvestsApiController(
    UserManager<SmartFarmUser> users,
    IHarvestService harvests) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, Guid cycleId, CancellationToken ct) =>
        Result(async () => Ok(await harvests.GetAsync(farmId, cycleId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Record(Guid farmId, Guid cycleId,
        [FromBody] RecordHarvestInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var saved = await harvests.RecordAsync(farmId, cycleId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, saved);
        });

    [HttpPost("{harvestId:guid}/void")]
    public Task<IActionResult> Void(Guid farmId, Guid cycleId, Guid harvestId,
        [FromBody] VoidHarvestInput input, CancellationToken ct) =>
        Result(async () => Ok(await harvests.VoidAsync(
            farmId, cycleId, harvestId, ActorId, input, ct)));
}

