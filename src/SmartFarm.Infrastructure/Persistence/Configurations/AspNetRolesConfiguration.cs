using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AspNetRolesConfiguration : IEntityTypeConfiguration<SmartFarmRole>
{
    public void Configure(EntityTypeBuilder<SmartFarmRole> builder)
    {
        builder.ToTable("AspNetRoles", "dbo");
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.Name).HasColumnType("nvarchar(256)").IsRequired(false).HasMaxLength(256);
        builder.Property(entity => entity.NormalizedName).HasColumnType("nvarchar(256)").IsRequired(false).HasMaxLength(256);
        builder.Property(entity => entity.ConcurrencyStamp).HasColumnType("nvarchar(max)").IsRequired(false).IsConcurrencyToken();
        builder.HasKey(entity => entity.Id).HasName("PK_AspNetRoles");
        builder.HasIndex(entity => entity.NormalizedName).HasDatabaseName("RoleNameIndex").IsUnique().HasFilter("NormalizedName IS NOT NULL");
    }
}
