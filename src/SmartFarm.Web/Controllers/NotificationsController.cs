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
    public async Task<IActionResult> AniAlerts([FromServices] SmartFarm.Infrastructure.Services.Queries.NotificationAlertService alerts,
        [FromServices] SmartFarm.Core.Interfaces.Farms.IFarmManagementService farms, CancellationToken ct, int page = 1, bool unread = true)
    {
        if (page < 1 || page > 100000) return BadRequest();
        var records = await alerts.ListAsync(Actor, new SmartFarm.Core.Contracts.Query.PageRequest { Unread = unread ? true : null, Page = page, PageSize = 100 }, ct);
        var visibleFarms = await farms.ListAsync(Actor, ct);
        return Ok(new { records.Items, records.Page, records.PageSize, records.TotalCount, records.HasMore,
            FarmTimeZones = visibleFarms.ToDictionary(f => f.Id, f => f.TimeZoneId) });
    }

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
        TempData["Message"] = SmartFarm.Web.Presentation.UiMessages.Text("Notification marked as read.");
        return RedirectToAction(nameof(Index), new {
            view = returnView == "unread" ? "unread" : null,
            page = returnPage > 1 ? (int?)returnPage : null
        });
    }
}
