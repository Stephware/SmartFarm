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
}
