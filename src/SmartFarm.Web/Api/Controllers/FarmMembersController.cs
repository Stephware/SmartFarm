using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Api.Controllers;

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

    [HttpPost("invitations")]
    public Task<IActionResult> Invite(Guid farmId, [FromBody] InviteMemberInput input, CancellationToken ct) =>
        Result(async () => Ok(await farms.CreateInvitationAsync(farmId, ActorId, input, ct)));

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
public sealed class FarmInvitationsController(
    UserManager<SmartFarmUser> users, IFarmManagementService farms)
    : FarmApiControllerBase(users)
{
    [HttpPost("accept")]
    public Task<IActionResult> Accept([FromBody] InvitationAcceptance input, CancellationToken ct) =>
        Result(async () => Ok(new { farmId = await farms.AcceptInvitationAsync(ActorId, input.Token, ct) }));
}

