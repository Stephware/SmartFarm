using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class AspNetUsersConfiguration : IEntityTypeConfiguration<SmartFarmUser>
{
    public void Configure(EntityTypeBuilder<SmartFarmUser> builder)
    {
        builder.ToTable("AspNetUsers", "dbo");
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.UserName).HasColumnType("nvarchar(256)").IsRequired(false).HasMaxLength(256);
        builder.Property(entity => entity.NormalizedUserName).HasColumnType("nvarchar(256)").IsRequired(false).HasMaxLength(256);
        builder.Property(entity => entity.Email).HasColumnType("nvarchar(256)").IsRequired(false).HasMaxLength(256);
        builder.Property(entity => entity.NormalizedEmail).HasColumnType("nvarchar(256)").IsRequired(false).HasMaxLength(256);
        builder.Property(entity => entity.EmailConfirmed).HasColumnType("bit").HasDefaultValueSql("0", "DF_AspNetUsers_EmailConfirmed");
        builder.Property(entity => entity.PasswordHash).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.Property(entity => entity.SecurityStamp).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.Property(entity => entity.ConcurrencyStamp).HasColumnType("nvarchar(max)").IsRequired(false).IsConcurrencyToken();
        builder.Property(entity => entity.PhoneNumber).HasColumnType("nvarchar(max)").IsRequired(false);
        builder.Property(entity => entity.PhoneNumberConfirmed).HasColumnType("bit").HasDefaultValueSql("0", "DF_AspNetUsers_PhoneNumberConfirmed");
        builder.Property(entity => entity.TwoFactorEnabled).HasColumnType("bit").HasDefaultValueSql("0", "DF_AspNetUsers_TwoFactorEnabled");
        builder.Property(entity => entity.LockoutEnd).HasColumnType("datetimeoffset(7)");
        builder.Property(entity => entity.LockoutEnabled).HasColumnType("bit").HasDefaultValueSql("0", "DF_AspNetUsers_LockoutEnabled");
        builder.Property(entity => entity.AccessFailedCount).HasColumnType("int").HasDefaultValueSql("0", "DF_AspNetUsers_AccessFailedCount");
        builder.Property(entity => entity.FullName).HasColumnType("nvarchar(120)").IsRequired(true).HasMaxLength(120);
        builder.Property(entity => entity.DisabledAtUtc).HasColumnType("datetime2(3)");
        builder.HasKey(entity => entity.Id).HasName("PK_AspNetUsers");
        builder.HasIndex(entity => entity.NormalizedEmail).HasDatabaseName("EmailIndex");
        builder.HasIndex(entity => entity.NormalizedUserName).HasDatabaseName("UserNameIndex").IsUnique().HasFilter("NormalizedUserName IS NOT NULL");
    }
}
