using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/tasks")]
public sealed class FarmTasksController(
    UserManager<SmartFarmUser> users,
    IFarmTaskService tasks) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await tasks.ListAsync(farmId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Create(Guid farmId, [FromBody] FarmTaskInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var created = await tasks.CreateAsync(farmId, ActorId, input, ct);
            return Ok(created);
        });

    [HttpPut("{taskId:guid}/close")]
    public Task<IActionResult> Close(Guid farmId, Guid taskId, [FromBody] FarmTaskCloseInput input, CancellationToken ct) =>
        Result(async () => Ok(await tasks.CloseAsync(farmId, taskId, ActorId, input, ct)));
}
