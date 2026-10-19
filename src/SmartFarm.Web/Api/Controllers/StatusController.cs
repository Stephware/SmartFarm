using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace SmartFarm.Web.Api.Controllers;

// An anonymous, database-independent smoke test. No private environment
// information is included in the response.
[ApiController]
[AllowAnonymous]
[Route("api/status")]
public sealed class StatusController : ControllerBase
{
    [HttpGet]
    public IActionResult Get() => Ok(new { app = "SmartFarm", status = "ok" });
}

