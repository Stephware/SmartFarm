using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class NotificationsController(
    UserManager<SmartFarmUser> users,
    IFarmTaskService tasks) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(CancellationToken ct) =>
        View(await tasks.NotificationsAsync(Actor, ct));

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Read(Guid id, CancellationToken ct, string? returnView = null)
    {
        await tasks.MarkNotificationReadAsync(Actor, id, ct);
        TempData["Message"] = "Notification marked as read.";
        return returnView == "unread"
            ? RedirectToAction(nameof(Index), new { view = "unread" })
            : RedirectToAction(nameof(Index));
    }
}
