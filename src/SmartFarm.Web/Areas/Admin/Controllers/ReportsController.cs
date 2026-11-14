using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/reports")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class ReportsController : Controller
{
    [HttpGet("")]
    public IActionResult Index() => StatusCode(410, "Platform analytics are retired. Operational diagnostics remain in Monitoring.");

    [HttpGet("csv")]
    public IActionResult Csv() => StatusCode(410, "Platform analytics are retired. Audit history is retained.");
}
