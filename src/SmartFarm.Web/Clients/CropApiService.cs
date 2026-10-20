using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Interfaces.Crops;

namespace SmartFarm.Web.Clients;

public sealed class CropApiService : ICropManagementService
{
    private readonly SmartFarmApiClient _api;

    public CropApiService(SmartFarmApiClient api)
    {
        _api = api;
    }

    private static string Farm(Guid farmId) => $"api/farms/{farmId}";

    public async Task<IReadOnlyList<CropTypeDetails>> ListCropTypesAsync(Guid farmId, Guid actor,
        CancellationToken ct = default) =>
        await _api.ReadAsync<List<CropTypeDetails>>($"{Farm(farmId)}/crop-types", actor, ct);

    public Task<CropTypeDetails> CreateCropTypeAsync(Guid farmId, Guid actor, CropTypeInput input,
        CancellationToken ct = default) =>
        _api.WriteAsync<CropTypeDetails>(HttpMethod.Post, $"{Farm(farmId)}/crop-types", actor, input, ct);

    public async Task<IReadOnlyList<CropCycleDetails>> ListCyclesAsync(Guid farmId, Guid actor,
        CancellationToken ct = default) =>
        await _api.ReadAsync<List<CropCycleDetails>>($"{Farm(farmId)}/crop-cycles", actor, ct);

    public Task<CropCycleDetails?> GetCycleAsync(Guid farmId, Guid cycleId, Guid actor,
        CancellationToken ct = default) =>
        _api.ReadOptionalAsync<CropCycleDetails>($"{Farm(farmId)}/crop-cycles/{cycleId}", actor, ct);

    public Task<CropCycleDetails> PlanAsync(Guid farmId, Guid plotId, Guid actor, CropCyclePlanInput input,
        CancellationToken ct = default) =>
        _api.WriteAsync<CropCycleDetails>(HttpMethod.Post, $"{Farm(farmId)}/plots/{plotId}/crop-cycles", actor, input, ct);

    public Task<CropCycleDetails> UpdatePlanAsync(Guid farmId, Guid cycleId, Guid actor, CropCyclePlanInput input,
        CancellationToken ct = default) =>
        _api.WriteAsync<CropCycleDetails>(HttpMethod.Put, $"{Farm(farmId)}/crop-cycles/{cycleId}", actor, input, ct);

    public Task<CropCycleDetails> TransitionAsync(Guid farmId, Guid cycleId, Guid actor, CropCycleTransitionInput input,
        CancellationToken ct = default) =>
        _api.WriteAsync<CropCycleDetails>(HttpMethod.Post, $"{Farm(farmId)}/crop-cycles/{cycleId}/transitions", actor, input, ct);
}
