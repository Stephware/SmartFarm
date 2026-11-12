using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Web.ViewModels.Farms;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class DashboardController(
    UserManager<SmartFarmUser> users, IFarmReportService reports, IFarmManagementService farms,
    IFarmTaskService tasks, ICropManagementService crops, IFarmWeatherService weather,
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
            // All supporting records remain scoped to this authorized farm.
            // Keep the existing dashboard report as the source of metric totals.
            var report = await reports.DashboardAsync(farmId, actor, ct);
            var plots = await farms.ListPlotsAsync(farmId, actor, ct);
            var cycles = await crops.ListCyclesAsync(farmId, actor, ct);
            var scheduled = await tasks.ListAsync(farmId, actor, ct);
            SmartFarm.Core.Contracts.Weather.WeatherOverview? savedForecast = null;
            try { savedForecast = await weather.GetAsync(farmId, actor, ct); }
            catch (Exception error) when (error is System.Data.Common.DbException or HttpRequestException)
            {
                logger.LogWarning(error, "Saved weather unavailable for dashboard");
            }
            return View(new FarmDashboardViewModel(report, panel)
            {
                Plots = plots,
                Cycles = cycles,
                Tasks = scheduled,
                Weather = savedForecast,
                FarmRole = farm.Role,
                ActorId = actor
            });
        }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        { return NotFound(); }
    }
}
