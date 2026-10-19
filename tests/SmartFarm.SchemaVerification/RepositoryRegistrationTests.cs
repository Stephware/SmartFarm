using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Services.Farms;
using SmartFarm.Infrastructure.Services.Crops;
using SmartFarm.Infrastructure.Services.Monitoring;
using SmartFarm.Infrastructure.Services.Inventory;
using SmartFarm.Infrastructure.Services.Harvests;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Repositories;
using Xunit;

namespace SmartFarm.SchemaVerification;

public sealed class RepositoryRegistrationTests
{
    // These tests create a SQL Server EF model but never connect to SQL Server.
    private static ServiceProvider Services()
    {
        var services = new ServiceCollection();
        services.AddSmartFarmPersistence(
            "Server=localhost;Database=SmartFarmDB;Integrated Security=True;TrustServerCertificate=True;");
        services.AddSmartFarmRepositories();
        return services.BuildServiceProvider(validateScopes: true);
    }

    [Fact]
    public void EveryExplicitRepositoryAndUnitOfWorkIsRegistered()
    {
        using var provider = Services();
        using var scope = provider.CreateScope();
        var services = scope.ServiceProvider;

        Assert.IsType<FarmRepository>(services.GetRequiredService<IFarmRepository>());
        Assert.IsType<UserRepository>(services.GetRequiredService<IUserRepository>());
        Assert.IsType<PlotRepository>(services.GetRequiredService<IPlotRepository>());
        Assert.IsType<FarmMemberRepository>(services.GetRequiredService<IFarmMemberRepository>());
        Assert.IsType<CropTypeRepository>(services.GetRequiredService<ICropTypeRepository>());
        Assert.IsType<CropCycleRepository>(services.GetRequiredService<ICropCycleRepository>());
        Assert.IsType<CropActivityRepository>(services.GetRequiredService<ICropActivityRepository>());
        Assert.IsType<CropObservationRepository>(services.GetRequiredService<ICropObservationRepository>());
        Assert.IsType<ObservationPhotoRepository>(services.GetRequiredService<IObservationPhotoRepository>());
        Assert.IsType<InventoryItemRepository>(services.GetRequiredService<IInventoryItemRepository>());
        Assert.IsType<InventoryTransactionRepository>(services.GetRequiredService<IInventoryTransactionRepository>());
        Assert.IsType<HarvestRepository>(services.GetRequiredService<IHarvestRepository>());
        Assert.IsType<EfUnitOfWork>(services.GetRequiredService<IUnitOfWork>());
    }

    [Fact]
    public void RepositoryStagesChangesWithoutSavingOrConnecting()
    {
        using var provider = Services();
        using var scope = provider.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();
        var repository = scope.ServiceProvider.GetRequiredService<IFarmRepository>();
        var farm = new Farm { Name = "Test Farm" };

        repository.Add(farm);

        Assert.Equal(EntityState.Added, db.Entry(farm).State);
        Assert.Same(repository, scope.ServiceProvider.GetRequiredService<IFarmRepository>());
    }

    [Fact]
    public void DifferentScopesReceiveIndependentRepositoriesAndDbContexts()
    {
        using var provider = Services();
        using var first = provider.CreateScope();
        using var second = provider.CreateScope();

        Assert.NotSame(first.ServiceProvider.GetRequiredService<IFarmRepository>(),
            second.ServiceProvider.GetRequiredService<IFarmRepository>());
        Assert.NotSame(first.ServiceProvider.GetRequiredService<SmartFarmDbContext>(),
            second.ServiceProvider.GetRequiredService<SmartFarmDbContext>());
    }
    [Fact]
    public void ServicesDependOnRepositoryContractsInsteadOfDbContext()
    {
        Type[] serviceTypes =
        [
            typeof(FarmManagementService),
            typeof(CropManagementService),
            typeof(MonitoringService),
            typeof(InventoryService),
            typeof(HarvestService),
            typeof(FarmAccessService)
        ];
        foreach (var serviceType in serviceTypes)
        {
            var constructor = Assert.Single(serviceType.GetConstructors());
            Assert.DoesNotContain(constructor.GetParameters(), parameter =>
                parameter.ParameterType == typeof(SmartFarmDbContext));

            Assert.Contains(constructor.GetParameters(), parameter =>
                parameter.ParameterType.IsInterface &&
                parameter.ParameterType.Namespace == typeof(IFarmRepository).Namespace);
        }
    }

}
