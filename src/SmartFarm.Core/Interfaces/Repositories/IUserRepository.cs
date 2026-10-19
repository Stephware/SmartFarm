namespace SmartFarm.Core.Interfaces.Repositories;

public interface IUserRepository
{
    Task<bool> IsActiveAsync(Guid userId, CancellationToken ct = default);
}
