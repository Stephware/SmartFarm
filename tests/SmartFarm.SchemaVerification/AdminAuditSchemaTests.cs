using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Metadata;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.SchemaVerification;

public sealed class AdminAuditSchemaTests
{
    [Fact]
    public void AuditHistoryUsesConstrainedMetadataAndNonCascadingActorLink()
    {
        using var db = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entity = db.GetService<IDesignTimeModel>().Model.FindEntityType(typeof(AdminAuditLog));
        Assert.NotNull(entity);
        Assert.Equal("AdminAuditLog", entity!.GetTableName());
        Assert.Equal("dbo", entity.GetSchema());
        Assert.Equal(8, entity.GetProperties().Count());
        Assert.Single(entity.GetForeignKeys());
        Assert.All(entity.GetForeignKeys(),
            fk => Assert.Equal(DeleteBehavior.NoAction, fk.DeleteBehavior));
        Assert.Equal(2, entity.GetIndexes().Count());
        Assert.Equal(100, entity.FindProperty(nameof(AdminAuditLog.Action))!.GetMaxLength());
        Assert.Equal(300, entity.FindProperty(nameof(AdminAuditLog.Reason))!.GetMaxLength());
        Assert.False(entity.FindProperty(nameof(AdminAuditLog.ActorUserId))!.IsNullable == false);
    }

    [Fact]
    public void UpgradeScriptIsIdempotentAndDoesNotTouchFarmRecords()
    {
        var path = Path.Combine(AppContext.BaseDirectory, "Database", "07_AdminFoundation.sql");
        var sql = File.ReadAllText(path);
        Assert.Contains("OBJECT_ID(N'dbo.AdminAuditLog'", sql);
        Assert.Contains("IF NOT EXISTS (SELECT 1 FROM sys.indexes", sql);
        Assert.Contains("BEGIN TRANSACTION", sql);
        Assert.Contains("ROLLBACK TRANSACTION", sql);
        Assert.DoesNotContain("DROP TABLE", sql);
        Assert.DoesNotContain("DELETE FROM", sql);
        Assert.DoesNotContain("INSERT INTO dbo.AspNetUsers", sql);
    }
}
