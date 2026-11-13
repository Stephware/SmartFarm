using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.Services.Admin;
using SmartFarm.Web.ViewModels.Admin;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class DashboardController(SmartFarmDbContext db, AdminDiagnosticsService diagnostics) : Controller
{
    [HttpGet("")]
    public async Task<IActionResult> Index(CancellationToken ct)
    {
        var totalUsers = await db.Users.CountAsync(ct);
        var disabledUsers = await db.Users.CountAsync(u => u.DisabledAtUtc != null, ct);
        var totalFarms = await db.Farms.CountAsync(ct);
        var archivedFarms = await db.Farms.CountAsync(f => f.ArchivedAtUtc != null, ct);
        var activePlots = await db.Plots.CountAsync(p => p.ArchivedAtUtc == null, ct);
        var recent = await db.Farms.AsNoTracking()
            .OrderByDescending(f => f.CreatedAtUtc).Take(6).ToListAsync(ct);
        return View(new AdminOverviewPage(
            totalUsers, totalUsers - disabledUsers, disabledUsers,
            totalFarms, totalFarms - archivedFarms, archivedFarms, activePlots,
            await db.SupportTickets.CountAsync(t => t.Status == "Open" || t.Status == "InProgress", ct),
            await AdminFarmDirectory.LoadAsync(db, recent, ct))
        {
            Worker = await diagnostics.WorkerSummaryAsync(ct)
        });
    }
}
