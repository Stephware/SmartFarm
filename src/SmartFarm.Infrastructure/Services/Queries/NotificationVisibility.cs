using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Persistence;
namespace SmartFarm.Infrastructure.Services.Queries;

public static class NotificationVisibility
{
    public static IQueryable<SmartFarm.Core.Entities.Notification> ForActor(SmartFarmDbContext db, Guid actor) => db.Notifications.Where(n => n.UserId == actor &&
        db.Users.Any(u => u.Id == actor && u.DisabledAtUtc == null) &&
        (n.FarmTaskId == null || db.FarmTasks.Any(t => t.Id == n.FarmTaskId && db.Farms.Any(f => f.Id == t.FarmId && f.ArchivedAtUtc == null &&
            (f.OwnerUserId == actor || db.FarmMembers.Any(m => m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null))))) &&
        (n.WeatherAdvisoryId == null || db.WeatherAdvisories.Any(a => a.Id == n.WeatherAdvisoryId && db.Farms.Any(f => f.Id == a.FarmId && f.ArchivedAtUtc == null &&
            (f.OwnerUserId == actor || db.FarmMembers.Any(m => m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null))))) &&
        (n.Type != 3 || db.Farms.Any(f => f.ArchivedAtUtc == null && n.DeduplicationKey.StartsWith("stock:" + f.Id.ToString().Replace("-", "") + ":") &&
            (f.OwnerUserId == actor || db.FarmMembers.Any(m => m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null && m.Role == 1)))));
}
