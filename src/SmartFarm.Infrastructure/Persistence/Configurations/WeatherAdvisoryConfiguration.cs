using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class WeatherAdvisoryConfiguration : IEntityTypeConfiguration<WeatherAdvisory>
{
    public void Configure(EntityTypeBuilder<WeatherAdvisory> builder)
    {
        builder.ToTable("WeatherAdvisory", "dbo", table =>
        {
            table.HasCheckConstraint("CK_WeatherAdvisory_Type", "AdvisoryType BETWEEN 1 AND 4");
            table.HasCheckConstraint("CK_WeatherAdvisory_Severity", "Severity BETWEEN 1 AND 3");
            table.HasCheckConstraint("CK_WeatherAdvisory_Status", "Status IN (0,1)");
            table.HasCheckConstraint("CK_WeatherAdvisory_Resolved", "Status <> 1 OR ResolvedAtUtc IS NOT NULL");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.WeatherForecastId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.AdvisoryDate).HasColumnType("date");
        builder.Property(entity => entity.AdvisoryType).HasColumnType("tinyint");
        builder.Property(entity => entity.Severity).HasColumnType("tinyint");
        builder.Property(entity => entity.Status).HasColumnType("tinyint").HasDefaultValueSql("0", "DF_WeatherAdvisory_Status");
        builder.Property(entity => entity.Message).HasColumnType("nvarchar(500)").IsRequired(true).HasMaxLength(500);
        builder.Property(entity => entity.CreatedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_WeatherAdvisory_CreatedAt");
        builder.Property(entity => entity.ResolvedAtUtc).HasColumnType("datetime2(3)");
        builder.HasKey(entity => entity.Id).HasName("PK_WeatherAdvisory");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_WeatherAdvisory_Farm");
        builder.HasOne<WeatherForecast>().WithMany().HasForeignKey(entity => entity.WeatherForecastId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_WeatherAdvisory_Forecast");
        builder.HasIndex(entity => new { entity.FarmId, entity.AdvisoryType, entity.AdvisoryDate }).HasDatabaseName("UX_WeatherAdvisory_Daily").IsUnique().HasFilter(null);
        builder.HasIndex(entity => entity.WeatherForecastId).HasDatabaseName("IX_WeatherAdvisory_Forecast").HasFilter("WeatherForecastId IS NOT NULL");
    }
}
