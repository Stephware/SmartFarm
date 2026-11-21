using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Security;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Services.Admin;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Admin;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/farms")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class FarmsController(
    SmartFarmDbContext db, Microsoft.AspNetCore.Identity.UserManager<SmartFarmUser> users,
    AdminAuditWriter audit) : Controller
{
    [HttpGet("")]
    public async Task<IActionResult> Index(string? search, string? status, int page = 1,
        CancellationToken ct = default)
    {
        var term = (search ?? "").Trim();
        if (term.Length > 100) term = term[..100];
        var selected = status is "active" or "archived" ? status : "all";
        page = Math.Clamp(page, 1, 100000);
        var query = db.Farms.AsNoTracking().AsQueryable();
        if (term.Length > 0)
            query = query.Where(f => f.Name.Contains(term) || f.LocationText.Contains(term));
        if (selected == "active") query = query.Where(f => f.ArchivedAtUtc == null);
        if (selected == "archived") query = query.Where(f => f.ArchivedAtUtc != null);
        var total = await query.CountAsync(ct);
        var farms = await query.OrderByDescending(f => f.CreatedAtUtc).ThenBy(f => f.Id)
            .Skip((page - 1) * AdminFarmIndexPage.PageSize)
            .Take(AdminFarmIndexPage.PageSize).ToListAsync(ct);
        return View(new AdminFarmIndexPage(term, selected, page, total,
            await AdminFarmDirectory.LoadAsync(db, farms, ct)));
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        var farm = await db.Farms.AsNoTracking().SingleOrDefaultAsync(f => f.Id == id, ct);
        if (farm is null) return NotFound();
        var activeCycles = await db.CropCycles.AsNoTracking().CountAsync(c => c.Status == 1 &&
            db.Plots.Any(p => p.Id == c.PlotId && p.FarmId == id), ct);
        var stockItems = await db.InventoryItems.AsNoTracking().CountAsync(i =>
            i.FarmId == id && i.ArchivedAtUtc == null && i.QuantityOnHand > 0, ct);
        var row = (await AdminFarmDirectory.LoadAsync(db, [farm], ct))[0];
        return View(new AdminFarmDetailPage(row, activeCycles, stockItems));
    }

    [HttpPost("{id:guid}/status")]
    [ValidateAntiForgeryToken]
    [EnableRateLimiting(EnrollmentRateLimiting.AdminActions)]
    public async Task<IActionResult> ChangeStatus(
        Guid id, [FromForm] bool archive, [FromForm] string? rowVersion,
        [FromForm] string? reason, [FromForm] string? currentPassword,
        CancellationToken ct)
    {
        var actor = await users.GetUserAsync(User);
        if (actor is null || actor.DisabledAtUtc is not null) return Forbid();
        var farm = await db.Farms.SingleOrDefaultAsync(f => f.Id == id, ct);
        if (farm is null) return NotFound();

        var note = reason?.Trim() ?? "";
        if (note.Length is < 8 or > 300)
        {
            TempData["Error"] = "Provide a reason between 8 and 300 characters.";
            return RedirectToAction(nameof(Details), new { id });
        }
        byte[] version;
        try { version = Convert.FromBase64String(rowVersion ?? ""); }
        catch (FormatException) { version = []; }
        if (version.Length != 8)
        {
            TempData["Error"] = "Farm details are out of date. Reload and try again.";
            return RedirectToAction(nameof(Details), new { id });
        }
        if ((farm.ArchivedAtUtc is not null) == archive)
        {
            TempData["Error"] = "Farm status changed. Reload and try again.";
            return RedirectToAction(nameof(Details), new { id });
        }

        // Preserve the same safety gates used when an owner archives a farm.
        if (archive)
        {
            if (await db.CropCycles.AnyAsync(c => c.Status == 1 &&
                db.Plots.Any(p => p.Id == c.PlotId && p.FarmId == id), ct) ||
                await db.InventoryItems.AnyAsync(i => i.FarmId == id &&
                    i.ArchivedAtUtc == null && i.QuantityOnHand > 0, ct))
            {
                TempData["Error"] = "Complete active crop cycles and clear remaining stock before archiving.";
                return RedirectToAction(nameof(Details), new { id });
            }
        }

        // Never archive or restore a farm using only an existing MFA cookie.
        if (!AdminActionSecurity.ValidPasswordInput(currentPassword) ||
            !await users.CheckPasswordAsync(actor, currentPassword!))
        {
            TempData["Error"] = "Current admin password is incorrect. No changes were made.";
            return RedirectToAction(nameof(Details), new { id });
        }

        await using var tx = await db.Database.BeginTransactionAsync(ct);
        db.Entry(farm).Property(f => f.RowVersion).OriginalValue = version;
        farm.ArchivedAtUtc = archive ? DateTime.UtcNow : null;
        try
        {
            await audit.RecordAsync(actor.Id, "web", archive ? "farm-archived" : "farm-restored",
                "farm", id, note, ct);
            await tx.CommitAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            await tx.RollbackAsync(ct);
            TempData["Error"] = "Farm was updated by another user. Reload and try again.";
            return RedirectToAction(nameof(Details), new { id });
        }
        TempData["Message"] = archive ? "Farm archived." : "Farm restored.";
        return RedirectToAction(nameof(Details), new { id });
    }
}
