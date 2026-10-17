using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class HarvestConfiguration : IEntityTypeConfiguration<Harvest>
{
    public void Configure(EntityTypeBuilder<Harvest> builder)
    {
        builder.ToTable("Harvest", "dbo", table =>
        {
            table.HasCheckConstraint("CK_Harvest_Quantity", "Quantity > 0");
            table.HasCheckConstraint("CK_Harvest_Void", "(VoidedAtUtc IS NULL AND VoidedByUserId IS NULL AND VoidReason IS NULL) OR (VoidedAtUtc IS NOT NULL AND VoidedByUserId IS NOT NULL AND VoidReason IS NOT NULL)");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.CropCycleId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.HarvestedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.Quantity).HasColumnType("decimal(18,3)");
        builder.Property(entity => entity.Unit).HasColumnType("nvarchar(20)").IsRequired(true).HasMaxLength(20);
        builder.Property(entity => entity.RecordedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.RecordedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_Harvest_RecordedAt");
        builder.Property(entity => entity.Notes).HasColumnType("nvarchar(1000)").IsRequired(false).HasMaxLength(1000);
        builder.Property(entity => entity.VoidedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.VoidedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.VoidReason).HasColumnType("nvarchar(300)").IsRequired(false).HasMaxLength(300);
        builder.HasKey(entity => entity.Id).HasName("PK_Harvest");
        builder.HasOne<CropCycle>().WithMany().HasForeignKey(entity => entity.CropCycleId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Harvest_CropCycle");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.RecordedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Harvest_RecordedBy");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.VoidedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Harvest_VoidedBy");
        builder.HasIndex(entity => new { entity.CropCycleId, entity.HarvestedAtUtc }).HasDatabaseName("IX_Harvest_Cycle_Time");
        builder.HasIndex(entity => entity.RecordedByUserId).HasDatabaseName("IX_Harvest_RecordedBy");
        builder.HasIndex(entity => entity.VoidedByUserId).HasDatabaseName("IX_Harvest_VoidedBy").HasFilter("VoidedByUserId IS NOT NULL");
    }
}
