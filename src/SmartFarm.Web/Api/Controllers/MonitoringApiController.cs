using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/crop-cycles/{cycleId:guid}/monitoring")]
public sealed class MonitoringApiController(
    UserManager<SmartFarmUser> users,
    IMonitoringService monitoring) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Get(Guid farmId, Guid cycleId, CancellationToken ct) =>
        Result(async () => Ok(await monitoring.GetAsync(farmId, cycleId, ActorId, ct)));

    [HttpPost("activities")]
    public Task<IActionResult> Activity(Guid farmId, Guid cycleId,
        [FromBody] RecordActivityInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var activity = await monitoring.RecordActivityAsync(farmId, cycleId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, activity);
        });

    [HttpPost("observations")]
    public Task<IActionResult> Observation(Guid farmId, Guid cycleId,
        [FromBody] RecordObservationInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var observation = await monitoring.RecordObservationAsync(farmId, cycleId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, observation);
        });

    [HttpPost("observations/{observationId:guid}/photos")]
    [RequestSizeLimit(6 * 1024 * 1024)]
    [RequestFormLimits(MultipartBodyLengthLimit = 6 * 1024 * 1024)]
    public Task<IActionResult> Upload(Guid farmId, Guid cycleId, Guid observationId,
        [FromForm] IFormFile? photo, CancellationToken ct) =>
        Result(async () =>
        {
            if (photo is null) return BadRequest(new { message = "Select a photo." });
            await using var input = photo.OpenReadStream();
            var saved = await monitoring.AddPhotoAsync(
                farmId, cycleId, observationId, ActorId,
                input, photo.Length, photo.ContentType, ct);
            return StatusCode(StatusCodes.Status201Created, saved);
        });

    [HttpGet("photos/{photoId:guid}")]
    public Task<IActionResult> Photo(Guid farmId, Guid cycleId, Guid photoId, CancellationToken ct) =>
        Result(async () =>
        {
            var (stream, contentType) = await monitoring.OpenPhotoAsync(
                farmId, cycleId, photoId, ActorId, ct);
            Response.Headers["X-Content-Type-Options"] = "nosniff";
            Response.Headers.CacheControl = "private, no-store";
            return File(stream, contentType);
        });
}

