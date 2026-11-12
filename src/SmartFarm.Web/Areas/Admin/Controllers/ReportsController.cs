using System.Globalization;
using System.Text;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.Services.Admin;
using SmartFarm.Web.ViewModels.Admin;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin/reports")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class ReportsController(AdminReportService reports) : Controller
{
    [HttpGet("")]
    public async Task<IActionResult> Index(int? days, CancellationToken ct) =>
        View(await reports.BuildAsync(days, ct));

    [HttpGet("csv")]
    public async Task<IActionResult> Csv(int? days, CancellationToken ct)
    {
        var data = await reports.BuildAsync(days, ct);
        var csv = new StringBuilder("UTC date,New farms,Harvest events,New support cases,Successful sync receipts\r\n");
        foreach (var day in data.Daily)
        {
            csv.Append(day.Date.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture)).Append(',')
                .Append(day.Farms).Append(',')
                .Append(day.HarvestEvents).Append(',')
                .Append(day.SupportRequests).Append(',')
                .Append(day.SuccessfulSyncReceipts).Append("\r\n");
        }

        // No user-entered fields or cell formulas. CSV contains numeric aggregate counts only.
        return File(new UTF8Encoding(true).GetPreamble()
                .Concat(Encoding.UTF8.GetBytes(csv.ToString())).ToArray(),
            "text/csv", $"anigo-platform-report-{data.Days}-days.csv");
    }
}
