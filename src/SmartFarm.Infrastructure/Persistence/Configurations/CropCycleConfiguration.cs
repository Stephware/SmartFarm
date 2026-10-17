using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class CropCycleConfiguration : IEntityTypeConfiguration<CropCycle>
{
    public void Configure(EntityTypeBuilder<CropCycle> builder)
    {
        builder.ToTable("CropCycle", "dbo", table =>
        {
            table.HasCheckConstraint("CK_CropCycle_Status", "Status IN (0,1,2,3,4)");
            table.HasCheckConstraint("CK_CropCycle_Watering", "WateringIntervalDays IS NULL OR WateringIntervalDays > 0");
            table.HasCheckConstraint("CK_CropCycle_ActivePlanting", "Status NOT IN (1,2,3) OR PlantedDate IS NOT NULL");
            table.HasCheckConstraint("CK_CropCycle_CompletedTime", "Status <> 2 OR CompletedAtUtc IS NOT NULL");
            table.HasCheckConstraint("CK_CropCycle_HarvestDate", "ExpectedHarvestDate IS NULL OR PlantedDate IS NULL OR ExpectedHarvestDate >= PlantedDate");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.PlotId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.CropTypeId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.PlannedPlantingDate).HasColumnType("date");
        builder.Property(entity => entity.PlantedDate).HasColumnType("date");
        builder.Property(entity => entity.ExpectedHarvestDate).HasColumnType("date");
        builder.Property(entity => entity.HarvestUnit).HasColumnType("nvarchar(20)").IsRequired(true).HasMaxLength(20);
        builder.Property(entity => entity.WateringIntervalDays).HasColumnType("smallint");
        builder.Property(entity => entity.Status).HasColumnType("tinyint").HasDefaultValueSql("0", "DF_CropCycle_Status");
        builder.Property(entity => entity.CompletedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.TerminationReason).HasColumnType("nvarchar(300)").IsRequired(false).HasMaxLength(300);
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_CropCycle_CreatedAt");
        builder.Property(entity => entity.RowVersion).HasColumnType("rowversion").IsRequired(true).IsRowVersion();
        builder.HasKey(entity => entity.Id).HasName("PK_CropCycle");
        builder.HasOne<Plot>().WithMany().HasForeignKey(entity => entity.PlotId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropCycle_Plot");
        builder.HasOne<CropType>().WithMany().HasForeignKey(entity => entity.CropTypeId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropCycle_CropType");
        builder.HasIndex(entity => entity.PlotId).HasDatabaseName("UX_CropCycle_OneActivePerPlot").IsUnique().HasFilter("Status = 1");
        builder.HasIndex(entity => entity.CropTypeId).HasDatabaseName("IX_CropCycle_CropType");
        builder.HasIndex(entity => new { entity.PlotId, entity.Status }).HasDatabaseName("IX_CropCycle_Plot_Status");
    }
}
