using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Enums;

namespace SmartFarm.Api.Controllers;

// Discoverable, stable code descriptions for clients; no farm data is exposed.
[ApiController]
[AllowAnonymous]
[Route("api/reference-data")]
public sealed class ReferenceDataController : ControllerBase
{
    [HttpGet]
    public IActionResult Get() => Ok(new
    {
        activityTypes = FarmCodeLabels.Activities,
        healthStatuses = FarmCodeLabels.HealthStatuses,
        taskTypes = FarmCodeLabels.TaskTypes,
        taskCloseStatuses = FarmCodeLabels.TaskCloseStatuses,
        memberRoles = FarmCodeLabels.MemberRoles,
        cropStatuses = FarmCodeLabels.CropStatuses,
        inventoryCategories = FarmCodeLabels.InventoryCategories,
        inventoryMovementTypes = FarmCodeLabels.InventoryMovementTypes,
        notificationTypes = FarmCodeLabels.NotificationTypes,
        weatherAdvisoryTypes = FarmCodeLabels.WeatherAdvisories,
        weatherSeverities = FarmCodeLabels.WeatherSeverities,
        weatherStatuses = FarmCodeLabels.WeatherStatuses
    });
}
