using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AdminAuditLogConfiguration : IEntityTypeConfiguration<AdminAuditLog>
{
    public void Configure(EntityTypeBuilder<AdminAuditLog> builder)
    {
        builder.ToTable("AdminAuditLog", "dbo");
        builder.HasKey(x => x.Id).HasName("PK_AdminAuditLog");
        builder.Property(x => x.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(x => x.ActorUserId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.ActorSource).IsRequired().HasMaxLength(40).HasColumnType("nvarchar(40)");
        builder.Property(x => x.Action).IsRequired().HasMaxLength(100).HasColumnType("nvarchar(100)");
        builder.Property(x => x.TargetType).IsRequired().HasMaxLength(80).HasColumnType("nvarchar(80)");
        builder.Property(x => x.TargetId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.Reason).HasMaxLength(300).HasColumnType("nvarchar(300)");
        builder.Property(x => x.OccurredAtUtc).HasColumnType("datetime2(3)")
            .HasDefaultValueSql("SYSUTCDATETIME()", "DF_AdminAuditLog_OccurredAt");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(x => x.ActorUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_AdminAuditLog_Actor");
        builder.HasIndex(x => x.OccurredAtUtc).HasDatabaseName("IX_AdminAuditLog_OccurredAt");
        builder.HasIndex(x => new { x.ActorUserId, x.OccurredAtUtc })
            .HasDatabaseName("IX_AdminAuditLog_Actor_Date");
    }
}
