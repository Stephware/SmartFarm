using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class CropObservationConfiguration : IEntityTypeConfiguration<CropObservation>
{
    public void Configure(EntityTypeBuilder<CropObservation> builder)
    {
        builder.ToTable("CropObservation", "dbo", table =>
        {
            table.HasCheckConstraint("CK_CropObservation_Health", "HealthStatus BETWEEN 0 AND 3");
            table.HasCheckConstraint("CK_CropObservation_Issue",
                "(IssueCategory = 0 AND IssueSeverity = 0 AND FollowUpStatus = 0 AND FollowUpTaskId IS NULL) OR " +
                "(IssueCategory BETWEEN 1 AND 5 AND IssueSeverity BETWEEN 1 AND 3 AND " +
                "FollowUpStatus BETWEEN 1 AND 2 AND HealthStatus IN (2,3))");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.CropCycleId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.HealthStatus).HasColumnType("tinyint");
        builder.Property(entity => entity.GrowthStage).HasColumnType("nvarchar(40)").IsRequired(false).HasMaxLength(40);
        builder.Property(entity => entity.ObservedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.RecordedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_CropObservation_RecordedAt");
        builder.Property(entity => entity.RecordedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Notes).HasColumnType("nvarchar(1000)").IsRequired(false).HasMaxLength(1000);
        builder.Property(entity => entity.IssueCategory).HasColumnType("tinyint").HasDefaultValue((byte)0, "DF_CropObservation_IssueCategory");
        builder.Property(entity => entity.IssueSeverity).HasColumnType("tinyint").HasDefaultValue((byte)0, "DF_CropObservation_IssueSeverity");
        builder.Property(entity => entity.FollowUpStatus).HasColumnType("tinyint").HasDefaultValue((byte)0, "DF_CropObservation_FollowUpStatus");
        builder.Property(entity => entity.FollowUpTaskId).HasColumnType("uniqueidentifier").IsRequired(false);
        builder.Property(entity => entity.RowVersion).HasColumnType("rowversion").IsRowVersion();
        builder.HasKey(entity => entity.Id).HasName("PK_CropObservation");
        builder.HasOne<CropCycle>().WithMany().HasForeignKey(entity => entity.CropCycleId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropObservation_CropCycle");
        builder.HasOne<FarmTask>().WithMany().HasForeignKey(entity => entity.FollowUpTaskId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropObservation_FollowUpTask");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.RecordedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_CropObservation_User");
        builder.HasIndex(entity => new { entity.CropCycleId, entity.ObservedAtUtc }).HasDatabaseName("IX_CropObservation_Cycle_Time");
        builder.HasIndex(entity => entity.FollowUpTaskId)
            .HasDatabaseName("IX_CropObservation_FollowUpTask");
        builder.HasIndex(entity => entity.RecordedByUserId).HasDatabaseName("IX_CropObservation_User");
    }
}
