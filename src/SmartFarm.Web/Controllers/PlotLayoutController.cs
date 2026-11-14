using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class PlotLayoutController : Controller
{
    [HttpGet]
    public IActionResult Index(Guid farmId) =>
        RedirectToAction("Index", "Crops", new { farmId }, "farm-plots");
}
