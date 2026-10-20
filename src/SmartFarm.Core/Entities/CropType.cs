namespace SmartFarm.Core.Entities;

public sealed class CropType
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public string Name { get; set; } = string.Empty;
    public short? DefaultGrowthDays { get; set; }
    public short? DefaultWateringIntervalDays { get; set; }
    public string DefaultHarvestUnit { get; set; } = string.Empty;
    public bool IsActive { get; set; } = true;
    public DateTime CreatedAtUtc { get; set; }
}
