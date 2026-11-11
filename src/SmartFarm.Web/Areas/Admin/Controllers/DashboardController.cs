using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Areas.Admin.Controllers;

[Area("Admin")]
[Route("admin")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
public sealed class DashboardController : Controller
{
    [HttpGet("")]
    public IActionResult Index() => View();
}
