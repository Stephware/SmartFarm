using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class FarmTaskConfiguration : IEntityTypeConfiguration<FarmTask>
{
    public void Configure(EntityTypeBuilder<FarmTask> builder)
    {
        builder.ToTable("FarmTask", "dbo", table =>
        {
            table.HasCheckConstraint("CK_FarmTask_Type", "TaskType BETWEEN 1 AND 6");
            table.HasCheckConstraint("CK_FarmTask_Status", "Status BETWEEN 0 AND 3");
            table.HasCheckConstraint("CK_FarmTask_ClosedAt", "Status = 0 OR ClosedAtUtc IS NOT NULL");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.CropCycleId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.AssignedToUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.TaskType).HasColumnType("tinyint");
        builder.Property(entity => entity.DueDate).HasColumnType("date");
        builder.Property(entity => entity.Status).HasColumnType("tinyint").HasDefaultValueSql("0", "DF_FarmTask_Status");
        builder.Property(entity => entity.Description).HasColumnType("nvarchar(500)").IsRequired(false).HasMaxLength(500);
        builder.Property(entity => entity.DedupeKey).HasColumnType("nvarchar(160)").IsRequired(false).HasMaxLength(160);
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_FarmTask_CreatedAt");
        builder.Property(entity => entity.ClosedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.ClosedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.RowVersion).HasColumnType("rowversion").IsRequired(true).IsRowVersion();
        builder.HasKey(entity => entity.Id).HasName("PK_FarmTask");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmTask_Farm");
        builder.HasOne<CropCycle>().WithMany().HasForeignKey(entity => entity.CropCycleId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmTask_CropCycle");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.AssignedToUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmTask_AssignedUser");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.ClosedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmTask_ClosedUser");
        builder.HasIndex(entity => new { entity.FarmId, entity.DedupeKey }).HasDatabaseName("UX_FarmTask_Dedupe").IsUnique().HasFilter("DedupeKey IS NOT NULL");
        builder.HasIndex(entity => new { entity.FarmId, entity.Status, entity.DueDate }).HasDatabaseName("IX_FarmTask_Farm_Status_Due");
        builder.HasIndex(entity => entity.CropCycleId).HasDatabaseName("IX_FarmTask_CropCycle").HasFilter("CropCycleId IS NOT NULL");
        builder.HasIndex(entity => entity.AssignedToUserId).HasDatabaseName("IX_FarmTask_Assigned").HasFilter("AssignedToUserId IS NOT NULL");
        builder.HasIndex(entity => entity.ClosedByUserId).HasDatabaseName("IX_FarmTask_ClosedBy").HasFilter("ClosedByUserId IS NOT NULL");
    }
}
