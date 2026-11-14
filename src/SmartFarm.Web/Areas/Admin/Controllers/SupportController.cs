using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Support;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/support")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class SupportController(SmartFarmDbContext db,
    UserManager<SmartFarmUser> users) : Controller
{
    private async Task<SmartFarmUser?> ActiveAdmin() =>
        (await users.GetUserAsync(User)) is { DisabledAtUtc: null } u ? u : null;

    [HttpGet("")]
    public async Task<IActionResult> Index(string? search, string? status, string? priority,
        int page = 1, CancellationToken ct = default)
    {
        if (await ActiveAdmin() is null) return Forbid();
        var term = (search ?? "").Trim();
        if (term.Length > 100) term = term[..100];
        var selected = SupportRules.Statuses.Contains(status) ? status! : "All";
        var urgency = SupportRules.Priorities.Contains(priority) ? priority! : "All";
        page = Math.Clamp(page, 1, 100000);
        var query = db.SupportTickets.AsNoTracking().AsQueryable();
        if (term.Length > 0)
            query = query.Where(t => t.Title.Contains(term) || db.Users.Any(u =>
                u.Id == t.UserId && u.Email != null && u.Email.Contains(term)));
        if (selected != "All") query = query.Where(t => t.Status == selected);
        if (urgency != "All") query = query.Where(t => t.Priority == urgency);
        var count = await query.CountAsync(ct);
        var tickets = await query.OrderBy(t => t.Status == "Closed" || t.Status == "Resolved")
            .ThenByDescending(t => t.Priority == "High")
            .ThenByDescending(t => t.UpdatedAtUtc)
            .Skip((page - 1) * AdminSupportListPage.PageSize)
            .Take(AdminSupportListPage.PageSize).ToListAsync(ct);
        return View(new AdminSupportListPage(term, selected, urgency, page, count,
            await db.SupportTickets.CountAsync(t => t.Status == "Open", ct),
            await db.SupportTickets.CountAsync(t => t.Status == "InProgress", ct),
            await db.SupportTickets.CountAsync(t => t.Status == "Resolved", ct),
            await SupportPageBuilder.ToRowsAsync(db, tickets, ct)));
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        if (await ActiveAdmin() is null) return Forbid();
        var ticket = await db.SupportTickets.AsNoTracking().SingleOrDefaultAsync(t => t.Id == id, ct);
        if (ticket is null) return NotFound();
        return View(new SupportDetailsPage(
            (await SupportPageBuilder.ToRowsAsync(db, [ticket], ct))[0],
            ticket.Description,
            await SupportPageBuilder.MessagesAsync(db, id, includeInternal: true, ct)));
    }

    [HttpPost("{id:guid}/status"), ValidateAntiForgeryToken]
    public IActionResult ChangeStatus(Guid id) =>
        StatusCode(410, "Support history is read-only. Existing case status is unchanged.");

    [HttpPost("{id:guid}/reply"), ValidateAntiForgeryToken]
    public IActionResult Reply(Guid id) =>
        StatusCode(410, "Support history is read-only. No message was saved.");
}
