using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Security;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Services.Admin;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Admin;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/users")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class UsersController(
    SmartFarmDbContext db,
    UserManager<SmartFarmUser> users,
    AdminAuditWriter audit) : Controller
{
    [HttpGet("")]
    public async Task<IActionResult> Index(string? search, string? status, int page = 1,
        CancellationToken ct = default)
    {
        var term = (search ?? "").Trim();
        if (term.Length > 100) term = term[..100];
        var selected = status is "active" or "disabled" ? status : "all";
        page = Math.Clamp(page, 1, 100000);
        var query = db.Users.AsNoTracking().AsQueryable();
        if (term.Length > 0)
            query = query.Where(u => u.FullName.Contains(term) || (u.Email != null && u.Email.Contains(term)));
        if (selected == "active") query = query.Where(u => u.DisabledAtUtc == null);
        if (selected == "disabled") query = query.Where(u => u.DisabledAtUtc != null);

        var count = await query.CountAsync(ct);
        var pageUsers = await query.OrderBy(u => u.FullName).ThenBy(u => u.Id)
            .Skip((page - 1) * AdminUserIndexPage.PageSize)
            .Take(AdminUserIndexPage.PageSize).ToListAsync(ct);
        return View(new AdminUserIndexPage(term, selected, page, count,
            await ToRows(pageUsers, ct)));
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        var user = await db.Users.AsNoTracking().SingleOrDefaultAsync(u => u.Id == id, ct);
        if (user is null) return NotFound();
        var farms = await db.Farms.AsNoTracking()
            .Where(f => f.OwnerUserId == id)
            .OrderByDescending(f => f.CreatedAtUtc).Take(20).ToListAsync(ct);
        var row = (await ToRows([user], ct))[0];
        return View(new AdminUserDetailPage(row, await AdminFarmDirectory.LoadAsync(db, farms, ct)));
    }

    // No role edits, account deletion, password reset or farm membership mutations.
    [HttpPost("{id:guid}/status")]
    [ValidateAntiForgeryToken]
    [EnableRateLimiting(EnrollmentRateLimiting.AdminActions)]
    public async Task<IActionResult> ChangeStatus(
        Guid id, [FromForm] bool disable, [FromForm] string? reason,
        [FromForm] string? currentPassword, CancellationToken ct)
    {
        var actor = await users.GetUserAsync(User);
        if (actor is null || actor.DisabledAtUtc is not null) return Forbid();
        var target = await users.FindByIdAsync(id.ToString());
        if (target is null) return NotFound();
        if (target.Id == actor.Id || await users.IsInRoleAsync(target, SuperAdminSecurity.Role))
        {
            TempData["Error"] = "Super Admin accounts cannot be changed from this page.";
            return RedirectToAction(nameof(Details), new { id });
        }

        var note = reason?.Trim() ?? "";
        if (note.Length is < 8 or > 300)
        {
            TempData["Error"] = "Provide a reason between 8 and 300 characters.";
            return RedirectToAction(nameof(Details), new { id });
        }
        if ((target.DisabledAtUtc is not null) == disable)
        {
            TempData["Message"] = "Account status is already up to date.";
            return RedirectToAction(nameof(Details), new { id });
        }

        // MFA protects entry to /admin; a password check confirms this high-impact
        // account change is being performed by the person at the keyboard.
        if (!AdminActionSecurity.ValidPasswordInput(currentPassword) ||
            !await users.CheckPasswordAsync(actor, currentPassword!))
        {
            TempData["Error"] = "Current admin password is incorrect. No changes were made.";
            return RedirectToAction(nameof(Details), new { id });
        }

        await using var tx = await db.Database.BeginTransactionAsync(ct);
        target.DisabledAtUtc = disable ? DateTime.UtcNow : null;
        // Rotating the security stamp revokes sessions/refresh tokens on revalidation.
        var result = await users.UpdateSecurityStampAsync(target);
        if (!result.Succeeded)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "Account was changed by another request. Reload and try again.";
            return RedirectToAction(nameof(Details), new { id });
        }
        await audit.RecordAsync(actor.Id, "web",
            disable ? "user-disabled" : "user-restored", "user", target.Id, note, ct);
        await tx.CommitAsync(ct);
        TempData["Message"] = disable ? "Account disabled." : "Account restored.";
        return RedirectToAction(nameof(Details), new { id });
    }

    private async Task<IReadOnlyList<AdminUserRow>> ToRows(
        IReadOnlyList<SmartFarmUser> pageUsers, CancellationToken ct)
    {
        if (pageUsers.Count == 0) return [];
        var ids = pageUsers.Select(u => u.Id).ToArray();
        var adminRole = await db.Roles.AsNoTracking()
            .Where(r => r.NormalizedName == "SUPERADMIN")
            .Select(r => (Guid?)r.Id).FirstOrDefaultAsync(ct);
        var adminIds = adminRole is Guid roleId
            ? await db.UserRoles.AsNoTracking()
                .Where(ur => ur.RoleId == roleId && ids.Contains(ur.UserId))
                .Select(ur => ur.UserId).ToListAsync(ct)
            : new List<Guid>();
        var admins = adminIds.ToHashSet();
        var owned = await db.Farms.AsNoTracking()
            .Where(f => ids.Contains(f.OwnerUserId))
            .GroupBy(f => f.OwnerUserId)
            .Select(g => new { Id = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Id, g => g.Count, ct);
        var memberships = await db.FarmMembers.AsNoTracking()
            .Where(m => ids.Contains(m.UserId) && m.RemovedAtUtc == null)
            .GroupBy(m => m.UserId)
            .Select(g => new { Id = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Id, g => g.Count, ct);
        return pageUsers.Select(u => new AdminUserRow(
            u.Id, u.FullName, u.Email ?? "", u.DisabledAtUtc.HasValue,
            admins.Contains(u.Id), owned.GetValueOrDefault(u.Id),
            memberships.GetValueOrDefault(u.Id))).ToList();
    }
}
