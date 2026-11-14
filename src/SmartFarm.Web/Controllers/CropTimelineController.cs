using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class CropTimelineController : Controller
{
    [HttpGet]
    public IActionResult Index(Guid farmId, Guid cycleId) =>
        RedirectToAction("Index", "Crops", new { farmId, cycleId }, "crop-history");
}
