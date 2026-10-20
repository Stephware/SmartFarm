using System.Security.Cryptography;
using System.Text.Json;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using System.Data;

namespace SmartFarm.Api.Controllers;

// A stable client operation ID makes retries safe even after a lost HTTP response.
public sealed class SyncWriteInput
{
    public Guid FarmId { get; set; }
    public Guid TargetId { get; set; }
    public string OperationType { get; set; } = "";
    public RecordActivityInput? Activity { get; set; }
    public RecordObservationInput? Observation { get; set; }
    public InventoryMovementInput? Movement { get; set; }
    public RecordHarvestInput? Harvest { get; set; }
}

public sealed record SyncWriteResult(Guid OperationId, string OperationType, JsonElement Result, bool Replayed);

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/sync/operations")]
public sealed class SyncOperationsController(
    UserManager<SmartFarmUser> users,
    SmartFarmDbContext db,
    IFarmAccessService access,
    IMonitoringService monitoring,
    IInventoryService inventory,
    IHarvestService harvests) : FarmApiControllerBase(users)
{
    [HttpPost("{operationId:guid}")]
    public Task<IActionResult> Submit(Guid operationId, [FromBody] SyncWriteInput input,
        CancellationToken ct) =>
        Result(async () =>
        {
            if (operationId == Guid.Empty || input.FarmId == Guid.Empty || input.TargetId == Guid.Empty)
                return BadRequest(new { message = "Valid operation, farm and target IDs are required." });
            if (input.OperationType is not ("activity" or "observation" or "inventory" or "harvest"))
                return BadRequest(new { message = "Unsupported sync operation." });

            // Never reveal another farm's ledger or apply writes under another account.
            if (!await access.CanAsync(input.FarmId, ActorId, FarmAction.View, ct))
                return NotFound(new { message = "Resource not found." });

            var valid = input.OperationType switch
            {
                "activity" => input.Activity is not null && input.Observation is null &&
                    input.Movement is null && input.Harvest is null,
                "observation" => input.Observation is not null && input.Activity is null &&
                    input.Movement is null && input.Harvest is null,
                "inventory" => input.Movement is not null && input.Activity is null &&
                    input.Observation is null && input.Harvest is null,
                _ => input.Harvest is not null && input.Activity is null &&
                    input.Observation is null && input.Movement is null
            };
            if (!valid)
                return BadRequest(new { message = "Provide exactly one matching operation payload." });

            var hash = SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(input));
            await using var transaction = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable, ct);

            // UPDLOCK + HOLDLOCK also locks the gap when this GUID is not present.
            // Concurrent requests with the same ID cannot both execute the business write.
            var previous = await db.SyncOperations
                .FromSqlInterpolated($"""
                    SELECT * FROM dbo.SyncOperation WITH (UPDLOCK, HOLDLOCK)
                    WHERE OperationId = {operationId}
                    """)
                .AsNoTracking().SingleOrDefaultAsync(ct);
            if (previous is not null)
            {
                if (previous.UserId != ActorId || previous.FarmId != input.FarmId ||
                    previous.OperationType != input.OperationType ||
                    !CryptographicOperations.FixedTimeEquals(previous.RequestHash, hash))
                    return Conflict(new { message = "Operation ID was already used with different data." });

                var saved = JsonSerializer.Deserialize<JsonElement>(previous.ResultJson!);
                return Ok(new SyncWriteResult(operationId, input.OperationType, saved, true));
            }

            object result = input.OperationType switch
            {
                "activity" => await monitoring.RecordActivityAsync(
                    input.FarmId, input.TargetId, ActorId, input.Activity!, ct),
                "observation" => await monitoring.RecordObservationAsync(
                    input.FarmId, input.TargetId, ActorId, input.Observation!, ct),
                "inventory" => await inventory.PostAsync(
                    input.FarmId, input.TargetId, ActorId, input.Movement!, ct),
                _ => await harvests.RecordAsync(
                    input.FarmId, input.TargetId, ActorId, input.Harvest!, ct)
            };

            var resultJson = JsonSerializer.Serialize(result, new JsonSerializerOptions(JsonSerializerDefaults.Web));
            db.SyncOperations.Add(new SyncOperation
            {
                OperationId = operationId,
                FarmId = input.FarmId,
                UserId = ActorId,
                TargetEntityId = input.TargetId,
                OperationType = input.OperationType,
                RequestHash = hash,
                Status = 1,
                ResultJson = resultJson,
                ReceivedAtUtc = DateTime.UtcNow,
                CompletedAtUtc = DateTime.UtcNow
            });
            await db.SaveChangesAsync(ct);
            await transaction.CommitAsync(ct);

            return Ok(new SyncWriteResult(operationId, input.OperationType,
                JsonSerializer.Deserialize<JsonElement>(resultJson), false));
        });
}
