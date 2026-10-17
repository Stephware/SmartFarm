using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Diagnostics.HealthChecks;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Web.HealthChecks;

public sealed class DatabaseConnectivityHealthCheck(IServiceScopeFactory scopeFactory) : IHealthCheck
{
    public async Task<HealthCheckResult> CheckHealthAsync(
        HealthCheckContext context,
        CancellationToken cancellationToken = default)
    {
        try
        {
            await using var scope = scopeFactory.CreateAsyncScope();
            var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();

            if (string.IsNullOrWhiteSpace(db.Database.GetConnectionString()))
            {
                return HealthCheckResult.Unhealthy("Database connection is not configured.");
            }

            // CanConnectAsync only opens the existing database; it does not validate
            // the schema, create a database, or apply migrations.
            return await db.Database.CanConnectAsync(cancellationToken)
                ? HealthCheckResult.Healthy("Database connection succeeded.")
                : HealthCheckResult.Unhealthy("Database connection failed.");
        }
        catch (Exception)
        {
            // Do not expose connection strings or provider exception details.
            return HealthCheckResult.Unhealthy("Database connection failed.");
        }
    }
}
