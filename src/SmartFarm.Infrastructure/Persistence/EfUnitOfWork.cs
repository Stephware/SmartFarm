using System.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Storage;
using SmartFarm.Core.Interfaces.Persistence;

namespace SmartFarm.Infrastructure.Persistence;

public sealed class EfUnitOfWork(SmartFarmDbContext db) : IUnitOfWork
{
    public bool HasActiveTransaction => db.Database.CurrentTransaction is not null;
    public Task<int> SaveChangesAsync(CancellationToken ct = default) =>
        db.SaveChangesAsync(ct);

    public async Task<IUnitOfWorkTransaction> BeginTransactionAsync(
        IsolationLevel isolationLevel, CancellationToken ct = default) =>
        new EfTransaction(await db.Database.BeginTransactionAsync(isolationLevel, ct));

    private sealed class EfTransaction(IDbContextTransaction transaction) : IUnitOfWorkTransaction
    {
        public Task CommitAsync(CancellationToken ct = default) =>
            transaction.CommitAsync(ct);

        // Disposing an uncommitted EF transaction rolls it back.
        public ValueTask DisposeAsync() => transaction.DisposeAsync();
    }
}
