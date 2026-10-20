using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class FarmTaskRepository(SmartFarmDbContext db) : IFarmTaskRepository
{
    public async Task<IReadOnlyList<FarmTask>> ListAsync(Guid farmId, CancellationToken ct = default) =>
        await db.FarmTasks.AsNoTracking().Where(t => t.FarmId == farmId)
            .OrderBy(t => t.Status).ThenBy(t => t.DueDate).ThenBy(t => t.CreatedAtUtc)
            .ToListAsync(ct);

    public Task<FarmTask?> FindAsync(Guid farmId, Guid taskId, CancellationToken ct = default) =>
        db.FarmTasks.SingleOrDefaultAsync(t => t.FarmId == farmId && t.Id == taskId, ct);

    public Task<bool> CropBelongsToFarmAsync(Guid farmId, Guid cycleId, CancellationToken ct = default) =>
        db.CropCycles.AsNoTracking().AnyAsync(c => c.Id == cycleId &&
            db.Plots.Any(p => p.Id == c.PlotId && p.FarmId == farmId && p.ArchivedAtUtc == null), ct);

    public Task<bool> IsAssignableAsync(Guid farmId, Guid userId, CancellationToken ct = default) =>
        db.Farms.AsNoTracking().AnyAsync(f => f.Id == farmId && f.ArchivedAtUtc == null &&
            ((f.OwnerUserId == userId && db.Users.Any(u => u.Id == userId && u.DisabledAtUtc == null)) ||
             db.FarmMembers.Any(m => m.FarmId == farmId && m.UserId == userId &&
                 m.RemovedAtUtc == null && db.Users.Any(u => u.Id == userId && u.DisabledAtUtc == null))), ct);

    public void Add(FarmTask task) => db.FarmTasks.Add(task);

    public void SetOriginalVersion(FarmTask task, byte[] version) =>
        db.Entry(task).Property(t => t.RowVersion).OriginalValue = version;

    public async Task<IReadOnlyList<Notification>> ListNotificationsAsync(Guid userId, CancellationToken ct = default) =>
        await db.Notifications.AsNoTracking().Where(n => n.UserId == userId)
            .OrderByDescending(n => n.CreatedAtUtc).Take(100).ToListAsync(ct);

    public Task<Notification?> FindNotificationAsync(Guid userId, Guid notificationId, CancellationToken ct = default) =>
        db.Notifications.SingleOrDefaultAsync(n => n.UserId == userId && n.Id == notificationId, ct);
}
