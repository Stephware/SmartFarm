using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}")]
public sealed class FarmReportsController(
    UserManager<SmartFarmUser> users, IFarmReportService reports)
    : FarmApiControllerBase(users)
{
    [HttpGet("dashboard")]
    public Task<IActionResult> Dashboard(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await reports.DashboardAsync(farmId, ActorId, ct)));

    [HttpGet("reports/harvests")]
    public Task<IActionResult> Harvests(Guid farmId, [FromQuery] DateOnly? from,
        [FromQuery] DateOnly? to, [FromQuery] Guid? plotId,
        [FromQuery] Guid? cropTypeId, CancellationToken ct) =>
        Result(async () => Ok(await reports.HarvestsAsync(
            farmId, ActorId, from, to, plotId, cropTypeId, ct)));
}
