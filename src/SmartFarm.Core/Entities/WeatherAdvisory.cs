namespace SmartFarm.Core.Entities;

public sealed class WeatherAdvisory
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public Guid? WeatherForecastId { get; set; }
    public DateOnly AdvisoryDate { get; set; }
    public byte AdvisoryType { get; set; }
    public byte Severity { get; set; }
    public byte Status { get; set; }
    public string Message { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; }
    public DateTime? ResolvedAtUtc { get; set; }
}
