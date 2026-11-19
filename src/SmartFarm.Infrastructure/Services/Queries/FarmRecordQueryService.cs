using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Query;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Queries;

// Additive bounded reads. Legacy list/sync paths deliberately do not call this service.
public sealed class FarmRecordQueryService(SmartFarmDbContext db, IFarmAccessService access)
{
    public async Task<object> ReadAsync(Guid farmId, Guid actor, string resource, PageRequest p, CancellationToken ct = default)
    {
        await Require(farmId, actor, resource is "members" or "enrollment" ? FarmAction.ManageMembers : FarmAction.View, ct);
        try { p.Validate(); } catch (ArgumentException e) { throw new FarmOperationException(FarmError.InvalidInput, e.Message); }
        var zone = await db.Farms.Where(f => f.Id == farmId).Select(f => f.TimeZoneId).SingleAsync(ct);
        DateTime? startUtc = p.From is { } lo ? FarmTime.StartOfDayUtc(lo, zone) : null;
        DateTime? until = p.To is { } hi && hi < DateOnly.MaxValue ? FarmTime.StartOfDayUtc(hi.AddDays(1), zone) : null;
        var cycles = db.CropCycles.Where(c => db.Plots.Any(plot => plot.Id == c.PlotId && plot.FarmId == farmId));
        var search = p.Search?.Trim();
        switch (resource)
        {
            case "tasks":
                var tasks = db.FarmTasks.AsNoTracking().Where(t => t.FarmId == farmId &&
                    (p.Status == null || t.Status == p.Status) && (p.Type == null || t.TaskType == p.Type) &&
                    (p.CropCycleId == null || t.CropCycleId == p.CropCycleId) &&
                    (p.AssignedToUserId == null || t.AssignedToUserId == p.AssignedToUserId) &&
                    (p.From == null || t.DueDate >= p.From) && (p.To == null || t.DueDate <= p.To) &&
                    (search == null || t.Description != null && t.Description.Contains(search)));
                return await Page(p.Sort == "asc" ? tasks.OrderBy(t => t.DueDate).ThenBy(t => t.Id) : tasks.OrderByDescending(t => t.DueDate).ThenByDescending(t => t.Id), p,
                    t => new FarmTaskDetails(t.Id, t.FarmId, t.CropCycleId, t.AssignedToUserId, t.TaskType, t.DueDate, t.Status, t.Description, t.CreatedAtUtc, t.ClosedAtUtc, Convert.ToBase64String(t.RowVersion)), ct);
            case "activities":
                var activities = db.CropActivities.AsNoTracking().Where(a => cycles.Any(c => c.Id == a.CropCycleId) &&
                    (p.CropCycleId == null || a.CropCycleId == p.CropCycleId) && (p.Type == null || a.ActivityType == p.Type) &&
                    (startUtc == null || a.OccurredAtUtc >= startUtc) && (until == null || a.OccurredAtUtc < until) &&
                    (search == null || a.Notes != null && a.Notes.Contains(search)));
                return await Page(p.Sort == "asc" ? activities.OrderBy(a => a.OccurredAtUtc).ThenBy(a => a.Id) : activities.OrderByDescending(a => a.OccurredAtUtc).ThenByDescending(a => a.Id), p,
                    a => new { a.CropCycleId, Record = new ActivityDetails(a.Id, a.ActivityType, a.OccurredAtUtc, a.RecordedAtUtc, a.RecordedByUserId, a.Notes, a.FarmTaskId) }, ct);
            case "observations":
                var observations = db.CropObservations.AsNoTracking().Where(o => cycles.Any(c => c.Id == o.CropCycleId) &&
                    (p.CropCycleId == null || o.CropCycleId == p.CropCycleId) && (p.Status == null || o.FollowUpStatus == p.Status) &&
                    (p.Type == null || o.HealthStatus == p.Type) && (startUtc == null || o.ObservedAtUtc >= startUtc) &&
                    (until == null || o.ObservedAtUtc < until) && (search == null || o.Notes != null && o.Notes.Contains(search)));
                var observationPage = await Page(p.Sort == "asc" ? observations.OrderBy(o => o.ObservedAtUtc).ThenBy(o => o.Id) : observations.OrderByDescending(o => o.ObservedAtUtc).ThenByDescending(o => o.Id), p, o => o, ct);
                var ids = observationPage.Items.Select(o => o.Id).ToList();
                var photos = await db.ObservationPhotos.AsNoTracking().Where(photo => ids.Contains(photo.CropObservationId))
                    .Select(photo => new PhotoDetails(photo.Id, photo.CropObservationId, photo.ContentType, photo.FileSizeBytes, photo.UploadedAtUtc)).ToListAsync(ct);
                return new PageResult<object>(observationPage.Items.Select(o => (object)new { o.CropCycleId,
                    Record = new ObservationDetails(o.Id, o.HealthStatus, o.GrowthStage, o.ObservedAtUtc, o.RecordedAtUtc, o.RecordedByUserId, o.Notes,
                        photos.Where(photo => photo.ObservationId == o.Id).ToList(), o.IssueCategory, o.IssueSeverity, o.FollowUpStatus, o.FollowUpTaskId, Convert.ToBase64String(o.RowVersion)) }).ToList(), p.Page, p.PageSize, observationPage.TotalCount);
            case "harvests":
                var harvests = db.Harvests.AsNoTracking().Where(h => cycles.Any(c => c.Id == h.CropCycleId) &&
                    (p.CropCycleId == null || h.CropCycleId == p.CropCycleId) && (p.Voided == null || (h.VoidedAtUtc != null) == p.Voided) &&
                    (startUtc == null || h.HarvestedAtUtc >= startUtc) && (until == null || h.HarvestedAtUtc < until) &&
                    (search == null || h.Notes != null && h.Notes.Contains(search)));
                return await Page(p.Sort == "asc" ? harvests.OrderBy(h => h.HarvestedAtUtc).ThenBy(h => h.Id) : harvests.OrderByDescending(h => h.HarvestedAtUtc).ThenByDescending(h => h.Id), p,
                    h => new HarvestEntry(h.Id, h.CropCycleId, h.Quantity, h.Unit, h.HarvestedAtUtc, h.RecordedAtUtc, h.RecordedByUserId, h.Notes, h.VoidedAtUtc != null, h.VoidedAtUtc, h.VoidedByUserId, h.VoidReason), ct);
            case "inventory":
                var items = db.InventoryItems.AsNoTracking().Where(i => i.FarmId == farmId && i.ArchivedAtUtc == null &&
                    (p.Type == null || i.Category == p.Type) && (search == null || i.Name.Contains(search)) &&
                    (p.LowStock == null || (i.ReorderLevel != null && i.QuantityOnHand <= i.ReorderLevel) == p.LowStock));
                return await Page(p.Sort == "asc" ? items.OrderBy(i => i.Name).ThenBy(i => i.Id) : items.OrderByDescending(i => i.Name).ThenByDescending(i => i.Id), p,
                    i => new InventoryItemDetails(i.Id, i.FarmId, i.Name, (InventoryCategory)i.Category, i.Unit, i.Description, i.QuantityOnHand, i.ReorderLevel,
                        i.ReorderLevel != null && i.QuantityOnHand <= i.ReorderLevel, i.ArchivedAtUtc, Convert.ToBase64String(i.RowVersion)), ct);
            case "movements":
                var movements = db.InventoryTransactions.AsNoTracking().Where(t => db.InventoryItems.Any(i => i.Id == t.InventoryItemId && i.FarmId == farmId) &&
                    (p.ItemId == null || t.InventoryItemId == p.ItemId) && (p.Type == null || t.TransactionType == p.Type) &&
                    (startUtc == null || t.OccurredAtUtc >= startUtc) && (until == null || t.OccurredAtUtc < until) &&
                    (search == null || t.Reason != null && t.Reason.Contains(search)));
                return await Page(p.Sort == "asc" ? movements.OrderBy(t => t.OccurredAtUtc).ThenBy(t => t.Id) : movements.OrderByDescending(t => t.OccurredAtUtc).ThenByDescending(t => t.Id), p,
                    t => new InventoryTransactionDetails(t.Id, t.InventoryItemId, t.CropActivityId, t.ReversesTransactionId, (StockTransactionType)t.TransactionType,
                        t.QuantityChange, t.OccurredAtUtc, t.RecordedAtUtc, t.RecordedByUserId, t.Reason), ct);
            case "members":
                var members = from m in db.FarmMembers.AsNoTracking() join u in db.Users.AsNoTracking() on m.UserId equals u.Id
                    where m.FarmId == farmId && (p.Status == null || (p.Status == 0 ? m.RemovedAtUtc == null : m.RemovedAtUtc != null)) &&
                        (p.Type == null || m.Role == p.Type) && (search == null || u.FullName.Contains(search) || u.Email!.Contains(search))
                    select new { m.Id, m.UserId, m.Role, m.JoinedAtUtc, m.RemovedAtUtc, u.FullName, u.Email };
                return await Page(p.Sort == "asc" ? members.OrderBy(m => m.JoinedAtUtc).ThenBy(m => m.Id) : members.OrderByDescending(m => m.JoinedAtUtc).ThenByDescending(m => m.Id), p, m => m, ct);
            case "enrollment":
                var requests = from r in db.FarmJoinRequests.AsNoTracking() join u in db.Users.AsNoTracking() on r.UserId equals u.Id
                    where r.FarmId == farmId && (p.EnrollmentStatus == null || r.Status == p.EnrollmentStatus) &&
                        (startUtc == null || r.RequestedAtUtc >= startUtc) && (until == null || r.RequestedAtUtc < until) &&
                        (search == null || u.FullName.Contains(search) || u.Email!.Contains(search))
                    select new { r.Id, r.UserId, r.Status, r.RequestedAtUtc, r.ReviewedAtUtc, u.FullName, u.Email };
                return await Page(p.Sort == "asc" ? requests.OrderBy(r => r.RequestedAtUtc).ThenBy(r => r.Id) : requests.OrderByDescending(r => r.RequestedAtUtc).ThenByDescending(r => r.Id), p, r => r, ct);
            default: throw new FarmOperationException(FarmError.NotFound, "Resource not found.");
        }
    }

    public async Task<PageResult<NotificationDetails>> NotificationsAsync(Guid actor, PageRequest p, CancellationToken ct = default)
    {
        try { p.Validate(); } catch (ArgumentException e) { throw new FarmOperationException(FarmError.InvalidInput, e.Message); }
        if (actor == Guid.Empty) throw new FarmOperationException(FarmError.NotFound, "Resource not found.");
        var q = NotificationVisibility.ForActor(db, actor).AsNoTracking().Where(n =>
            (p.Type == null || n.Type == p.Type) && (p.Unread == null || (n.ReadAtUtc == null) == p.Unread) &&
            (p.Search == null || n.Title.Contains(p.Search) || n.Message.Contains(p.Search)));
        return await Page(p.Sort == "asc" ? q.OrderBy(n => n.CreatedAtUtc).ThenBy(n => n.Id) : q.OrderByDescending(n => n.CreatedAtUtc).ThenByDescending(n => n.Id), p,
            n => new NotificationDetails(n.Id, n.Type, n.Title, n.Message, n.FarmTaskId, n.WeatherAdvisoryId, n.CreatedAtUtc, n.ReadAtUtc), ct);
    }

    private async Task Require(Guid farm, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farm, actor, action, ct)) throw new FarmOperationException(FarmError.NotFound, "Resource not found.");
    }
    private static async Task<PageResult<TResult>> Page<T, TResult>(IOrderedQueryable<T> query, PageRequest p, Func<T, TResult> map, CancellationToken ct)
    {
        var count = await query.CountAsync(ct);
        var rows = await query.Skip((p.Page - 1) * p.PageSize).Take(p.PageSize).ToListAsync(ct);
        return new(rows.Select(map).ToList(), p.Page, p.PageSize, count);
    }
}
