using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts.Crops;

public sealed class CropTypeInput
{
    [Required, StringLength(100, MinimumLength = 2)]
    public string Name { get; set; } = string.Empty;

    [Range(1, short.MaxValue)]
    public short? DefaultGrowthDays { get; set; }

    [Range(1, short.MaxValue)]
    public short? DefaultWateringIntervalDays { get; set; }

    [Required, StringLength(20, MinimumLength = 1)]
    public string DefaultHarvestUnit { get; set; } = "kg";
}

public sealed record CropTypeDetails(
    Guid Id, string Name, short? DefaultGrowthDays,
    short? DefaultWateringIntervalDays, string DefaultHarvestUnit);

public sealed class CropCyclePlanInput
{
    public Guid CropTypeId { get; set; }
    public DateOnly PlannedPlantingDate { get; set; }

    public DateOnly? ExpectedHarvestDate { get; set; }

    [StringLength(20)]
    public string? HarvestUnit { get; set; }

    [Range(1, short.MaxValue)]
    public short? WateringIntervalDays { get; set; }

    public string? RowVersion { get; set; }
}

public sealed class CropCycleTransitionInput
{
    [Required]
    public string RowVersion { get; set; } = string.Empty;

    public CropCycleStatus Status { get; set; }

    public DateOnly? PlantedDate { get; set; }

    [StringLength(300)]
    public string? TerminationReason { get; set; }
}

public sealed record CropCycleDetails(
    Guid Id, Guid FarmId, Guid PlotId, string PlotName,
    Guid CropTypeId, string CropTypeName, CropCycleStatus Status,
    DateOnly PlannedPlantingDate, DateOnly? PlantedDate,
    DateOnly? ExpectedHarvestDate, string HarvestUnit,
    short? WateringIntervalDays, DateTime? CompletedAtUtc,
    string? TerminationReason, string RowVersion);

