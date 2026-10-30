using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Conventions;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence;

public sealed class SmartFarmDbContext(DbContextOptions<SmartFarmDbContext> options)
    : IdentityDbContext<SmartFarmUser, SmartFarmRole, Guid>(options)
{
    public DbSet<Farm> Farms => Set<Farm>();
    public DbSet<FarmFinanceEntry> FarmFinanceEntries => Set<FarmFinanceEntry>();
    public DbSet<FarmJoinRequest> FarmJoinRequests => Set<FarmJoinRequest>();
    public DbSet<FarmMember> FarmMembers => Set<FarmMember>();
    public DbSet<Plot> Plots => Set<Plot>();
    public DbSet<CropType> CropTypes => Set<CropType>();
    public DbSet<CropCycle> CropCycles => Set<CropCycle>();
    public DbSet<FarmTask> FarmTasks => Set<FarmTask>();
    public DbSet<CropActivity> CropActivities => Set<CropActivity>();
    public DbSet<CropObservation> CropObservations => Set<CropObservation>();
    public DbSet<ObservationPhoto> ObservationPhotos => Set<ObservationPhoto>();
    public DbSet<InventoryItem> InventoryItems => Set<InventoryItem>();
    public DbSet<InventoryTransaction> InventoryTransactions => Set<InventoryTransaction>();
    public DbSet<Harvest> Harvests => Set<Harvest>();
    public DbSet<WeatherForecast> WeatherForecasts => Set<WeatherForecast>();
    public DbSet<WeatherAdvisory> WeatherAdvisories => Set<WeatherAdvisory>();
    public DbSet<Notification> Notifications => Set<Notification>();
    public DbSet<SyncOperation> SyncOperations => Set<SyncOperation>();

    // The existing seven-table Identity schema predates passkeys (schema v3).
    protected override Version SchemaVersion => IdentitySchemaVersions.Version1;

    protected override void ConfigureConventions(ModelConfigurationBuilder configurationBuilder)
    {
        base.ConfigureConventions(configurationBuilder);
        // SQL-first schema defines exactly which FK indexes exist.
        configurationBuilder.Conventions.Remove(typeof(ForeignKeyIndexConvention));
    }

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);
        builder.HasDefaultSchema("dbo");
        builder.ApplyConfigurationsFromAssembly(typeof(SmartFarmDbContext).Assembly);
    }
}
