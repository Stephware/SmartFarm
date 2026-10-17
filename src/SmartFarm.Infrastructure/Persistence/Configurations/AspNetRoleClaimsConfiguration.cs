using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AspNetRoleClaimsConfiguration : IEntityTypeConfiguration<IdentityRoleClaim<Guid>>
{
    public void Configure(EntityTypeBuilder<IdentityRoleClaim<Guid>> builder)
    {
        builder.ToTable("AspNetRoleClaims", "dbo");
        builder.Property(entity => entity.Id).HasColumnType("int").UseIdentityColumn();
        builder.Property(entity => entity.RoleId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.ClaimType).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.Property(entity => entity.ClaimValue).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.HasKey(entity => entity.Id).HasName("PK_AspNetRoleClaims");
        builder.HasOne<SmartFarmRole>().WithMany().HasForeignKey(entity => entity.RoleId)
            .OnDelete(DeleteBehavior.Cascade).HasConstraintName("FK_AspNetRoleClaims_AspNetRoles_RoleId");
        builder.HasIndex(entity => entity.RoleId).HasDatabaseName("IX_AspNetRoleClaims_RoleId");
    }
}
