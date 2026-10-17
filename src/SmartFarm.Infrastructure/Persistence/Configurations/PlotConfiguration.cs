using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class PlotConfiguration : IEntityTypeConfiguration<Plot>
{
    public void Configure(EntityTypeBuilder<Plot> builder)
    {
        builder.ToTable("Plot", "dbo", table =>
        {
            table.HasCheckConstraint("CK_Plot_Area", "AreaSqMeters > 0");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Name).HasColumnType("nvarchar(100)").IsRequired(true).HasMaxLength(100);
        builder.Property(entity => entity.AreaSqMeters).HasColumnType("decimal(12,2)");
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_Plot_CreatedAt");
        builder.Property(entity => entity.ArchivedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(entity => entity.RowVersion).HasColumnType("rowversion").IsRequired(true).IsRowVersion();
        builder.HasKey(entity => entity.Id).HasName("PK_Plot");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Plot_Farm");
        builder.HasIndex(entity => new { entity.FarmId, entity.Name }).HasDatabaseName("UX_Plot_Farm_Name_Active").IsUnique().HasFilter("ArchivedAtUtc IS NULL");
        builder.HasIndex(entity => entity.FarmId).HasDatabaseName("IX_Plot_Farm");
    }
}
