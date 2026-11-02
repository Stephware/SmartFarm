using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Web.ViewModels.Farms;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class DashboardController(
    UserManager<SmartFarmUser> users, IFarmReportService reports, IFarmManagementService farms,
    IFarmEnrollmentService enrollment, ILogger<DashboardController> logger) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try
        {
            var farm = await farms.GetAsync(farmId, actor, ct);
            if (farm is null) return NotFound();
            FarmEnrollmentPanel? panel = null;
            if (farm.Role == "Owner")
            {
                try { panel = new(farmId, await enrollment.OwnerOverviewAsync(farmId, actor, ct), true); }
                catch (Exception error) when (error is System.Data.Common.DbException or HttpRequestException or FarmOperationException)
                {
                    logger.LogError(error, "Could not load owner join requests");
                    panel = new(farmId, null, true, "Join requests are unavailable. Refresh this page to try again.");
                }
            }
            return View(new FarmDashboardViewModel(await reports.DashboardAsync(farmId, actor, ct), panel));
        }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        { return NotFound(); }
    }
}
