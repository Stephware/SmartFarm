using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Crops;

public static class CropCycleRules
{
    public static bool CanTransition(CropCycleStatus from, CropCycleStatus to) => (from, to) switch
    {
        (CropCycleStatus.Planned, CropCycleStatus.Active or CropCycleStatus.Cancelled) => true,
        (CropCycleStatus.Active, CropCycleStatus.Completed or CropCycleStatus.Failed or CropCycleStatus.Cancelled) => true,
        _ => false
    };

    public static bool IsTerminal(CropCycleStatus status) =>
        status is CropCycleStatus.Completed or CropCycleStatus.Failed or CropCycleStatus.Cancelled;
}

