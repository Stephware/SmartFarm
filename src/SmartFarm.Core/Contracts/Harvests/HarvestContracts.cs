using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Core.Contracts.Harvests;

public sealed class RecordHarvestInput
{
    [Range(typeof(decimal), "0.001", "999999999999999.999")]
    public decimal Quantity { get; set; }

    [StringLength(1000)]
    public string? Notes { get; set; }
}

public sealed class VoidHarvestInput
{
    [Required, StringLength(300, MinimumLength = 3)]
    public string Reason { get; set; } = "";
}

public sealed record HarvestEntry(
    Guid Id, Guid CropCycleId, decimal Quantity, string Unit,
    DateTime HarvestedAtUtc, DateTime RecordedAtUtc, Guid RecordedByUserId,
    string? Notes, bool IsVoided, DateTime? VoidedAtUtc,
    Guid? VoidedByUserId, string? VoidReason);

public sealed record HarvestOverview(
    Guid FarmId, Guid CycleId, string CropName, string PlotName,
    string Unit, byte CropStatus, bool CanRecord, bool CanVoid,
    decimal TotalQuantity, int ValidHarvestCount, int VoidedHarvestCount,
    IReadOnlyList<HarvestEntry> Entries);

