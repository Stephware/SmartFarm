using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AspNetUserTokensConfiguration : IEntityTypeConfiguration<IdentityUserToken<Guid>>
{
    public void Configure(EntityTypeBuilder<IdentityUserToken<Guid>> builder)
    {
        builder.ToTable("AspNetUserTokens", "dbo");
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.LoginProvider).HasColumnType("nvarchar(128)").IsRequired(true).HasMaxLength(128);
        builder.Property(entity => entity.Name).HasColumnType("nvarchar(128)").IsRequired(true).HasMaxLength(128);
        builder.Property(entity => entity.Value).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.HasKey(entity => new { entity.UserId, entity.LoginProvider, entity.Name }).HasName("PK_AspNetUserTokens");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.Cascade).HasConstraintName("FK_AspNetUserTokens_AspNetUsers_UserId");
    }
}
