using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Services.Crops;
using SmartFarm.Infrastructure.Services.Farms;
using SmartFarm.Infrastructure.Services.Harvests;
using SmartFarm.Infrastructure.Services.Inventory;
using SmartFarm.Infrastructure.Services.Monitoring;
using SmartFarm.Infrastructure.Services.Reports;
using SmartFarm.Infrastructure.Services.Tasks;
using SmartFarm.Infrastructure.Services.Weather;
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
        services.AddScoped<IFarmEnrollmentRepository, FarmEnrollmentRepository>();
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
        services.AddScoped<IFarmTaskRepository, FarmTaskRepository>();
        services.AddScoped<IUnitOfWork, EfUnitOfWork>();
        return services;
    }

    // MVC and Web API use the same service implementations, without host-to-host HTTP calls.
    public static IServiceCollection AddSmartFarmBusinessServices(this IServiceCollection services)
    {
        services.AddSmartFarmRepositories();
        services.AddScoped<IFarmAccessService, FarmAccessService>();
        services.AddScoped<IFarmManagementService, FarmManagementService>();
        services.AddScoped<IFarmEnrollmentService, FarmEnrollmentService>();
        services.AddScoped<ICropManagementService, CropManagementService>();
        services.AddScoped<IMonitoringService, MonitoringService>();
        services.AddScoped<IInventoryService, InventoryService>();
        services.AddScoped<IHarvestService, HarvestService>();
        services.AddScoped<IFarmReportService, FarmReportService>();
        services.AddScoped<IFarmTaskService, FarmTaskService>();
        services.AddScoped<IFarmWeatherService, FarmWeatherService>();
        return services;
    }
}
