using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/crop-types")]
public sealed class CropTypesController(
    UserManager<SmartFarmUser> users,
    ICropManagementService crops) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await crops.ListCropTypesAsync(farmId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Create(Guid farmId, [FromBody] CropTypeInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var created = await crops.CreateCropTypeAsync(farmId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, created);
        });
}

