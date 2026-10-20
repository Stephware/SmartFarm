using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class WeatherForecastConfiguration : IEntityTypeConfiguration<WeatherForecast>
{
    public void Configure(EntityTypeBuilder<WeatherForecast> builder)
    {
        builder.ToTable("WeatherForecast", "dbo", table =>
        {
            table.HasCheckConstraint("CK_WeatherForecast_Temperature", "MinTemperatureC IS NULL OR MaxTemperatureC IS NULL OR MinTemperatureC <= MaxTemperatureC");
            table.HasCheckConstraint("CK_WeatherForecast_Rain", "RainfallMm IS NULL OR RainfallMm >= 0");
            table.HasCheckConstraint("CK_WeatherForecast_Probability", "MaxPrecipitationProbability IS NULL OR MaxPrecipitationProbability BETWEEN 0 AND 100");
            table.HasCheckConstraint("CK_WeatherForecast_Wind", "MaxWindKph IS NULL OR MaxWindKph >= 0");
        });
        builder.Property(entity => entity.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(entity => entity.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(entity => entity.ForecastDate).HasColumnType("date");
        builder.Property(entity => entity.RetrievedAtUtc).HasColumnType("datetime2(3)").HasDefaultValueSql("SYSUTCDATETIME()", "DF_WeatherForecast_RetrievedAt");
        builder.Property(entity => entity.MinTemperatureC).HasColumnType("decimal(5,2)");
        builder.Property(entity => entity.MaxTemperatureC).HasColumnType("decimal(5,2)");
        builder.Property(entity => entity.RainfallMm).HasColumnType("decimal(8,2)");
        builder.Property(entity => entity.MaxPrecipitationProbability).HasColumnType("decimal(5,2)");
        builder.Property(entity => entity.MaxWindKph).HasColumnType("decimal(7,2)");
        builder.Property(entity => entity.Provider).HasColumnType("nvarchar(32)").IsRequired(true).HasMaxLength(32);
        builder.HasKey(entity => entity.Id).HasName("PK_WeatherForecast");
        builder.HasOne<Farm>().WithMany().HasForeignKey(entity => entity.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_WeatherForecast_Farm");
        builder.HasIndex(entity => new { entity.FarmId, entity.ForecastDate, entity.RetrievedAtUtc }).HasDatabaseName("IX_WeatherForecast_Farm_Date_Retrieved");
    }
}
