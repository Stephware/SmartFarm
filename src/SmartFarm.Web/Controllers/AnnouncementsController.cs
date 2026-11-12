using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Announcements;

namespace SmartFarm.Web.Controllers;

// A global bulletin feed; no farm-role elevation and no exposure of drafts.
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
[Route("announcements")]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class AnnouncementsController(SmartFarmDbContext db,
    UserManager<SmartFarmUser> users) : Controller
{
    private async Task<bool> ActiveUser() =>
        (await users.GetUserAsync(User)) is { DisabledAtUtc: null };

    [HttpGet("")]
    public async Task<IActionResult> Index(int page = 1, CancellationToken ct = default)
    {
        if (!await ActiveUser()) return Forbid();
        page = Math.Clamp(page, 1, 100000);
        var now = DateTime.UtcNow;
        var query = db.PlatformAnnouncements.AsNoTracking().Where(a =>
            a.State == "Published" && a.PublishedAtUtc != null && a.PublishedAtUtc <= now &&
            (a.ExpiresAtUtc == null || a.ExpiresAtUtc > now));
        var total = await query.CountAsync(ct);
        var rows = await query.OrderByDescending(a => a.PublishedAtUtc).ThenBy(a => a.Id)
            .Skip((page - 1) * PublicAnnouncementIndexPage.PageSize)
            .Take(PublicAnnouncementIndexPage.PageSize).ToListAsync(ct);
        return View(new PublicAnnouncementIndexPage(page, total,
            rows.Select(AnnouncementMapper.ToRow).ToArray()));
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        if (!await ActiveUser()) return Forbid();
        var now = DateTime.UtcNow;
        var item = await db.PlatformAnnouncements.AsNoTracking()
            .SingleOrDefaultAsync(a => a.Id == id && a.State == "Published" &&
                a.PublishedAtUtc != null && a.PublishedAtUtc <= now &&
                (a.ExpiresAtUtc == null || a.ExpiresAtUtc > now), ct);
        return item is null ? NotFound() : View(AnnouncementMapper.ToRow(item));
    }
}
