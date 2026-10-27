using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.EntityFrameworkCore.Migrations;
using Microsoft.EntityFrameworkCore.Migrations.Operations;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.SchemaVerification;

// Offline checks: no connection string, database calls, or DDL execution.
public sealed class SchemaModelTests
{
    [Fact]
    public void EnrollmentPersistsUniqueCodesAndPendingRequestsWithDecisionIntegrity()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var model = context.GetService<IDesignTimeModel>().Model;
        var farm = model.FindEntityType(typeof(Farm))!;
        var code = farm.FindProperty("JoinCode");
        Assert.NotNull(code);
        Assert.False(code.IsNullable);
        Assert.Equal(12, code.GetMaxLength());
        Assert.Contains(farm.GetIndexes(), index => index.IsUnique && index.Properties.Count == 1 && index.Properties[0].Name == "JoinCode");
        var request = model.GetEntityTypes().Single(entity => entity.GetTableName() == "FarmJoinRequest");
        Assert.Equal(7, request.GetProperties().Count());
        Assert.Equal(3, request.GetForeignKeys().Count());
        Assert.All(request.GetForeignKeys(), key => Assert.Equal(DeleteBehavior.NoAction, key.DeleteBehavior));
        var pending = request.GetIndexes().Single(index => index.IsUnique);
        Assert.Equal(new[] { "FarmId", "UserId" }, pending.Properties.Select(property => property.Name));
        Assert.Equal("Status = N'Pending'", pending.GetFilter());
        Assert.Contains(request.GetCheckConstraints(), constraint => constraint.Name == "CK_FarmJoinRequest_Status");
        Assert.Contains(request.GetCheckConstraints(), constraint => constraint.Name == "CK_FarmJoinRequest_Review");
        var codes = Enumerable.Range(0, 100).Select(_ => new Farm().JoinCode).ToArray();
        Assert.All(codes, value => Assert.Matches("^[0-9A-F]{12}$", value));
        Assert.Equal(codes.Length, codes.Distinct().Count());
    }

    [Fact]
    public void SqlFirstModelContainsOnlyExpectedTablesColumnsAndConstraints()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entities = context.GetService<IDesignTimeModel>().Model.GetEntityTypes().ToArray();
        Assert.Equal(24, entities.Length); // Includes seven Identity tables; excludes passkeys.
        Assert.Equal(201, entities.Sum(entity => entity.GetProperties().Count()));
        Assert.Equal(42, entities.Sum(entity => entity.GetForeignKeys().Count()));
        Assert.Equal(48, entities.Sum(entity => entity.GetIndexes().Count()));
        Assert.Equal(38, entities.Sum(entity => entity.GetCheckConstraints().Count()));
        Assert.Equal(30, entities.SelectMany(entity => entity.GetProperties()).Count(property =>
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
        Assert.Equal(2, assembly.Migrations.Count);
        var migrationType = assembly.Migrations.Single(entry => entry.Key.EndsWith("_ExistingSchemaBaseline", StringComparison.Ordinal)).Value;
        var migration = assembly.CreateMigration(migrationType, context.Database.ProviderName!);
        Assert.Empty(migration.UpOperations);
        Assert.Empty(migration.DownOperations);
        var enrollmentType = assembly.Migrations.Single(entry => entry.Key.EndsWith("_FarmerRegistrationApproval", StringComparison.Ordinal)).Value;
        var enrollment = assembly.CreateMigration(enrollmentType, context.Database.ProviderName!);
        var upgrade = Assert.IsType<SqlOperation>(Assert.Single(enrollment.UpOperations));
        var script = File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "Database", "04_FarmerRegistrationApproval.sql"));
        Assert.Equal(script.Trim(), upgrade.Sql.Trim());
        Assert.Throws<NotSupportedException>(() => enrollment.DownOperations);
    }
}
