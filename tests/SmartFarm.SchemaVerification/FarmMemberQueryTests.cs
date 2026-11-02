using System.Data.Common;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Repositories;
using Xunit;

namespace SmartFarm.SchemaVerification;

public sealed class FarmMemberQueryTests
{
    [Fact]
    public async Task MemberDetailsQueryTranslatesToSqlBeforeConnecting()
    {
        var options = new DbContextOptionsBuilder<SmartFarmDbContext>()
            .UseSqlServer("Server=localhost;Database=SmartFarmDB;Integrated Security=True;")
            .AddInterceptors(new StopBeforeConnecting())
            .Options;
        await using var db = new SmartFarmDbContext(options);

        // A translation error fails before this sentinel; no database is needed.
        await Assert.ThrowsAsync<ConnectionBlockedException>(() =>
            new FarmMemberRepository(db).ListDetailsAsync(Guid.NewGuid()));
    }

    private sealed class ConnectionBlockedException : Exception;

    private sealed class StopBeforeConnecting : DbConnectionInterceptor
    {
        public override ValueTask<InterceptionResult> ConnectionOpeningAsync(
            DbConnection connection, ConnectionEventData eventData, InterceptionResult result,
            CancellationToken cancellationToken = default) => throw new ConnectionBlockedException();
    }
}
