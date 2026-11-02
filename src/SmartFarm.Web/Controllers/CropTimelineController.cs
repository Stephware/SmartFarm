using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Monitoring;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class CropTimelineController(
    UserManager<SmartFarmUser> users, ICropManagementService crops,
    IFarmManagementService farms, IMonitoringService monitoring,
    IHarvestService harvests, IFarmTaskService tasks) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, Guid cycleId, CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try
        {
            var farm = await farms.GetAsync(farmId, actor, ct);
            var cycle = await crops.GetCycleAsync(farmId, cycleId, actor, ct);
            if (farm is null || cycle is null) return NotFound();

            var activities = await monitoring.GetAsync(farmId, cycleId, actor, ct);
            var harvest = await harvests.GetAsync(farmId, cycleId, actor, ct);
            var farmTasks = await tasks.ListAsync(farmId, actor, ct);
            return View(CropTimelineBuilder.Build(cycle, activities, harvest,
                farmTasks, farm.TimeZoneId));
        }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        { return NotFound(); }
    }
}
