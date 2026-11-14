using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
[Route("announcements")]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class AnnouncementsController : Controller
{
    [HttpGet("")]
    public IActionResult Index() => StatusCode(410, "The platform bulletin is retired. Farm reminders remain in Notifications.");

    [HttpGet("{id:guid}")]
    public IActionResult Details(Guid id) => StatusCode(410, "The platform bulletin is retired. Historical records are retained.");
}
