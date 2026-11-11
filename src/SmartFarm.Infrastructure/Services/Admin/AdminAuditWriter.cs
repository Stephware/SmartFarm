using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Admin;

// A scoped writer. Call within the same transaction as any privileged mutation.
public sealed class AdminAuditWriter(SmartFarmDbContext db)
{
    public async Task RecordAsync(Guid? actor, string source, string action,
        string targetType, Guid? targetId, string? reason = null, CancellationToken ct = default)
    {
        db.AdminAuditLogs.Add(new AdminAuditLog
        {
            ActorUserId = actor, ActorSource = source, Action = action,
            TargetType = targetType, TargetId = targetId,
            Reason = reason, OccurredAtUtc = DateTime.UtcNow
        });
        await db.SaveChangesAsync(ct);
    }
}
