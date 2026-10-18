using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}")]
public sealed class CropCyclesController(
    UserManager<SmartFarmUser> users,
    ICropManagementService crops) : FarmApiControllerBase(users)
{
    [HttpGet("crop-cycles")]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await crops.ListCyclesAsync(farmId, ActorId, ct)));

    [HttpGet("crop-cycles/{cycleId:guid}")]
    public Task<IActionResult> Get(Guid farmId, Guid cycleId, CancellationToken ct) =>
        Result(async () =>
            await crops.GetCycleAsync(farmId, cycleId, ActorId, ct) is { } cycle
                ? Ok(cycle) : NotFound());

    [HttpPost("plots/{plotId:guid}/crop-cycles")]
    public Task<IActionResult> Plan(Guid farmId, Guid plotId, [FromBody] CropCyclePlanInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var created = await crops.PlanAsync(farmId, plotId, ActorId, input, ct);
            return CreatedAtAction(nameof(Get), new { farmId, cycleId = created.Id }, created);
        });

    [HttpPut("crop-cycles/{cycleId:guid}")]
    public Task<IActionResult> UpdatePlan(Guid farmId, Guid cycleId,
        [FromBody] CropCyclePlanInput input, CancellationToken ct) =>
        Result(async () => Ok(await crops.UpdatePlanAsync(farmId, cycleId, ActorId, input, ct)));

    [HttpPost("crop-cycles/{cycleId:guid}/transitions")]
    public Task<IActionResult> Transition(Guid farmId, Guid cycleId,
        [FromBody] CropCycleTransitionInput input, CancellationToken ct) =>
        Result(async () => Ok(await crops.TransitionAsync(farmId, cycleId, ActorId, input, ct)));
}

