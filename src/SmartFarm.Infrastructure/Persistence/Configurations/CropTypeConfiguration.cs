using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class CropTypeConfiguration : IEntityTypeConfiguration<CropType>
{
    public void Configure(EntityTypeBuilder<CropType> builder)
    {
        builder.ToTable("CropType", "dbo", table =>
        {
            table.HasCheckConstraint("CK_CropType_GrowthDays", "DefaultGrowthDays IS NULL OR DefaultGrowthDays > 0");
            table.HasCheckConstraint("CK_CropType_WateringDays", "DefaultWateringIntervalDays IS NULL OR DefaultWateringIntervalDays > 0");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.Name).HasColumnType("nvarchar(100)").IsRequired(true).HasMaxLength(100);
        builder.Property(entity => entity.DefaultGrowthDays).HasColumnType("smallint");
        builder.Property(entity => entity.DefaultWateringIntervalDays).HasColumnType("smallint");
        builder.Property(entity => entity.DefaultHarvestUnit).HasColumnType("nvarchar(20)").IsRequired(true).HasMaxLength(20);
        builder.Property(entity => entity.IsActive).HasColumnType("bit").HasDefaultValue(true, "DF_CropType_IsActive");
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_CropType_CreatedAt");
        builder.HasKey(entity => entity.Id).HasName("PK_CropType");
        builder.HasIndex(entity => entity.Name).HasDatabaseName("UX_CropType_Name").IsUnique().HasFilter(null);
    }
}
