using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class NotificationConfiguration : IEntityTypeConfiguration<Notification>
{
    public void Configure(EntityTypeBuilder<Notification> builder)
    {
        builder.ToTable("Notification", "dbo", table =>
        {
            table.HasCheckConstraint("CK_Notification_Type", "Type IN (1,2,3)");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.UserId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.FarmTaskId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.WeatherAdvisoryId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.Type).HasColumnType("tinyint");
        builder.Property(entity => entity.DeduplicationKey).HasColumnType("nvarchar(180)").IsRequired(true).HasMaxLength(180);
        builder.Property(entity => entity.Title).HasColumnType("nvarchar(150)").IsRequired(true).HasMaxLength(150);
        builder.Property(entity => entity.Message).HasColumnType("nvarchar(500)").IsRequired(true).HasMaxLength(500);
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_Notification_CreatedAt");
        builder.Property(entity => entity.ReadAtUtc).HasColumnType("datetime2(3)");
        builder.HasKey(entity => entity.Id).HasName("PK_Notification");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(entity => entity.UserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Notification_User");
        builder.HasOne<FarmTask>().WithMany().HasForeignKey(entity => entity.FarmTaskId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Notification_Task");
        builder.HasOne<WeatherAdvisory>().WithMany().HasForeignKey(entity => entity.WeatherAdvisoryId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_Notification_Advisory");
        builder.HasIndex(entity => new { entity.UserId, entity.DeduplicationKey }).HasDatabaseName("UX_Notification_User_Dedupe").IsUnique().HasFilter(null);
        builder.HasIndex(entity => new { entity.UserId, entity.ReadAtUtc, entity.CreatedAtUtc }).HasDatabaseName("IX_Notification_User_Unread");
        builder.HasIndex(entity => entity.FarmTaskId).HasDatabaseName("IX_Notification_Task").HasFilter("FarmTaskId IS NOT NULL");
        builder.HasIndex(entity => entity.WeatherAdvisoryId).HasDatabaseName("IX_Notification_Advisory").HasFilter("WeatherAdvisoryId IS NOT NULL");
    }
}
