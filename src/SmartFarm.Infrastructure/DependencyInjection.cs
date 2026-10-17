using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddSmartFarmPersistence(
        this IServiceCollection services,
        string? connectionString)
    {
        // Registration does not open a connection or change the existing schema.
        services.AddDbContext<SmartFarmDbContext>(options =>
            options.UseSqlServer(connectionString));

        return services;
    }
}
