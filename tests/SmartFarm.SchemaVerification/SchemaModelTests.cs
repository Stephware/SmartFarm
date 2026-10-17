using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.EntityFrameworkCore.Migrations;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.SchemaVerification;

// Offline checks: no connection string, database calls, or DDL execution.
public sealed class SchemaModelTests
{
    [Fact]
    public void SqlFirstModelContainsOnlyExpectedTablesColumnsAndConstraints()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entities = context.GetService<IDesignTimeModel>().Model.GetEntityTypes().ToArray();
        Assert.Equal(23, entities.Length); // Includes seven Identity tables; excludes passkeys.
        Assert.Equal(193, entities.Sum(entity => entity.GetProperties().Count()));
        Assert.Equal(39, entities.Sum(entity => entity.GetForeignKeys().Count()));
        Assert.Equal(45, entities.Sum(entity => entity.GetIndexes().Count()));
        Assert.Equal(36, entities.Sum(entity => entity.GetCheckConstraints().Count()));
        Assert.Equal(28, entities.SelectMany(entity => entity.GetProperties()).Count(property =>
            property.GetDefaultValueSql() is not null || property.TryGetDefaultValue(out _)));
        Assert.All(entities, entity => Assert.Equal("dbo", entity.GetSchema()));
    }

    [Fact]
    public void GuidKeysAreClientAssignedAndSqlRowVersionsProvideConcurrency()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entities = context.Model.GetEntityTypes().ToArray();
        var guidKeys = entities.SelectMany(entity => entity.FindPrimaryKey()!.Properties)
            .Where(property => property.ClrType == typeof(Guid));
        Assert.All(guidKeys, property => Assert.Equal(ValueGenerated.Never, property.ValueGenerated));
        var rowVersions = entities.SelectMany(entity => entity.GetProperties())
            .Where(property => property.Name == "RowVersion").ToArray();
        Assert.Equal(5, rowVersions.Length);
        Assert.All(rowVersions, property =>
        {
            Assert.True(property.IsConcurrencyToken);
            Assert.Equal(ValueGenerated.OnAddOrUpdate, property.ValueGenerated);
        });
        Assert.False(context.Model.FindEntityType(typeof(InventoryItem))!
            .FindProperty(nameof(InventoryItem.MetadataVersion))!.IsConcurrencyToken);
        Assert.NotEqual(Guid.Empty, new Farm().Id);
        Assert.NotEqual(Guid.Empty, new SyncOperation().OperationId);
    }

    [Fact]
    public void ExplicitFalseCropTypeValueDoesNotUseTrueDatabaseDefault()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var inactiveCrop = new CropType { IsActive = false };
        context.Add(inactiveCrop);
        var entry = context.Entry(inactiveCrop).Property(crop => crop.IsActive);
        Assert.False(entry.IsTemporary);
        Assert.Equal(true, entry.Metadata.Sentinel);
        Assert.NotEqual(entry.CurrentValue, entry.Metadata.Sentinel);
        Assert.True(new CropType().IsActive);
        Assert.Equal("Asia/Manila", new Farm().TimeZoneId);
    }

    [Fact]
    public void AdoptionBaselineContainsNoDdlAndSnapshotHasNoDrift()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        Assert.False(context.Database.HasPendingModelChanges());
        var assembly = context.GetService<IMigrationsAssembly>();
        var migrationType = Assert.Single(assembly.Migrations).Value;
        var migration = assembly.CreateMigration(migrationType, context.Database.ProviderName!);
        Assert.Empty(migration.UpOperations);
        Assert.Empty(migration.DownOperations);
    }
}
