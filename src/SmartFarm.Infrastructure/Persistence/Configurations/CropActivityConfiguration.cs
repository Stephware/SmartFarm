using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class CropActivityConfiguration : IEntityTypeConfiguration<CropActivity>
{
    public void Configure(EntityTypeBuilder<CropActivity> builder)
    {
        builder.ToTable("CropActivity", "dbo", table =>
        {
            table.HasCheckConstraint("CK_CropActivity_Type", "ActivityType BETWEEN 1 AND 5");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.CropCycleId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.FarmTaskId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.ActivityType).HasColumnType("tinyint");
        builder.Property(entity => entity.OccurredAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.RecordedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_CropActivity_RecordedAt");
        builder.Property(entity => entity.RecordedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Notes).HasColumnType("nvarchar(1000)").IsRequired(false).HasMaxLength(1000);
        builder.HasKey(entity => entity.Id).HasName("PK_CropActivity");
        builder.HasOne<CropCycle>().WithMany().HasForeignKey(entity => entity.CropCycleId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropActivity_CropCycle");
        builder.HasOne<FarmTask>().WithMany().HasForeignKey(entity => entity.FarmTaskId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropActivity_FarmTask");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.RecordedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropActivity_User");
        builder.HasIndex(entity => new { entity.CropCycleId, entity.OccurredAtUtc }).HasDatabaseName("IX_CropActivity_Cycle_Time");
        builder.HasIndex(entity => entity.FarmTaskId).HasDatabaseName("IX_CropActivity_Task").HasFilter("FarmTaskId IS NOT NULL");
        builder.HasIndex(entity => entity.RecordedByUserId).HasDatabaseName("IX_CropActivity_User");
    }
}
