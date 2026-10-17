using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AspNetUserRolesConfiguration : IEntityTypeConfiguration<IdentityUserRole<Guid>>
{
    public void Configure(EntityTypeBuilder<IdentityUserRole<Guid>> builder)
    {
        builder.ToTable("AspNetUserRoles", "dbo");
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.RoleId).HasColumnType("uniqueidentifier");
        builder.HasKey(entity => new { entity.UserId, entity.RoleId }).HasName("PK_AspNetUserRoles");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.Cascade).HasConstraintName("FK_AspNetUserRoles_AspNetUsers_UserId");
        builder.HasOne<SmartFarmRole>().WithMany().HasForeignKey(entity => entity.RoleId)
            .OnDelete(DeleteBehavior.Cascade).HasConstraintName("FK_AspNetUserRoles_AspNetRoles_RoleId");
        builder.HasIndex(entity => entity.RoleId).HasDatabaseName("IX_AspNetUserRoles_RoleId");
    }
}
