using SmartFarm.Core.Contracts.Crops;

namespace SmartFarm.Core.Interfaces.Crops;

public interface ICropManagementService
{
    Task<IReadOnlyList<CropTypeDetails>> ListCropTypesAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<CropTypeDetails> CreateCropTypeAsync(Guid farmId, Guid actor, CropTypeInput input, CancellationToken ct = default);
    Task<IReadOnlyList<CropCycleDetails>> ListCyclesAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<CropCycleDetails?> GetCycleAsync(Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default);
    Task<CropCycleDetails> PlanAsync(Guid farmId, Guid plotId, Guid actor, CropCyclePlanInput input, CancellationToken ct = default);
    Task<CropCycleDetails> UpdatePlanAsync(Guid farmId, Guid cycleId, Guid actor, CropCyclePlanInput input, CancellationToken ct = default);
    Task<CropCycleDetails> TransitionAsync(Guid farmId, Guid cycleId, Guid actor, CropCycleTransitionInput input, CancellationToken ct = default);
}

