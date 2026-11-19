using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Query;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Services.Queries;

namespace SmartFarm.Api.Controllers;

[ApiController, Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/records")]
public sealed class FarmRecordsController(UserManager<SmartFarmUser> users, FarmRecordQueryService records) : FarmApiControllerBase(users)
{
    [HttpGet("{resource}")]
    public Task<IActionResult> List(Guid farmId, string resource, [FromQuery] PageRequest query, CancellationToken ct) =>
        Result(async () => Ok(await records.ReadAsync(farmId, ActorId, resource, query, ct)));
}
