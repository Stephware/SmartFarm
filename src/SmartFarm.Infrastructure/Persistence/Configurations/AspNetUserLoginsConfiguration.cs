using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AspNetUserLoginsConfiguration : IEntityTypeConfiguration<IdentityUserLogin<Guid>>
{
    public void Configure(EntityTypeBuilder<IdentityUserLogin<Guid>> builder)
    {
        builder.ToTable("AspNetUserLogins", "dbo");
        builder.Property(entity => entity.LoginProvider).HasColumnType("nvarchar(128)").IsRequired(true).HasMaxLength(128);
        builder.Property(entity => entity.ProviderKey).HasColumnType("nvarchar(128)").IsRequired(true).HasMaxLength(128);
        builder.Property(entity => entity.ProviderDisplayName).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.HasKey(entity => new { entity.LoginProvider, entity.ProviderKey }).HasName("PK_AspNetUserLogins");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.Cascade).HasConstraintName("FK_AspNetUserLogins_AspNetUsers_UserId");
        builder.HasIndex(entity => entity.UserId).HasDatabaseName("IX_AspNetUserLogins_UserId");
    }
}
