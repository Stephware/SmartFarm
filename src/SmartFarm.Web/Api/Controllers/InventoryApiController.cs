using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/inventory")]
public sealed class InventoryApiController(
    UserManager<SmartFarmUser> users, IInventoryService inventory) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await inventory.ListAsync(farmId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Create(Guid farmId, [FromBody] InventoryItemInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var item = await inventory.CreateAsync(farmId, ActorId, input, ct);
            return CreatedAtAction(nameof(Get), new { farmId, itemId = item.Id }, item);
        });

    [HttpGet("{itemId:guid}")]
    public Task<IActionResult> Get(Guid farmId, Guid itemId, CancellationToken ct) =>
        Result(async () =>
            await inventory.GetAsync(farmId, itemId, ActorId, ct) is { } item ? Ok(item) : NotFound());

    [HttpPut("{itemId:guid}")]
    public Task<IActionResult> Update(Guid farmId, Guid itemId,
        [FromBody] InventoryItemInput input, CancellationToken ct) =>
        Result(async () => Ok(await inventory.UpdateAsync(farmId, itemId, ActorId, input, ct)));

    [HttpDelete("{itemId:guid}")]
    public Task<IActionResult> Archive(Guid farmId, Guid itemId,
        [FromBody] RowVersionInput input, CancellationToken ct) =>
        Result(async () =>
        {
            await inventory.ArchiveAsync(farmId, itemId, ActorId, input.RowVersion, ct);
            return NoContent();
        });

    [HttpGet("{itemId:guid}/transactions")]
    public Task<IActionResult> History(Guid farmId, Guid itemId, CancellationToken ct) =>
        Result(async () => Ok(await inventory.HistoryAsync(farmId, itemId, ActorId, ct)));

    [HttpPost("{itemId:guid}/transactions")]
    public Task<IActionResult> Post(Guid farmId, Guid itemId,
        [FromBody] InventoryMovementInput input, CancellationToken ct) =>
        Result(async () => Ok(await inventory.PostAsync(farmId, itemId, ActorId, input, ct)));

    [HttpPost("{itemId:guid}/transactions/{transactionId:guid}/reverse")]
    public Task<IActionResult> Reverse(Guid farmId, Guid itemId, Guid transactionId,
        [FromBody] InventoryReversalInput input, CancellationToken ct) =>
        Result(async () => Ok(await inventory.ReverseAsync(
            farmId, itemId, transactionId, ActorId, input, ct)));

    [HttpGet("activities")]
    public Task<IActionResult> Activities(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await inventory.ActivityOptionsAsync(farmId, ActorId, ct)));
}

