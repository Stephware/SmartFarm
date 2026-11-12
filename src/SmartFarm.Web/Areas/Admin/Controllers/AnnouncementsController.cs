using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Services.Admin;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Announcements;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/announcements")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class AnnouncementsController(
    SmartFarmDbContext db, UserManager<SmartFarmUser> users,
    AdminAuditWriter audit) : Controller
{
    private async Task<SmartFarmUser?> ActiveAdmin() =>
        (await users.GetUserAsync(User)) is { DisabledAtUtc: null } u ? u : null;

    [HttpGet("")]
    public async Task<IActionResult> Index(string? status, int page = 1, CancellationToken ct = default)
    {
        if (await ActiveAdmin() is null) return Forbid();
        var selected = status is "Draft" or "Published" or "Archived" ? status : "All";
        page = Math.Clamp(page, 1, 100000);
        var query = db.PlatformAnnouncements.AsNoTracking().AsQueryable();
        if (selected != "All") query = query.Where(x => x.State == selected);
        var total = await query.CountAsync(ct);
        var rows = await query.OrderByDescending(x => x.UpdatedAtUtc).ThenBy(x => x.Id)
            .Skip((page - 1) * AdminAnnouncementIndexPage.PageSize)
            .Take(AdminAnnouncementIndexPage.PageSize).ToListAsync(ct);
        return View(new AdminAnnouncementIndexPage(selected, page, total,
            await db.PlatformAnnouncements.CountAsync(x => x.State == "Draft", ct),
            await db.PlatformAnnouncements.CountAsync(x => x.State == "Published" &&
                x.PublishedAtUtc <= DateTime.UtcNow &&
                (x.ExpiresAtUtc == null || x.ExpiresAtUtc > DateTime.UtcNow), ct),
            rows.Select(AnnouncementMapper.ToRow).ToArray()));
    }

    [HttpGet("new")]
    public IActionResult Create() => View("Editor",
        new AnnouncementEditorPage(null, new AnnouncementInput()));

    [HttpPost("new"), ValidateAntiForgeryToken]
    [EnableRateLimiting(EnrollmentRateLimiting.AdminActions)]
    public async Task<IActionResult> Create([Bind(Prefix = "Input")] AnnouncementInput input,
        CancellationToken ct)
    {
        var actor = await ActiveAdmin();
        if (actor is null) return Forbid();
        Normalize(input);
        ValidateDraft(input);
        if (!ModelState.IsValid)
            return View("Editor", new AnnouncementEditorPage(null, input));
        var now = DateTime.UtcNow;
        var item = new PlatformAnnouncement
        {
            CreatedByUserId = actor.Id, Title = input.Title, Body = input.Body,
            Category = input.Category, State = "Draft",
            CreatedAtUtc = now, UpdatedAtUtc = now, ExpiresAtUtc = input.ExpiresAtUtc
        };
        await using var tx = await db.Database.BeginTransactionAsync(ct);
        db.PlatformAnnouncements.Add(item);
        await audit.RecordAsync(actor.Id, "web", "announcement-created",
            "announcement", item.Id, null, ct);
        await tx.CommitAsync(ct);
        TempData["Message"] = "Draft saved. Review it before publishing.";
        return RedirectToAction(nameof(Details), new { id = item.Id });
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        if (await ActiveAdmin() is null) return Forbid();
        var item = await db.PlatformAnnouncements.AsNoTracking()
            .SingleOrDefaultAsync(x => x.Id == id, ct);
        return item is null ? NotFound() : View(AnnouncementMapper.ToRow(item));
    }

    [HttpGet("{id:guid}/edit")]
    public async Task<IActionResult> Edit(Guid id, CancellationToken ct)
    {
        if (await ActiveAdmin() is null) return Forbid();
        var item = await db.PlatformAnnouncements.AsNoTracking()
            .SingleOrDefaultAsync(x => x.Id == id, ct);
        if (item is null) return NotFound();
        if (item.State != "Draft") return RedirectToAction(nameof(Details), new { id });
        return View("Editor", new AnnouncementEditorPage(id, AnnouncementMapper.ToInput(item)));
    }

    [HttpPost("{id:guid}/edit"), ValidateAntiForgeryToken]
    [EnableRateLimiting(EnrollmentRateLimiting.AdminActions)]
    public async Task<IActionResult> Edit(Guid id,
        [Bind(Prefix = "Input")] AnnouncementInput input, CancellationToken ct)
    {
        var actor = await ActiveAdmin();
        if (actor is null) return Forbid();
        var item = await db.PlatformAnnouncements.SingleOrDefaultAsync(x => x.Id == id, ct);
        if (item is null) return NotFound();
        if (item.State != "Draft")
        {
            TempData["Error"] = "Only drafts can be edited.";
            return RedirectToAction(nameof(Details), new { id });
        }
        Normalize(input);
        ValidateDraft(input);
        if (!AnnouncementRules.ValidRowVersion(input.RowVersion, out var version))
            ModelState.AddModelError("Input.RowVersion", "Draft changed. Reload the page.");
        if (!ModelState.IsValid)
            return View("Editor", new AnnouncementEditorPage(id, input));

        db.Entry(item).Property(x => x.RowVersion).OriginalValue = version;
        item.Title = input.Title;
        item.Body = input.Body;
        item.Category = input.Category;
        item.ExpiresAtUtc = input.ExpiresAtUtc;
        item.UpdatedAtUtc = DateTime.UtcNow;
        await using var tx = await db.Database.BeginTransactionAsync(ct);
        try
        {
            await audit.RecordAsync(actor.Id, "web", "announcement-edited",
                "announcement", id, null, ct);
            await tx.CommitAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "Someone else updated this draft. Reload to review the latest version.";
            return RedirectToAction(nameof(Details), new { id });
        }
        TempData["Message"] = "Draft updated.";
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost("{id:guid}/publish"), ValidateAntiForgeryToken]
    [EnableRateLimiting(EnrollmentRateLimiting.AdminActions)]
    public async Task<IActionResult> Publish(Guid id, [FromForm] string? rowVersion,
        [FromForm] string? currentPassword, [FromForm] string? reason, CancellationToken ct)
    {
        var actor = await ActiveAdmin();
        if (actor is null) return Forbid();
        var item = await db.PlatformAnnouncements.SingleOrDefaultAsync(x => x.Id == id, ct);
        if (item is null) return NotFound();
        if (item.State != "Draft")
        {
            TempData["Error"] = "Only drafts can be published.";
            return RedirectToAction(nameof(Details), new { id });
        }
        if (!await ConfirmAction(actor, currentPassword, reason) ||
            !AnnouncementRules.ValidRowVersion(rowVersion, out var version))
        {
            TempData["Error"] = "A valid reason, current admin password, and fresh draft are required.";
            return RedirectToAction(nameof(Details), new { id });
        }
        var now = DateTime.UtcNow;
        if (!AnnouncementRules.ValidExpiry(item.ExpiresAtUtc, now))
        {
            TempData["Error"] = "This draft's expiry has passed. Edit the expiry before publishing.";
            return RedirectToAction(nameof(Details), new { id });
        }

        await using var tx = await db.Database.BeginTransactionAsync(ct);
        db.Entry(item).Property(x => x.RowVersion).OriginalValue = version;
        item.State = "Published";
        item.PublishedAtUtc = now;
        item.UpdatedAtUtc = now;
        try
        {
            await audit.RecordAsync(actor.Id, "web", "announcement-published",
                "announcement", id, reason!.Trim(), ct);
            await tx.CommitAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "Draft changed while publishing. Reload before trying again.";
            return RedirectToAction(nameof(Details), new { id });
        }
        TempData["Message"] = "Announcement published to signed-in users.";
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost("{id:guid}/archive"), ValidateAntiForgeryToken]
    [EnableRateLimiting(EnrollmentRateLimiting.AdminActions)]
    public async Task<IActionResult> Archive(Guid id, [FromForm] string? rowVersion,
        [FromForm] string? currentPassword, [FromForm] string? reason, CancellationToken ct)
    {
        var actor = await ActiveAdmin();
        if (actor is null) return Forbid();
        var item = await db.PlatformAnnouncements.SingleOrDefaultAsync(x => x.Id == id, ct);
        if (item is null) return NotFound();
        if (item.State != "Published")
        {
            TempData["Error"] = "Only published announcements can be archived.";
            return RedirectToAction(nameof(Details), new { id });
        }
        if (!await ConfirmAction(actor, currentPassword, reason) ||
            !AnnouncementRules.ValidRowVersion(rowVersion, out var version))
        {
            TempData["Error"] = "A valid reason, current admin password, and fresh record are required.";
            return RedirectToAction(nameof(Details), new { id });
        }
        await using var tx = await db.Database.BeginTransactionAsync(ct);
        db.Entry(item).Property(x => x.RowVersion).OriginalValue = version;
        item.State = "Archived";
        item.UpdatedAtUtc = DateTime.UtcNow;
        try
        {
            await audit.RecordAsync(actor.Id, "web", "announcement-archived",
                "announcement", id, reason!.Trim(), ct);
            await tx.CommitAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "Announcement changed. Reload before trying again.";
            return RedirectToAction(nameof(Details), new { id });
        }
        TempData["Message"] = "Announcement archived. It is no longer visible to users.";
        return RedirectToAction(nameof(Details), new { id });
    }

    private async Task<bool> ConfirmAction(SmartFarmUser actor, string? password, string? reason) =>
        reason?.Trim().Length is >= 8 and <= 300 &&
        AdminActionSecurity.ValidPasswordInput(password) &&
        await users.CheckPasswordAsync(actor, password!);

    private static void Normalize(AnnouncementInput input)
    {
        input.Title = input.Title?.Trim() ?? "";
        input.Body = input.Body?.Trim() ?? "";
        input.Category = input.Category?.Trim() ?? "";
        if (input.ExpiresAtUtc.HasValue)
            input.ExpiresAtUtc = DateTime.SpecifyKind(input.ExpiresAtUtc.Value, DateTimeKind.Utc);
    }

    private void ValidateDraft(AnnouncementInput input)
    {
        if (input.Title.Length is < 5 or > 140)
            ModelState.AddModelError("Input.Title", "Use a title between 5 and 140 characters.");
        if (input.Body.Length is < 15 or > 2000)
            ModelState.AddModelError("Input.Body", "Use 15 to 2000 characters.");
        if (!AnnouncementRules.Categories.Contains(input.Category))
            ModelState.AddModelError("Input.Category", "Choose a listed category.");
        if (!AnnouncementRules.ValidExpiry(input.ExpiresAtUtc, DateTime.UtcNow))
            ModelState.AddModelError("Input.ExpiresAtUtc", "Expiry must be later than now (UTC).");
    }
}
