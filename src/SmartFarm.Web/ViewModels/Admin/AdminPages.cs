using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Microsoft.EntityFrameworkCore;

namespace SmartFarm.Web.ViewModels.Admin;

public sealed record AdminFarmRow(
    Guid Id, string Name, string Location, string OwnerName, string OwnerEmail,
    bool Archived, int Members, int Plots, DateTime CreatedAtUtc, string RowVersion);

public sealed record AdminUserRow(
    Guid Id, string Name, string Email, bool Disabled, bool IsSuperAdmin,
    int OwnedFarms, int ActiveMemberships);

public sealed record AdminOverviewPage(
    int TotalUsers, int ActiveUsers, int DisabledUsers,
    int TotalFarms, int ActiveFarms, int ArchivedFarms, int ActivePlots,
    IReadOnlyList<AdminFarmRow> RecentFarms);

public sealed record AdminUserIndexPage(
    string Search, string Status, int Page, int TotalCount, IReadOnlyList<AdminUserRow> Users)
{
    public const int PageSize = 20;
    public bool HasPrevious => Page > 1;
    public bool HasNext => Page * PageSize < TotalCount;
}

public sealed record AdminUserDetailPage(AdminUserRow User, IReadOnlyList<AdminFarmRow> OwnedFarms);

public sealed record AdminFarmIndexPage(
    string Search, string Status, int Page, int TotalCount, IReadOnlyList<AdminFarmRow> Farms)
{
    public const int PageSize = 20;
    public bool HasPrevious => Page > 1;
    public bool HasNext => Page * PageSize < TotalCount;
}

public sealed record AdminFarmDetailPage(AdminFarmRow Farm, int ActiveCropCycles, int PositiveStockItems);

// Batch the directory's related counts to avoid querying the database per farm card.
// Farm join codes, sensitive location coordinates and private records are deliberately omitted.
public static class AdminFarmDirectory
{
    public static async Task<IReadOnlyList<AdminFarmRow>> LoadAsync(
        SmartFarmDbContext db, IReadOnlyList<Farm> farms, CancellationToken ct = default)
    {
        if (farms.Count == 0) return [];
        var farmIds = farms.Select(f => f.Id).ToArray();
        var ownerIds = farms.Select(f => f.OwnerUserId).Distinct().ToArray();
        var owners = await db.Users.AsNoTracking()
            .Where(u => ownerIds.Contains(u.Id))
            .Select(u => new { u.Id, u.FullName, u.Email })
            .ToDictionaryAsync(u => u.Id, ct);
        var members = await db.FarmMembers.AsNoTracking()
            .Where(m => farmIds.Contains(m.FarmId) && m.RemovedAtUtc == null)
            .GroupBy(m => m.FarmId)
            .Select(g => new { Id = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Id, g => g.Count, ct);
        var plots = await db.Plots.AsNoTracking()
            .Where(p => farmIds.Contains(p.FarmId) && p.ArchivedAtUtc == null)
            .GroupBy(p => p.FarmId)
            .Select(g => new { Id = g.Key, Count = g.Count() })
            .ToDictionaryAsync(g => g.Id, g => g.Count, ct);
        return farms.Select(f =>
        {
            owners.TryGetValue(f.OwnerUserId, out var owner);
            return new AdminFarmRow(f.Id, f.Name, f.LocationText,
                owner?.FullName ?? "Unknown owner", owner?.Email ?? "",
                f.ArchivedAtUtc.HasValue, members.GetValueOrDefault(f.Id),
                plots.GetValueOrDefault(f.Id), f.CreatedAtUtc,
                Convert.ToBase64String(f.RowVersion));
        }).ToList();
    }
}
