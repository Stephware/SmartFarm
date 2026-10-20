using Microsoft.AspNetCore.Mvc;

namespace SmartFarm.Web.Controllers;

public sealed class HomeController : Controller
{
    public IActionResult Index() => View();
}
