using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class ObservationPhotoConfiguration : IEntityTypeConfiguration<ObservationPhoto>
{
    public void Configure(EntityTypeBuilder<ObservationPhoto> builder)
    {
        builder.ToTable("ObservationPhoto", "dbo", table =>
        {
            table.HasCheckConstraint("CK_ObservationPhoto_Size", "FileSizeBytes > 0");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.CropObservationId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.StorageKey).HasColumnType("nvarchar(450)").IsRequired(true).HasMaxLength(450);
        builder.Property(entity => entity.ContentType).HasColumnType("nvarchar(100)").IsRequired(true).HasMaxLength(100);
        builder.Property(entity => entity.FileSizeBytes).HasColumnType("bigint");
        builder.Property(entity => entity.UploadedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_ObservationPhoto_UploadedAt");
        builder.Property(entity => entity.UploadedByUserId).HasColumnType("uniqueidentifier");
        builder.HasKey(entity => entity.Id).HasName("PK_ObservationPhoto");
        builder.HasOne<CropObservation>().WithMany().HasForeignKey(entity => entity.CropObservationId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_ObservationPhoto_Observation");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UploadedByUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_ObservationPhoto_User");
        builder.HasIndex(entity => entity.CropObservationId).HasDatabaseName("IX_ObservationPhoto_Observation");
        builder.HasIndex(entity => entity.UploadedByUserId).HasDatabaseName("IX_ObservationPhoto_User");
    }
}
