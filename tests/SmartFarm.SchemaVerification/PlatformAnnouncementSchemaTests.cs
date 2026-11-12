using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Metadata;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.SchemaVerification;

public sealed class PlatformAnnouncementSchemaTests
{
    [Fact]
    public void AnnouncementsHaveSafeConstraintsAndNonCascadingCreator()
    {
        using var db = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entity = db.GetService<IDesignTimeModel>().Model.FindEntityType(typeof(PlatformAnnouncement));
        Assert.NotNull(entity);
        Assert.Equal("PlatformAnnouncement", entity!.GetTableName());
        Assert.Equal("dbo", entity.GetSchema());
        Assert.Equal(11, entity.GetProperties().Count());
        Assert.Equal(2, entity.GetIndexes().Count());
        Assert.Equal(3, entity.GetCheckConstraints().Count());
        Assert.Single(entity.GetForeignKeys());
        Assert.All(entity.GetForeignKeys(), fk => Assert.Equal(DeleteBehavior.NoAction, fk.DeleteBehavior));
        Assert.Equal(140, entity.FindProperty(nameof(PlatformAnnouncement.Title))!.GetMaxLength());
        Assert.Equal(2000, entity.FindProperty(nameof(PlatformAnnouncement.Body))!.GetMaxLength());
        Assert.True(entity.FindProperty(nameof(PlatformAnnouncement.RowVersion))!.IsConcurrencyToken);
        Assert.Contains(entity.GetCheckConstraints(), ck =>
            ck.Name == "CK_PlatformAnnouncement_Published" && ck.Sql.Contains("PublishedAtUtc IS NOT NULL"));
    }

    [Fact]
    public void UpgradeScriptIsAdditiveAndIdempotent()
    {
        var sql = File.ReadAllText(Path.Combine(AppContext.BaseDirectory,
            "Database", "09_PlatformAnnouncements.sql"));
        Assert.Contains("IF OBJECT_ID(N'dbo.PlatformAnnouncement'", sql);
        Assert.Contains("IF NOT EXISTS (SELECT 1 FROM sys.indexes", sql);
        Assert.Contains("BEGIN TRANSACTION", sql);
        Assert.Contains("ROLLBACK TRANSACTION", sql);
        Assert.DoesNotContain("DROP TABLE", sql);
        Assert.DoesNotContain("DELETE FROM", sql);
        Assert.DoesNotContain("INSERT INTO dbo.AspNetUsers", sql);
    }
}
