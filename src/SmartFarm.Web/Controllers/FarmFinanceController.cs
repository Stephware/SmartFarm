using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

// Compatibility tombstones; the authenticated API retains historical cash records.
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class FarmFinanceController : Controller
{
    [HttpGet]
    public IActionResult Index() => StatusCode(410, "Cash tools are retired. Use Harvests for farm harvest reports.");

    [HttpGet]
    public IActionResult ExportInsightsCsv() => StatusCode(410, "Cash reports are retired. Historical records are retained.");

    [HttpPost, ValidateAntiForgeryToken]
    public IActionResult Record() => StatusCode(410, "Cash entry is retired. No record was saved.");

    [HttpPost, ValidateAntiForgeryToken]
    public IActionResult Void() => StatusCode(410, "Cash changes are retired. Existing records are unchanged.");
}
