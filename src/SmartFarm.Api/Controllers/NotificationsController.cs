using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/notifications")]
public sealed class NotificationsController(
    UserManager<SmartFarmUser> users,
    IFarmTaskService tasks) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(CancellationToken ct) =>
        Result(async () => Ok(await tasks.NotificationsAsync(ActorId, ct)));

    [HttpPut("{notificationId:guid}/read")]
    public Task<IActionResult> Read(Guid notificationId, CancellationToken ct) =>
        Result(async () =>
        {
            await tasks.MarkNotificationReadAsync(ActorId, notificationId, ct);
            return NoContent();
        });
}
