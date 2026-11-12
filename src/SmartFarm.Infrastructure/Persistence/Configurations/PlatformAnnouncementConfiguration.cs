using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class PlatformAnnouncementConfiguration : IEntityTypeConfiguration<PlatformAnnouncement>
{
    public void Configure(EntityTypeBuilder<PlatformAnnouncement> builder)
    {
        builder.ToTable("PlatformAnnouncement", "dbo", table =>
        {
            table.HasCheckConstraint("CK_PlatformAnnouncement_State",
                "State IN (N'Draft', N'Published', N'Archived')");
            table.HasCheckConstraint("CK_PlatformAnnouncement_Category",
                "Category IN (N'General', N'Maintenance', N'Update')");
            table.HasCheckConstraint("CK_PlatformAnnouncement_Published",
                "(State = N'Draft' AND PublishedAtUtc IS NULL) OR " +
                "(State IN (N'Published', N'Archived') AND PublishedAtUtc IS NOT NULL)");
        });
        builder.HasKey(x => x.Id).HasName("PK_PlatformAnnouncement");
        builder.Property(x => x.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(x => x.CreatedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.Title).HasColumnType("nvarchar(140)").HasMaxLength(140).IsRequired();
        builder.Property(x => x.Body).HasColumnType("nvarchar(2000)").HasMaxLength(2000).IsRequired();
        builder.Property(x => x.Category).HasColumnType("nvarchar(20)").HasMaxLength(20).IsRequired();
        builder.Property(x => x.State).HasColumnType("nvarchar(16)").HasMaxLength(16).IsRequired();
        builder.Property(x => x.CreatedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.UpdatedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.PublishedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.ExpiresAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.RowVersion).HasColumnType("rowversion").IsRowVersion();
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(x => x.CreatedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_PlatformAnnouncement_Creator");
        builder.HasIndex(x => new { x.State, x.PublishedAtUtc })
            .HasDatabaseName("IX_PlatformAnnouncement_State_Published");
        builder.HasIndex(x => x.CreatedByUserId)
            .HasDatabaseName("IX_PlatformAnnouncement_Creator");
    }
}
