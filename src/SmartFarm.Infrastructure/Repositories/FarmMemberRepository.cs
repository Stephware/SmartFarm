using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class FarmMemberRepository(SmartFarmDbContext db) : IFarmMemberRepository
{
public Task<FarmMember?> GetActiveAsync(Guid farmId, Guid userId, CancellationToken ct = default) =>
        db.FarmMembers.SingleOrDefaultAsync(m =>
            m.FarmId == farmId && m.UserId == userId && m.RemovedAtUtc == null, ct);

    public async Task<IReadOnlyList<FarmMember>> ListActiveAsync(Guid farmId, CancellationToken ct = default) =>
        await db.FarmMembers.AsNoTracking()
            .Where(m => m.FarmId == farmId && m.RemovedAtUtc == null)
            .OrderBy(m => m.JoinedAtUtc).ToListAsync(ct);

    public void Add(FarmMember member) => db.FarmMembers.Add(member);
    public async Task<IReadOnlyList<SmartFarm.Core.Contracts.Farms.FarmMemberDetails>> ListDetailsAsync(Guid farmId,
        CancellationToken ct = default) =>
        await db.FarmMembers.AsNoTracking().Where(m => m.FarmId == farmId && m.RemovedAtUtc == null)
            .Join(db.Users, m => m.UserId, u => u.Id, (m, u) => new { Member = m, User = u })
            .OrderBy(row => row.User.FullName)
            .Select(row => new SmartFarm.Core.Contracts.Farms.FarmMemberDetails(
                row.Member.Id, row.Member.UserId, row.User.FullName, row.User.Email ?? "",
                row.Member.Role == 1 ? "Manager" : "FieldWorker")).ToListAsync(ct);

    public async Task<IReadOnlyList<FarmMember>> ListForUserAsync(Guid farmId, Guid userId,
        CancellationToken ct = default) =>
        await db.FarmMembers.Where(m => m.FarmId == farmId && m.UserId == userId).ToListAsync(ct);

    public Task<FarmMember?> GetByMemberIdAsync(Guid farmId, Guid memberId, CancellationToken ct = default) =>
        db.FarmMembers.SingleOrDefaultAsync(m => m.Id == memberId && m.FarmId == farmId && m.RemovedAtUtc == null, ct);

    public Task<byte?> GetActiveRoleAsync(Guid farmId, Guid userId, CancellationToken ct = default) =>
        db.FarmMembers.AsNoTracking().Where(m => m.FarmId == farmId && m.UserId == userId && m.RemovedAtUtc == null)
            .Select(m => (byte?)m.Role).SingleOrDefaultAsync(ct);

}
