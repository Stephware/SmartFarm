using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class FarmJoinRequestConfiguration : IEntityTypeConfiguration<FarmJoinRequest>
{
    public void Configure(EntityTypeBuilder<FarmJoinRequest> builder)
    {
        builder.ToTable("FarmJoinRequest", "dbo", table =>
        {
            table.HasCheckConstraint("CK_FarmJoinRequest_Status", "Status IN (N'Pending', N'Approved', N'Rejected', N'Canceled')");
            table.HasCheckConstraint("CK_FarmJoinRequest_Review", "(Status = N'Pending' AND ReviewedByUserId IS NULL AND ReviewedAtUtc IS NULL) OR (Status IN (N'Approved', N'Rejected') AND ReviewedByUserId IS NOT NULL AND ReviewedAtUtc IS NOT NULL) OR (Status = N'Canceled' AND ReviewedByUserId = UserId AND ReviewedAtUtc IS NOT NULL)");
        });
        builder.HasKey(entity => entity.Id).HasName("PK_FarmJoinRequest");
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Status).HasColumnType("nvarchar(8)").IsRequired().HasMaxLength(8)
            .HasDefaultValueSql("N'Pending'", "DF_FarmJoinRequest_Status");
        builder.Property(entity => entity.RequestedAtUtc).HasColumnType("datetime2(3)")
            .HasDefaultValueSql("SYSUTCDATETIME()", "DF_FarmJoinRequest_RequestedAt");
        builder.Property(entity => entity.ReviewedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.ReviewedAtUtc).HasColumnType("datetime2(3)");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmJoinRequest_Farm");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmJoinRequest_User");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.ReviewedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmJoinRequest_Reviewer");
        builder.HasIndex(entity => new { entity.FarmId, entity.UserId }).IsUnique()
            .HasFilter("Status = N'Pending'").HasDatabaseName("UX_FarmJoinRequest_Pending");
        builder.HasIndex(entity => entity.UserId).HasDatabaseName("IX_FarmJoinRequest_User");
    }
}
