using System.Globalization;
using System.Text;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Reports;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class ReportsController(
    UserManager<SmartFarmUser> users, IFarmReportService reports,
    IFarmManagementService farms, ICropManagementService crops) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id)
        ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Harvests(Guid farmId, DateOnly? from,
        DateOnly? to, Guid? plotId, Guid? cropTypeId, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await reports.HarvestsAsync(farmId, Actor, from, to, plotId, cropTypeId, ct);
            var plots = await farms.ListPlotsAsync(farmId, Actor, ct);
            var types = await crops.ListCropTypesAsync(farmId, Actor, ct);
            return View(new HarvestReportPage(report, plots, types));
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound) { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }

    // Read-only report download; the API checks farm membership before any data is returned.
    [HttpGet]
    public async Task<IActionResult> ExportCsv(Guid farmId, DateOnly? from,
        DateOnly? to, Guid? plotId, Guid? cropTypeId, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await reports.HarvestsAsync(farmId, Actor, from, to, plotId, cropTypeId, ct);
            var content = HarvestCsv.Build(report);
            return File(Encoding.UTF8.GetBytes(content), "text/csv; charset=utf-8",
                $"smartfarm-harvests-{farmId:N}.csv");
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound) { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }
}
