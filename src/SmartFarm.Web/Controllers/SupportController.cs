using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Security;
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
    public async Task<IActionResult> Index(CancellationToken ct)
    {
        var user = await ActiveUser();
        if (user is null) return Forbid();
        var tickets = await db.SupportTickets.AsNoTracking()
            .Where(t => t.UserId == user.Id)
            .OrderByDescending(t => t.UpdatedAtUtc).Take(100).ToListAsync(ct);
        return View(new SupportListPage(await SupportPageBuilder.ToRowsAsync(db, tickets, ct)));
    }

    [HttpGet("new")]
    public async Task<IActionResult> Create(CancellationToken ct)
    {
        var user = await ActiveUser();
        if (user is null) return Forbid();
        return View(new SupportCreatePage(new SupportCreateInput(), await FarmChoices(user.Id, ct)));
    }

    [HttpPost("new")]
    [ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public async Task<IActionResult> Create([Bind(Prefix = "Input")] SupportCreateInput input,
        CancellationToken ct)
    {
        var user = await ActiveUser();
        if (user is null) return Forbid();
        input.Title = input.Title?.Trim() ?? "";
        input.Description = input.Description?.Trim() ?? "";
        if (input.FarmId is Guid farmId && !await SupportRules.CanLinkFarm(db, user.Id, farmId, ct))
            ModelState.AddModelError("Input.FarmId", "Choose a farm you can access.");
        if (input.Title.Length is < 5 or > 140)
            ModelState.AddModelError("Input.Title", "Use a title between 5 and 140 characters.");
        if (input.Description.Length is < 15 or > 2000)
            ModelState.AddModelError("Input.Description", "Describe the issue in 15 to 2000 characters.");
        if (!ModelState.IsValid)
            return View(new SupportCreatePage(input, await FarmChoices(user.Id, ct)));

        var now = DateTime.UtcNow;
        var ticket = new SupportTicket
        {
            UserId = user.Id, FarmId = input.FarmId,
            Title = input.Title, Description = input.Description,
            Category = input.Category, Status = "Open", Priority = "Normal",
            CreatedAtUtc = now, UpdatedAtUtc = now
        };
        db.SupportTickets.Add(ticket);
        await db.SaveChangesAsync(ct);
        TempData["Message"] = "Support request submitted. You can follow its progress here.";
        return RedirectToAction(nameof(Details), new { id = ticket.Id });
    }

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

    [HttpPost("{id:guid}/reply")]
    [ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Enrollment)]
    public async Task<IActionResult> Reply(Guid id, [FromForm] string? body, CancellationToken ct)
    {
        var user = await ActiveUser();
        if (user is null) return Forbid();
        var ticket = await db.SupportTickets
            .SingleOrDefaultAsync(t => t.Id == id && t.UserId == user.Id, ct);
        if (ticket is null) return NotFound();
        if (ticket.Status == "Closed")
        {
            TempData["Error"] = "This request is closed. Create a new request if you still need help.";
            return RedirectToAction(nameof(Details), new { id });
        }
        if (!SupportRules.ValidMessage(body))
        {
            TempData["Error"] = "Reply must be between 2 and 1200 characters.";
            return RedirectToAction(nameof(Details), new { id });
        }

        var now = DateTime.UtcNow;
        await using var tx = await db.Database.BeginTransactionAsync(ct);
        ticket.UpdatedAtUtc = now;
        if (ticket.Status == "Resolved")
        {
            ticket.Status = "Open";
            ticket.ResolvedAtUtc = null;
        }
        db.SupportTicketMessages.Add(new SupportTicketMessage
        {
            TicketId = id, AuthorUserId = user.Id, Body = body!.Trim(),
            IsInternal = false, CreatedAtUtc = now
        });
        try
        {
            await db.SaveChangesAsync(ct);
            await tx.CommitAsync(ct);
            TempData["Message"] = "Reply added.";
        }
        catch (DbUpdateConcurrencyException)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "This request changed. Reload and try again.";
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    private async Task<IReadOnlyList<SupportFarmChoice>> FarmChoices(Guid actor, CancellationToken ct) =>
        await db.Farms.AsNoTracking()
            .Where(f => f.ArchivedAtUtc == null &&
                (f.OwnerUserId == actor || db.FarmMembers.Any(m =>
                    m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null)))
            .OrderBy(f => f.Name)
            .Select(f => new SupportFarmChoice(f.Id, f.Name)).ToListAsync(ct);
}
