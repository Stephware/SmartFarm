using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Services.Admin;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Support;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/support")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class SupportController(SmartFarmDbContext db,
    UserManager<SmartFarmUser> users, AdminAuditWriter audit) : Controller
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

    [HttpPost("{id:guid}/status")]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ChangeStatus(Guid id,
        [FromForm] string? status, [FromForm] string? priority,
        [FromForm] string? rowVersion, [FromForm] string? note,
        CancellationToken ct)
    {
        var actor = await ActiveAdmin();
        if (actor is null) return Forbid();
        var ticket = await db.SupportTickets.SingleOrDefaultAsync(t => t.Id == id, ct);
        if (ticket is null) return NotFound();
        if (!SupportRules.Statuses.Contains(status) || !SupportRules.Priorities.Contains(priority) ||
            !SupportRules.ValidMessage(note) || note!.Trim().Length < 8)
        {
            TempData["Error"] = "Choose a valid status and priority, and add a reason (8–1200 characters).";
            return RedirectToAction(nameof(Details), new { id });
        }
        byte[] version;
        try { version = Convert.FromBase64String(rowVersion ?? ""); }
        catch (FormatException) { version = []; }
        if (version.Length != 8)
        {
            TempData["Error"] = "Case details are stale. Reload the page.";
            return RedirectToAction(nameof(Details), new { id });
        }
        if (ticket.Status == status && ticket.Priority == priority)
        {
            TempData["Error"] = "No changes to save.";
            return RedirectToAction(nameof(Details), new { id });
        }
        await using var tx = await db.Database.BeginTransactionAsync(ct);
        db.Entry(ticket).Property(t => t.RowVersion).OriginalValue = version;
        var oldStatus = ticket.Status;
        ticket.Status = status!;
        ticket.Priority = priority!;
        ticket.UpdatedAtUtc = DateTime.UtcNow;
        ticket.ResolvedAtUtc = status is "Resolved" or "Closed"
            ? ticket.ResolvedAtUtc ?? ticket.UpdatedAtUtc : null;
        db.SupportTicketMessages.Add(new SupportTicketMessage
        {
            TicketId = id, AuthorUserId = actor.Id,
            Body = note!.Trim(), IsInternal = false, CreatedAtUtc = ticket.UpdatedAtUtc
        });
        try
        {
            await audit.RecordAsync(actor.Id, "web", "support-status-changed", "support-ticket", id,
                $"{oldStatus} to {status}; priority: {priority}", ct);
            await tx.CommitAsync(ct);
            TempData["Message"] = "Case updated. The requester can see your reason.";
        }
        catch (DbUpdateConcurrencyException)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "Someone else updated this case. Reload before saving.";
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost("{id:guid}/reply")]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Reply(Guid id, [FromForm] string? body,
        [FromForm] bool isInternal, CancellationToken ct)
    {
        var actor = await ActiveAdmin();
        if (actor is null) return Forbid();
        var ticket = await db.SupportTickets.SingleOrDefaultAsync(t => t.Id == id, ct);
        if (ticket is null) return NotFound();
        if (!SupportRules.ValidMessage(body))
        {
            TempData["Error"] = "Reply must be between 2 and 1200 characters.";
            return RedirectToAction(nameof(Details), new { id });
        }
        await using var tx = await db.Database.BeginTransactionAsync(ct);
        ticket.UpdatedAtUtc = DateTime.UtcNow;
        db.SupportTicketMessages.Add(new SupportTicketMessage
        {
            TicketId = id, AuthorUserId = actor.Id,
            Body = body!.Trim(), IsInternal = isInternal, CreatedAtUtc = ticket.UpdatedAtUtc
        });
        try
        {
            await audit.RecordAsync(actor.Id, "web",
                isInternal ? "support-internal-note" : "support-replied",
                "support-ticket", id, null, ct);
            await tx.CommitAsync(ct);
            TempData["Message"] = isInternal ? "Private note saved." : "Reply sent to requester.";
        }
        catch (DbUpdateConcurrencyException)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "Someone else updated this case. Reload before replying.";
        }
        return RedirectToAction(nameof(Details), new { id });
    }
}
