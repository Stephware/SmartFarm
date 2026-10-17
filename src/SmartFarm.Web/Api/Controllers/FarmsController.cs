using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Web.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = IdentityConstants.BearerScheme)]
[Route("api/farms")]
public sealed class FarmsController(
    UserManager<SmartFarmUser> users, IFarmManagementService farms)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct) =>
        Ok(await farms.ListAsync(ActorId, ct));

    [HttpGet("{farmId:guid}")]
    public Task<IActionResult> Get(Guid farmId, CancellationToken ct) => Result(async () =>
        await farms.GetAsync(farmId, ActorId, ct) is { } farm ? Ok(farm) : NotFound());

    [HttpPost]
    public Task<IActionResult> Create([FromBody] FarmInput input, CancellationToken ct) => Result(async () =>
    {
        var created = await farms.CreateAsync(ActorId, input, ct);
        return CreatedAtAction(nameof(Get), new { farmId = created.Id }, created);
    });

    [HttpPut("{farmId:guid}")]
    public Task<IActionResult> Update(Guid farmId, [FromBody] FarmInput input, CancellationToken ct) => Result(async () =>
        Ok(await farms.UpdateAsync(farmId, ActorId, input, ct)));

    [HttpDelete("{farmId:guid}")]
    public Task<IActionResult> Archive(Guid farmId, [FromBody] RowVersionInput input, CancellationToken ct) => Result(async () =>
    {
        await farms.ArchiveAsync(farmId, ActorId, input.RowVersion, ct);
        return NoContent();
    });

    [HttpGet("{farmId:guid}/plots")]
    public Task<IActionResult> Plots(Guid farmId, CancellationToken ct) => Result(async () =>
        Ok(await farms.ListPlotsAsync(farmId, ActorId, ct)));

    [HttpGet("{farmId:guid}/plots/{plotId:guid}")]
    public Task<IActionResult> Plot(Guid farmId, Guid plotId, CancellationToken ct) => Result(async () =>
        await farms.GetPlotAsync(farmId, plotId, ActorId, ct) is { } plot ? Ok(plot) : NotFound());

    [HttpPost("{farmId:guid}/plots")]
    public Task<IActionResult> CreatePlot(Guid farmId, [FromBody] PlotInput input, CancellationToken ct) => Result(async () =>
    {
        var plot = await farms.CreatePlotAsync(farmId, ActorId, input, ct);
        return CreatedAtAction(nameof(Plot), new { farmId, plotId = plot.Id }, plot);
    });

    [HttpPut("{farmId:guid}/plots/{plotId:guid}")]
    public Task<IActionResult> UpdatePlot(Guid farmId, Guid plotId, [FromBody] PlotInput input, CancellationToken ct) =>
        Result(async () => Ok(await farms.UpdatePlotAsync(farmId, plotId, ActorId, input, ct)));

    [HttpDelete("{farmId:guid}/plots/{plotId:guid}")]
    public Task<IActionResult> ArchivePlot(Guid farmId, Guid plotId, [FromBody] RowVersionInput input, CancellationToken ct) => Result(async () =>
    {
        await farms.ArchivePlotAsync(farmId, plotId, ActorId, input.RowVersion, ct);
        return NoContent();
    });
}

