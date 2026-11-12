using System.ComponentModel.DataAnnotations;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Web.ViewModels.Support;

public static class SupportRules
{
    public static readonly string[] Categories = ["Account", "Farm", "Mobile", "Other"];
    public static readonly string[] Statuses = ["Open", "InProgress", "Resolved", "Closed"];
    public static readonly string[] Priorities = ["Normal", "High"];

    public static bool ValidMessage(string? body) => body?.Trim().Length is >= 2 and <= 1200;

    // Only the owner or a current member can connect a request to a farm.
    public static Task<bool> CanLinkFarm(SmartFarmDbContext db, Guid actor, Guid farmId,
        CancellationToken ct = default) =>
        db.Farms.AsNoTracking().AnyAsync(f => f.Id == farmId && f.ArchivedAtUtc == null &&
            (f.OwnerUserId == actor || db.FarmMembers.Any(m =>
                m.FarmId == farmId && m.UserId == actor && m.RemovedAtUtc == null)), ct);
}

public sealed class SupportCreateInput
{
    [Required, StringLength(140, MinimumLength = 5)]
    public string Title { get; set; } = "";

    [Required, StringLength(2000, MinimumLength = 15)]
    public string Description { get; set; } = "";

    [Required, RegularExpression("Account|Farm|Mobile|Other")]
    public string Category { get; set; } = "Other";

    public Guid? FarmId { get; set; }
}

public sealed record SupportFarmChoice(Guid Id, string Name);
public sealed record SupportCreatePage(SupportCreateInput Input, IReadOnlyList<SupportFarmChoice> Farms);

public sealed record SupportRow(Guid Id, Guid UserId, string Title,
    string Category, string Status, string Priority,
    string UserName, string UserEmail, string? FarmName,
    DateTime CreatedAtUtc, DateTime UpdatedAtUtc, string RowVersion);

public sealed record SupportMessageRow(Guid Id, string AuthorName, string Body,
    bool IsInternal, DateTime CreatedAtUtc);

public sealed record SupportDetailsPage(SupportRow Ticket,
    string Description, IReadOnlyList<SupportMessageRow> Messages);

public sealed record SupportListPage(IReadOnlyList<SupportRow> Tickets);

public sealed record AdminSupportListPage(string Search, string Status, string Priority,
    int Page, int Total, int OpenCount, int InProgressCount,
    int ResolvedCount, IReadOnlyList<SupportRow> Tickets)
{
    public const int PageSize = 20;
    public bool HasPrevious => Page > 1;
    public bool HasNext => Page * PageSize < Total;
}

public static class SupportPageBuilder
{
    public static async Task<IReadOnlyList<SupportRow>> ToRowsAsync(
        SmartFarmDbContext db, IReadOnlyList<SupportTicket> tickets, CancellationToken ct)
    {
        if (tickets.Count == 0) return [];
        var userIds = tickets.Select(t => t.UserId).Distinct().ToArray();
        var farmIds = tickets.Where(t => t.FarmId.HasValue).Select(t => t.FarmId!.Value).Distinct().ToArray();
        var people = await db.Users.AsNoTracking().Where(u => userIds.Contains(u.Id))
            .Select(u => new { u.Id, u.FullName, u.Email }).ToDictionaryAsync(u => u.Id, ct);
        var farms = await db.Farms.AsNoTracking().Where(f => farmIds.Contains(f.Id))
            .Select(f => new { f.Id, f.Name }).ToDictionaryAsync(f => f.Id, ct);
        return tickets.Select(t =>
        {
            people.TryGetValue(t.UserId, out var person);
            var name = t.FarmId is Guid farmId && farms.TryGetValue(farmId, out var farm)
                ? farm.Name : null;
            return new SupportRow(t.Id, t.UserId, t.Title, t.Category, t.Status, t.Priority,
                person?.FullName ?? "Unknown user", person?.Email ?? "",
                name, t.CreatedAtUtc, t.UpdatedAtUtc, Convert.ToBase64String(t.RowVersion));
        }).ToList();
    }

    public static async Task<IReadOnlyList<SupportMessageRow>> MessagesAsync(
        SmartFarmDbContext db, Guid ticketId, bool includeInternal, CancellationToken ct)
    {
        var query = db.SupportTicketMessages.AsNoTracking().Where(m => m.TicketId == ticketId);
        if (!includeInternal) query = query.Where(m => !m.IsInternal);
        return await (from message in query
            join author in db.Users.AsNoTracking() on message.AuthorUserId equals author.Id
            orderby message.CreatedAtUtc, message.Id
            select new SupportMessageRow(message.Id, author.FullName,
                message.Body, message.IsInternal, message.CreatedAtUtc)).ToListAsync(ct);
    }
}
