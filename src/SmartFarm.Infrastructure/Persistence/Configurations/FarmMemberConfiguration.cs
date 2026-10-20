using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class FarmMemberConfiguration : IEntityTypeConfiguration<FarmMember>
{
    public void Configure(EntityTypeBuilder<FarmMember> builder)
    {
        builder.ToTable("FarmMember", "dbo", table =>
        {
            table.HasCheckConstraint("CK_FarmMember_Role", "Role IN (1,2)");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Role).HasColumnType("tinyint");
        builder.Property(entity => entity.JoinedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_FarmMember_JoinedAt");
        builder.Property(entity => entity.RemovedAtUtc).HasColumnType("datetime2(3)");
        builder.HasKey(entity => entity.Id).HasName("PK_FarmMember");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmMember_Farm");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_FarmMember_User");
        builder.HasIndex(entity => new { entity.FarmId, entity.UserId }).HasDatabaseName("UX_FarmMember_Active").IsUnique().HasFilter("RemovedAtUtc IS NULL");
        builder.HasIndex(entity => entity.UserId).HasDatabaseName("IX_FarmMember_User");
    }
}
