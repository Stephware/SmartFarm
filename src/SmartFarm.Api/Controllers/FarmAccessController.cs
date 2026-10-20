using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Security;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

// A protected permission check, not farm CRUD. Future farm endpoints must
// perform the same server-side, per-farm authorization before reading or writing.
[ApiController]
[Route("api/farms/{farmId:guid}/access")]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
public sealed class FarmAccessController(
    UserManager<SmartFarmUser> users,
    IFarmAccessService farmAccess) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get(Guid farmId, CancellationToken cancellationToken)
    {
        var rawUserId = users.GetUserId(User);
        if (!Guid.TryParse(rawUserId, out var userId)) return Unauthorized();

        var level = await farmAccess.GetAccessLevelAsync(farmId, userId, cancellationToken);
        if (!FarmAccessRules.Allows(level, FarmAction.View)) return NotFound();
        return Ok(new
        {
            farmId,
            role = level.ToString(),
            canManage = FarmAccessRules.Allows(level, FarmAction.Manage),
            canManageMembers = FarmAccessRules.Allows(level, FarmAction.ManageMembers)
        });
    }
}
