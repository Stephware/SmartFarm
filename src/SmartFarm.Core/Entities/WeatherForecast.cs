namespace SmartFarm.Core.Entities;

public sealed class WeatherForecast
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public DateOnly ForecastDate { get; set; }
    public DateTime RetrievedAtUtc { get; set; }
    public decimal? MinTemperatureC { get; set; }
    public decimal? MaxTemperatureC { get; set; }
    public decimal? RainfallMm { get; set; }
    public decimal? MaxPrecipitationProbability { get; set; }
    public decimal? MaxWindKph { get; set; }
    public string Provider { get; set; } = string.Empty;
}
