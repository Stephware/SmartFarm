using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.Services.Admin;

namespace SmartFarm.Web.Areas.Admin.Controllers;

// Read-only diagnostics. No remote job execution, retries, database mutations,
// private operation payloads or unauthenticated health endpoints.
[Area("Admin")]
[Route("admin/monitoring")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class MonitoringController(AdminDiagnosticsService diagnostics) : Controller
{
    [HttpGet("")]
    public async Task<IActionResult> Index(CancellationToken ct) =>
        View(await diagnostics.OverviewAsync(ct));

    [HttpGet("sync")]
    public async Task<IActionResult> Sync(string? search, string? status, string? type,
        int page = 1, CancellationToken ct = default) =>
        View(await diagnostics.SyncHistoryAsync(search, status, type, page, ct));
}
