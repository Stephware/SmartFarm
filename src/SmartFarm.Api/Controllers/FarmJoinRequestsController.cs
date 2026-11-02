using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Security;

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
    [EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public Task<IActionResult> Submit([FromBody] FarmJoinRequestInput input, CancellationToken ct) =>
        Result(async () => Ok(await enrollment.RequestAsync(ActorId, input.FarmCode, ct)));

    [HttpGet("/api/farms/{farmId:guid}/join-requests")]
    public Task<IActionResult> Overview(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await enrollment.OwnerOverviewAsync(farmId, ActorId, ct)));

    [HttpPost("/api/farms/{farmId:guid}/join-requests/{requestId:guid}/decision")]
    [EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public Task<IActionResult> Decision(Guid farmId, Guid requestId, [FromBody] FarmJoinDecisionInput input, CancellationToken ct) =>
        Result(async () =>
        {
            await enrollment.DecideAsync(farmId, requestId, ActorId, input.Approve!.Value, ct);
            return NoContent();
        });

    [HttpPost("/api/farms/{farmId:guid}/join-requests/{requestId:guid}/cancel")]
    [EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public Task<IActionResult> Cancel(Guid farmId, Guid requestId, CancellationToken ct) =>
        Result(async () =>
        {
            await enrollment.CancelAsync(farmId, requestId, ActorId, ct);
            return NoContent();
        });

    [HttpPost("/api/farms/{farmId:guid}/join-code/regenerate")]
    [EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public Task<IActionResult> RegenerateCode(Guid farmId, [FromBody] FarmCodeRegenerationInput input, CancellationToken ct) =>
        Result(async () => Ok(new
        {
            joinCode = await enrollment.RegenerateCodeAsync(farmId, ActorId, input.CurrentCode, ct)
        }));
}
