using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class FarmEnrollmentRepository(SmartFarmDbContext db) : IFarmEnrollmentRepository
{
    public Task<Farm?> FindFarmByCodeForUpdateAsync(string code, CancellationToken ct) =>
        db.Farms.FromSqlInterpolated($"SELECT * FROM dbo.Farm WITH (UPDLOCK, HOLDLOCK) WHERE JoinCode = {code} AND ArchivedAtUtc IS NULL")
            .SingleOrDefaultAsync(ct);

    public Task<Farm?> FindFarmForUpdateAsync(Guid farmId, CancellationToken ct) =>
        db.Farms.FromSqlInterpolated($"SELECT * FROM dbo.Farm WITH (UPDLOCK, HOLDLOCK) WHERE Id = {farmId} AND ArchivedAtUtc IS NULL")
            .SingleOrDefaultAsync(ct);

    public Task<bool> IsActiveUserAsync(Guid userId, CancellationToken ct) =>
        db.Users.FromSqlInterpolated($"SELECT * FROM dbo.AspNetUsers WITH (HOLDLOCK) WHERE Id = {userId} AND DisabledAtUtc IS NULL")
            .AnyAsync(ct);

    public Task<FarmJoinRequest?> FindPendingAsync(Guid farmId, Guid userId, CancellationToken ct) =>
        db.FarmJoinRequests.SingleOrDefaultAsync(r => r.FarmId == farmId && r.UserId == userId && r.Status == "Pending", ct);

    public Task<FarmJoinRequest?> FindAsync(Guid farmId, Guid requestId, CancellationToken ct) =>
        db.FarmJoinRequests.SingleOrDefaultAsync(r => r.FarmId == farmId && r.Id == requestId, ct);

    private IQueryable<FarmJoinRequestDetails> Details(IQueryable<FarmJoinRequest> query) =>
        from request in query.AsNoTracking()
        join farm in db.Farms on request.FarmId equals farm.Id
        join user in db.Users on request.UserId equals user.Id
        select new FarmJoinRequestDetails(request.Id, farm.Id, farm.Name, user.Id, user.FullName,
            user.Email ?? "", request.Status, request.RequestedAtUtc, request.ReviewedAtUtc);

    public Task<FarmJoinRequestDetails> DetailsAsync(Guid requestId, CancellationToken ct) =>
        Details(db.FarmJoinRequests.Where(r => r.Id == requestId)).SingleAsync(ct);

    public async Task<IReadOnlyList<FarmJoinRequestDetails>> ListForUserAsync(Guid userId, CancellationToken ct) =>
        await Details(db.FarmJoinRequests.Where(r => r.UserId == userId).OrderByDescending(r => r.RequestedAtUtc).ThenBy(r => r.Id)).ToListAsync(ct);

    public async Task<IReadOnlyList<FarmJoinRequestDetails>> ListPendingAsync(Guid farmId, CancellationToken ct) =>
        await Details(db.FarmJoinRequests.Where(r => r.FarmId == farmId && r.Status == "Pending").OrderBy(r => r.RequestedAtUtc).ThenBy(r => r.Id)).ToListAsync(ct);

    public void Add(FarmJoinRequest request) => db.FarmJoinRequests.Add(request);
}
