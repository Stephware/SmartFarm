using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/members")]
public sealed class FarmMembersController(
    UserManager<SmartFarmUser> users, IFarmManagementService farms)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await farms.MembersAsync(farmId, ActorId, ct)));

    [AllowAnonymous, HttpPost("invitations")]
    public IActionResult Invite(Guid farmId) =>
        StatusCode(StatusCodes.Status410Gone, new { message = "Invitation links are retired. Use a farm code and owner approval." });

    [HttpPut("{memberId:guid}")]
    public Task<IActionResult> Role(Guid farmId, Guid memberId,
        [FromBody] UpdateMemberInput input, CancellationToken ct) => Result(async () =>
    {
        await farms.ChangeMemberRoleAsync(farmId, memberId, ActorId, input.Role, ct);
        return NoContent();
    });

    [HttpDelete("{memberId:guid}")]
    public Task<IActionResult> Remove(Guid farmId, Guid memberId, CancellationToken ct) => Result(async () =>
    {
        await farms.RemoveMemberAsync(farmId, memberId, ActorId, ct);
        return NoContent();
    });
}

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farm-invitations")]
public sealed class FarmInvitationsController : ControllerBase
{
    [AllowAnonymous, HttpPost("accept")]
    public IActionResult Accept() =>
        StatusCode(StatusCodes.Status410Gone, new { message = "Invitation links are retired. Use a farm code and owner approval." });
}
