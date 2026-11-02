using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/finance")]
public sealed class FarmFinanceController(
    UserManager<SmartFarmUser> users, IFarmFinanceService finance)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await finance.ListAsync(farmId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Record(Guid farmId, [FromBody] FarmFinanceInput input,
        CancellationToken ct) =>
        Result(async () => Ok(await finance.RecordAsync(farmId, ActorId, input, ct)));

    [HttpPost("{entryId:guid}/void")]
    public Task<IActionResult> Void(Guid farmId, Guid entryId,
        [FromBody] FarmFinanceVoidInput input, CancellationToken ct) =>
        Result(async () =>
        {
            await finance.VoidAsync(farmId, entryId, ActorId, input, ct);
            return NoContent();
        });
}
