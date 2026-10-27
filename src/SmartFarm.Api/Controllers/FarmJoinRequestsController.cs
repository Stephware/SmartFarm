using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farm-join-requests")]
public sealed class FarmJoinRequestsController(UserManager<SmartFarmUser> users, IFarmEnrollmentService enrollment)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Mine(CancellationToken ct) =>
        Result(async () => Ok(await enrollment.MyRequestsAsync(ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Submit([FromBody] FarmJoinRequestInput input, CancellationToken ct) =>
        Result(async () => Ok(await enrollment.RequestAsync(ActorId, input.FarmCode, ct)));

    [HttpGet("/api/farms/{farmId:guid}/join-requests")]
    public Task<IActionResult> Overview(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await enrollment.OwnerOverviewAsync(farmId, ActorId, ct)));

    [HttpPost("/api/farms/{farmId:guid}/join-requests/{requestId:guid}/decision")]
    public Task<IActionResult> Decision(Guid farmId, Guid requestId, [FromBody] FarmJoinDecisionInput input, CancellationToken ct) =>
        Result(async () =>
        {
            await enrollment.DecideAsync(farmId, requestId, ActorId, input.Approve!.Value, ct);
            return NoContent();
        });
}
