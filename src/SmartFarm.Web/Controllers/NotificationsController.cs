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

    [HttpGet]
    [ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
    public async Task<IActionResult> AniAlerts([FromServices] SmartFarm.Infrastructure.Services.Queries.NotificationAlertService alerts, CancellationToken ct)
        => Ok(await alerts.ListAsync(Actor, new SmartFarm.Core.Contracts.Query.PageRequest { Unread = true, PageSize = 100 }, ct));

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> AcknowledgeAni(Guid id,
        [FromServices] SmartFarm.Infrastructure.Services.Queries.NotificationAlertService alerts, CancellationToken ct)
    {
        try { await alerts.AcknowledgeAsync(Actor, id, ct); return NoContent(); }
        catch (SmartFarm.Core.Contracts.Farms.FarmOperationException) { return NotFound(); }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Read(Guid id, CancellationToken ct, string? returnView = null, int returnPage = 1)
    {
        await tasks.MarkNotificationReadAsync(Actor, id, ct);
        TempData["Message"] = "Notification marked as read.";
        return RedirectToAction(nameof(Index), new {
            view = returnView == "unread" ? "unread" : null,
            page = returnPage > 1 ? (int?)returnPage : null
        });
    }
}
