using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class InventoryTransactionConfiguration : IEntityTypeConfiguration<InventoryTransaction>
{
    public void Configure(EntityTypeBuilder<InventoryTransaction> builder)
    {
        builder.ToTable("InventoryTransaction", "dbo", table =>
        {
            table.HasCheckConstraint("CK_InventoryTransaction_Quantity", "QuantityChange <> 0");
            table.HasCheckConstraint("CK_InventoryTransaction_Type", "TransactionType BETWEEN 1 AND 5");
            table.HasCheckConstraint("CK_InventoryTransaction_Sign", "(TransactionType IN (1,2) AND QuantityChange > 0) OR (TransactionType = 3 AND QuantityChange < 0) OR (TransactionType IN (4,5))");
            table.HasCheckConstraint("CK_InventoryTransaction_ConsumptionActivity", "TransactionType <> 3 OR CropActivityId IS NOT NULL");
            table.HasCheckConstraint("CK_InventoryTransaction_Reversal", "(TransactionType = 5 AND ReversesTransactionId IS NOT NULL) OR (TransactionType <> 5 AND ReversesTransactionId IS NULL)");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.InventoryItemId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.CropActivityId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.ReversesTransactionId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.TransactionType).HasColumnType("tinyint");
        builder.Property(entity => entity.QuantityChange).HasColumnType("decimal(18,3)");
        builder.Property(entity => entity.OccurredAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.RecordedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_InventoryTransaction_RecordedAt");
        builder.Property(entity => entity.RecordedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Reason).HasColumnType("nvarchar(300)").IsRequired(false).HasMaxLength(300);
        builder.HasKey(entity => entity.Id).HasName("PK_InventoryTransaction");
        builder.HasOne<InventoryItem>().WithMany().HasForeignKey(entity => entity.InventoryItemId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_InventoryTransaction_Item");
        builder.HasOne<CropActivity>().WithMany().HasForeignKey(entity => entity.CropActivityId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_InventoryTransaction_Activity");
        builder.HasOne<InventoryTransaction>().WithMany().HasForeignKey(entity => entity.ReversesTransactionId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_InventoryTransaction_Reverses");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.RecordedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_InventoryTransaction_User");
        builder.HasIndex(entity => entity.ReversesTransactionId).HasDatabaseName("UX_InventoryTransaction_Reverses").IsUnique().HasFilter("ReversesTransactionId IS NOT NULL");
        builder.HasIndex(entity => new { entity.InventoryItemId, entity.OccurredAtUtc }).HasDatabaseName("IX_InventoryTransaction_Item_Time");
        builder.HasIndex(entity => entity.CropActivityId).HasDatabaseName("IX_InventoryTransaction_Activity").HasFilter("CropActivityId IS NOT NULL");
        builder.HasIndex(entity => entity.RecordedByUserId).HasDatabaseName("IX_InventoryTransaction_User");
    }
}
