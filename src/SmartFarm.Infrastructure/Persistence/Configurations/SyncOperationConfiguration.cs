using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class SyncOperationConfiguration : IEntityTypeConfiguration<SyncOperation>
{
    public void Configure(EntityTypeBuilder<SyncOperation> builder)
    {
        builder.ToTable("SyncOperation", "dbo", table =>
        {
            table.HasCheckConstraint("CK_SyncOperation_Status", "Status IN (1,2)");
        });
        builder.Property(entity => entity.OperationId).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.DeviceId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.OperationType).HasColumnType("nvarchar(50)").IsRequired(true).HasMaxLength(50);
        builder.Property(entity => entity.TargetEntityId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.RequestHash).HasColumnType("varbinary(32)").IsRequired(true).HasMaxLength(32);
        builder.Property(entity => entity.Status).HasColumnType("tinyint");
        builder.Property(entity => entity.ResultJson).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.Property(entity => entity.ErrorCode).HasColumnType("nvarchar(60)").IsRequired(false).HasMaxLength(60);
        builder.Property(entity => entity.ReceivedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_SyncOperation_ReceivedAt");
        builder.Property(entity => entity.CompletedAtUtc).HasColumnType("datetime2(3)");
        builder.HasKey(entity => entity.OperationId).HasName("PK_SyncOperation");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_SyncOperation_Farm");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_SyncOperation_User");
        builder.HasIndex(entity => new { entity.FarmId, entity.ReceivedAtUtc }).HasDatabaseName("IX_SyncOperation_Farm_Received");
        builder.HasIndex(entity => entity.UserId).HasDatabaseName("IX_SyncOperation_User");
    }
}
