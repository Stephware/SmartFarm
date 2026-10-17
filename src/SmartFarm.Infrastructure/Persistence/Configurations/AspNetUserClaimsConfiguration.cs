using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AspNetUserClaimsConfiguration : IEntityTypeConfiguration<IdentityUserClaim<Guid>>
{
    public void Configure(EntityTypeBuilder<IdentityUserClaim<Guid>> builder)
    {
        builder.ToTable("AspNetUserClaims", "dbo");
        builder.Property(entity => entity.Id).HasColumnType("int").UseIdentityColumn();
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.ClaimType).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.Property(entity => entity.ClaimValue).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.HasKey(entity => entity.Id).HasName("PK_AspNetUserClaims");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.Cascade).HasConstraintName("FK_AspNetUserClaims_AspNetUsers_UserId");
        builder.HasIndex(entity => entity.UserId).HasDatabaseName("IX_AspNetUserClaims_UserId");
    }
}
