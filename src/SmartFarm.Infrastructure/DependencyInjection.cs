using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Repositories;
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

    public static IServiceCollection AddSmartFarmRepositories(this IServiceCollection services)
    {
        services.AddScoped<IFarmRepository, FarmRepository>();
        services.AddScoped<IUserRepository, UserRepository>();
        services.AddScoped<IPlotRepository, PlotRepository>();
        services.AddScoped<IFarmMemberRepository, FarmMemberRepository>();
        services.AddScoped<ICropTypeRepository, CropTypeRepository>();
        services.AddScoped<ICropCycleRepository, CropCycleRepository>();
        services.AddScoped<ICropActivityRepository, CropActivityRepository>();
        services.AddScoped<ICropObservationRepository, CropObservationRepository>();
        services.AddScoped<IObservationPhotoRepository, ObservationPhotoRepository>();
        services.AddScoped<IInventoryItemRepository, InventoryItemRepository>();
        services.AddScoped<IInventoryTransactionRepository, InventoryTransactionRepository>();
        services.AddScoped<IHarvestRepository, HarvestRepository>();
        services.AddScoped<IUnitOfWork, EfUnitOfWork>();
        return services;
    }
}
