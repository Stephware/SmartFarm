namespace SmartFarm.Core.Entities;

public sealed class Plot
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public string Name { get; set; } = string.Empty;
    public decimal AreaSqMeters { get; set; }
    public DateTime CreatedAtUtc { get; set; }
    public DateTime? ArchivedAtUtc { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
