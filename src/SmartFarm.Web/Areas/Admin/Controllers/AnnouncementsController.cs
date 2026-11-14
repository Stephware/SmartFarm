using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Announcements;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/announcements")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class AnnouncementsController(
    SmartFarmDbContext db, UserManager<SmartFarmUser> users) : Controller
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
            await db.PlatformAnnouncements.CountAsync(x => x.State == "Published", ct),
            rows.Select(AnnouncementMapper.ToRow).ToArray()));
    }

    [HttpGet("new")]
    public IActionResult Create() => StatusCode(410, "Announcement publishing is retired. History is read-only.");

    [HttpPost("new"), ValidateAntiForgeryToken]
    public IActionResult Create([FromForm] string? body) => StatusCode(410, "Announcement publishing is retired.");

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        if (await ActiveAdmin() is null) return Forbid();
        var item = await db.PlatformAnnouncements.AsNoTracking()
            .SingleOrDefaultAsync(x => x.Id == id, ct);
        return item is null ? NotFound() : View(AnnouncementMapper.ToRow(item));
    }

    [HttpGet("{id:guid}/edit")]
    public IActionResult Edit(Guid id) => StatusCode(410, "Announcement history is read-only.");

    [HttpPost("{id:guid}/edit"), ValidateAntiForgeryToken]
    public IActionResult Edit(Guid id, [FromForm] string? body) => StatusCode(410, "Announcement history is read-only.");

    [HttpPost("{id:guid}/publish"), ValidateAntiForgeryToken]
    public IActionResult Publish(Guid id) => StatusCode(410, "Announcement publishing is retired.");

    [HttpPost("{id:guid}/archive"), ValidateAntiForgeryToken]
    public IActionResult Archive(Guid id) => StatusCode(410, "Announcement history is read-only.");
}
