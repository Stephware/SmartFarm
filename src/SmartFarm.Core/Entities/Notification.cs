namespace SmartFarm.Core.Entities;

public sealed class Notification
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid UserId { get; set; }
    public Guid? FarmTaskId { get; set; }
    public Guid? WeatherAdvisoryId { get; set; }
    public byte Type { get; set; }
    public string DeduplicationKey { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; }
    public DateTime? ReadAtUtc { get; set; }
}
