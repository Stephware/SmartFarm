using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Repositories;

public sealed class UserRepository(SmartFarmDbContext db) : IUserRepository
{
    public Task<bool> IsActiveAsync(Guid userId, CancellationToken ct = default) =>
        db.Users.AsNoTracking().AnyAsync(u => u.Id == userId && u.DisabledAtUtc == null, ct);
}
