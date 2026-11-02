using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class FarmFinanceEntryConfiguration : IEntityTypeConfiguration<FarmFinanceEntry>
{
    public void Configure(EntityTypeBuilder<FarmFinanceEntry> builder)
    {
        builder.ToTable("FarmFinanceEntry", "dbo", table =>
        {
            table.HasCheckConstraint("CK_FarmFinanceEntry_Kind", "Kind IN (N'Expense', N'Income')");
            table.HasCheckConstraint("CK_FarmFinanceEntry_Amount", "AmountPhp > 0");
            table.HasCheckConstraint("CK_FarmFinanceEntry_Void",
                "(VoidedAtUtc IS NULL AND VoidedByUserId IS NULL AND VoidReason IS NULL) OR " +
                "(VoidedAtUtc IS NOT NULL AND VoidedByUserId IS NOT NULL AND VoidReason IS NOT NULL)");
        });
        builder.HasKey(x => x.Id).HasName("PK_FarmFinanceEntry");
        builder.Property(x => x.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(x => x.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.CropCycleId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.RecordedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.EntryDate).HasColumnType("date");
        builder.Property(x => x.Kind).HasColumnType("nvarchar(10)").HasMaxLength(10).IsRequired();
        builder.Property(x => x.Category).HasColumnType("nvarchar(60)").HasMaxLength(60).IsRequired();
        builder.Property(x => x.AmountPhp).HasColumnType("decimal(14,2)");
        builder.Property(x => x.Notes).HasColumnType("nvarchar(500)").HasMaxLength(500);
        builder.Property(x => x.RecordedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql(
            "SYSUTCDATETIME()", "DF_FarmFinanceEntry_RecordedAt");
        builder.Property(x => x.VoidedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.VoidedByUserId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.VoidReason).HasColumnType("nvarchar(300)").HasMaxLength(300);
        builder.HasOne<Farm>().WithMany().HasForeignKey(x => x.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmFinanceEntry_Farm");
        builder.HasOne<CropCycle>().WithMany().HasForeignKey(x => x.CropCycleId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmFinanceEntry_CropCycle");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(x => x.RecordedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmFinanceEntry_RecordedBy");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(x => x.VoidedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmFinanceEntry_VoidedBy");
        builder.HasIndex(x => new { x.FarmId, x.EntryDate }).HasDatabaseName("IX_FarmFinanceEntry_Farm_Date");
        builder.HasIndex(x => x.CropCycleId).HasDatabaseName("IX_FarmFinanceEntry_Cycle");
        builder.HasIndex(x => x.RecordedByUserId).HasDatabaseName("IX_FarmFinanceEntry_RecordedBy");
    }
}
