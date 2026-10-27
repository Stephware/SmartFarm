using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class FarmConfiguration : IEntityTypeConfiguration<Farm>
{
    public void Configure(EntityTypeBuilder<Farm> builder)
    {
        builder.ToTable("Farm", "dbo", table =>
        {
            table.HasCheckConstraint("CK_Farm_Coordinates", "(Latitude IS NULL AND Longitude IS NULL) OR (Latitude IS NOT NULL AND Longitude IS NOT NULL AND Latitude BETWEEN -90 AND 90 AND Longitude BETWEEN -180 AND 180)");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.OwnerUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.JoinCode).HasColumnType("nvarchar(12)").IsRequired().HasMaxLength(12);
        builder.Property(entity => entity.Name).HasColumnType("nvarchar(120)").IsRequired(true).HasMaxLength(120);
        builder.Property(entity => entity.LocationText).HasColumnType("nvarchar(250)").IsRequired(true).HasMaxLength(250);
        builder.Property(entity => entity.Latitude).HasColumnType("decimal(9,6)");
        builder.Property(entity => entity.Longitude).HasColumnType("decimal(9,6)");
        builder.Property(entity => entity.TimeZoneId).HasColumnType("nvarchar(64)").IsRequired(true).HasMaxLength(64).HasDefaultValueSql("N'Asia/Manila'", "DF_Farm_TimeZone");
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_Farm_CreatedAt");
        builder.Property(entity => entity.ArchivedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.RowVersion).HasColumnType("rowversion").IsRequired(true).IsRowVersion();
        builder.HasKey(entity => entity.Id).HasName("PK_Farm");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.OwnerUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Farm_OwnerUser");
        builder.HasIndex(entity => entity.OwnerUserId).HasDatabaseName("IX_Farm_Owner");
        builder.HasIndex(entity => entity.JoinCode).IsUnique().HasDatabaseName("UX_Farm_JoinCode");
    }
}
