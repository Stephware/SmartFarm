using System.Data;

namespace SmartFarm.Core.Interfaces.Persistence;

// The service coordinates when changes are committed; repositories stage them.
public interface IUnitOfWork
{
    Task<int> SaveChangesAsync(CancellationToken ct = default);
    Task<IUnitOfWorkTransaction> BeginTransactionAsync(IsolationLevel isolationLevel,
        CancellationToken ct = default);
}

public interface IUnitOfWorkTransaction : IAsyncDisposable
{
    Task CommitAsync(CancellationToken ct = default);
}
