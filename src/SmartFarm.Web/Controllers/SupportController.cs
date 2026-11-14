using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Support;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
[Route("support")]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class SupportController(SmartFarmDbContext db,
    UserManager<SmartFarmUser> users) : Controller
{
    private async Task<SmartFarmUser?> ActiveUser() =>
        (await users.GetUserAsync(User)) is { DisabledAtUtc: null } u ? u : null;

    [HttpGet("")]
    public IActionResult Index() => RedirectToAction("Help", "Home");

    [HttpGet("history")]
    public async Task<IActionResult> History(CancellationToken ct)
    {
        var user = await ActiveUser();
        if (user is null) return Forbid();
        var tickets = await db.SupportTickets.AsNoTracking()
            .Where(t => t.UserId == user.Id)
            .OrderByDescending(t => t.UpdatedAtUtc).Take(100).ToListAsync(ct);
        return View("Index", new SupportListPage(await SupportPageBuilder.ToRowsAsync(db, tickets, ct)));
    }

    [HttpGet("new")]
    public IActionResult Create() => StatusCode(410, "New support requests are retired. Open Help for next steps.");

    [HttpPost("new"), ValidateAntiForgeryToken]
    public IActionResult Create([FromForm] string? body, CancellationToken ct) =>
        StatusCode(410, "New support requests are retired. No request was saved.");

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        var user = await ActiveUser();
        if (user is null) return Forbid();
        var ticket = await db.SupportTickets.AsNoTracking()
            .SingleOrDefaultAsync(t => t.Id == id && t.UserId == user.Id, ct);
        if (ticket is null) return NotFound();
        return View(new SupportDetailsPage(
            (await SupportPageBuilder.ToRowsAsync(db, [ticket], ct))[0],
            ticket.Description,
            await SupportPageBuilder.MessagesAsync(db, id, includeInternal: false, ct)));
    }

    [HttpPost("{id:guid}/reply"), ValidateAntiForgeryToken]
    public IActionResult Reply(Guid id) => StatusCode(410, "Support history is read-only. Open Help for next steps.");
}
