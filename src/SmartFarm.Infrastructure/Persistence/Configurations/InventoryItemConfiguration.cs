using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class InventoryItemConfiguration : IEntityTypeConfiguration<InventoryItem>
{
    public void Configure(EntityTypeBuilder<InventoryItem> builder)
    {
        builder.ToTable("InventoryItem", "dbo", table =>
        {
            table.HasCheckConstraint("CK_InventoryItem_Category", "Category BETWEEN 1 AND 4");
            table.HasCheckConstraint("CK_InventoryItem_Stock", "QuantityOnHand >= 0");
            table.HasCheckConstraint("CK_InventoryItem_Reorder", "ReorderLevel IS NULL OR ReorderLevel >= 0");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Name).HasColumnType("nvarchar(120)").IsRequired(true).HasMaxLength(120);
        builder.Property(entity => entity.Category).HasColumnType("tinyint");
        builder.Property(entity => entity.Unit).HasColumnType("nvarchar(20)").IsRequired(true).HasMaxLength(20);
        builder.Property(entity => entity.Description).HasColumnType("nvarchar(500)").IsRequired(false).HasMaxLength(500);
        builder.Property(entity => entity.QuantityOnHand).HasColumnType("decimal(18,3)").HasDefaultValueSql("0", "DF_InventoryItem_Quantity");
        builder.Property(entity => entity.ReorderLevel).HasColumnType("decimal(18,3)");
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_InventoryItem_CreatedAt");
        builder.Property(entity => entity.ArchivedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.MetadataVersion).HasColumnType("uniqueidentifier").HasDefaultValueSql("NEWID()", "DF_InventoryItem_MetadataVersion");
        builder.Property(entity => entity.RowVersion).HasColumnType("rowversion").IsRequired(true).IsRowVersion();
        builder.HasKey(entity => entity.Id).HasName("PK_InventoryItem");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_InventoryItem_Farm");
        builder.HasIndex(entity => new { entity.FarmId, entity.ArchivedAtUtc }).HasDatabaseName("IX_InventoryItem_Farm_Archived");
    }
}
