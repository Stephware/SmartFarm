namespace SmartFarm.Core.Entities;

public sealed class CropCycle
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid PlotId { get; set; }
    public Guid CropTypeId { get; set; }
    public DateOnly PlannedPlantingDate { get; set; }
    public DateOnly? PlantedDate { get; set; }
    public DateOnly? ExpectedHarvestDate { get; set; }
    public string HarvestUnit { get; set; } = string.Empty;
    public short? WateringIntervalDays { get; set; }
    public byte Status { get; set; }
    public DateTime? CompletedAtUtc { get; set; }
    public string? TerminationReason { get; set; }
    public DateTime CreatedAtUtc { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
