using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Admin;

namespace SmartFarm.Web.Areas.Admin.Controllers;

// Audit history is strictly read-only and available only after Super Admin MFA.
[Area("Admin")]
[Route("admin/audit")]
[Authorize(Policy = SuperAdminSecurity.Policy)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class AuditController(SmartFarmDbContext db) : Controller
{
    [HttpGet("")]
    public async Task<IActionResult> Index(
        string? search, string? action, string? targetType, Guid? targetId,
        DateOnly? from, DateOnly? to, int page = 1, CancellationToken ct = default)
    {
        var term = (search ?? "").Trim();
        if (term.Length > 100) term = term[..100];
        var selectedAction = AdminAuditDisplay.IsKnownAction(action) ? action! : "all";
        var selectedType = AdminAuditDisplay.TargetTypes.Contains(targetType)
            ? targetType! : "all";
        page = Math.Clamp(page, 1, 100000);

        if (from > to)
            return View(new AdminAuditListPage(term, selectedAction, selectedType,
                targetId, from, to, page, 0, 0, [],
                "Start date must be on or before end date."));

        var events = db.AdminAuditLogs.AsNoTracking().AsQueryable();
        if (selectedAction != "all") events = events.Where(e => e.Action == selectedAction);
        if (selectedType != "all") events = events.Where(e => e.TargetType == selectedType);
        if (targetId.HasValue) events = events.Where(e => e.TargetId == targetId.Value);
        if (from.HasValue)
        {
            var start = from.Value.ToDateTime(TimeOnly.MinValue);
            events = events.Where(e => e.OccurredAtUtc >= start);
        }
        if (to.HasValue && to.Value < DateOnly.MaxValue)
        {
            var endExclusive = to.Value.AddDays(1).ToDateTime(TimeOnly.MinValue);
            events = events.Where(e => e.OccurredAtUtc < endExclusive);
        }
        if (term.Length > 0)
        {
            // Search actor names/emails, action names, or exact target/actor GUID.
            // Never search arbitrary audit reasons (which might contain private context).
            var isGuid = Guid.TryParse(term, out var guid);
            events = events.Where(e =>
                e.Action.Contains(term) ||
                db.Users.Any(u => e.ActorUserId == u.Id &&
                    (u.FullName.Contains(term) || (u.Email != null && u.Email.Contains(term)))) ||
                (isGuid && (e.TargetId == guid || e.ActorUserId == guid)));
        }

        var total = await events.CountAsync(ct);
        var rows = await events
            .OrderByDescending(e => e.OccurredAtUtc).ThenByDescending(e => e.Id)
            .Skip((page - 1) * AdminAuditListPage.PageSize)
            .Take(AdminAuditListPage.PageSize)
            .Select(e => new AdminAuditRow(e.Id, e.OccurredAtUtc,
                e.ActorUserId == null ? "Server operator" :
                    db.Users.Where(u => u.Id == e.ActorUserId)
                        .Select(u => u.FullName).FirstOrDefault() ?? "Unknown account",
                e.ActorUserId == null ? null :
                    db.Users.Where(u => u.Id == e.ActorUserId)
                        .Select(u => u.Email).FirstOrDefault(),
                e.ActorSource, e.Action, e.TargetType, e.TargetId))
            .ToListAsync(ct);

        var recent = await db.AdminAuditLogs.AsNoTracking().CountAsync(
            e => e.OccurredAtUtc >= DateTime.UtcNow.AddDays(-7), ct);
        return View(new AdminAuditListPage(term, selectedAction, selectedType,
            targetId, from, to, page, total, recent, rows));
    }

    [HttpGet("{id:guid}")]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        var entry = await db.AdminAuditLogs.AsNoTracking()
            .SingleOrDefaultAsync(e => e.Id == id, ct);
        if (entry is null) return NotFound();
        var actor = entry.ActorUserId is Guid actorId
            ? await db.Users.AsNoTracking().Where(u => u.Id == actorId)
                .Select(u => new { u.FullName, u.Email }).FirstOrDefaultAsync(ct)
            : null;
        return View(new AdminAuditDetailPage(
            new AdminAuditRow(entry.Id, entry.OccurredAtUtc,
                actor?.FullName ?? (entry.ActorUserId is null ? "Server operator" : "Unknown account"),
                actor?.Email, entry.ActorSource, entry.Action,
                entry.TargetType, entry.TargetId),
            entry.Reason));
    }
}
