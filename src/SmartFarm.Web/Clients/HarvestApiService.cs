using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Interfaces.Harvests;

namespace SmartFarm.Web.Clients;

public sealed class HarvestApiService : IHarvestService
{
    private readonly SmartFarmApiClient _api;

    public HarvestApiService(SmartFarmApiClient api)
    {
        _api = api;
    }

    private static string Entries(Guid farmId, Guid cycleId) =>
        $"api/farms/{farmId}/crop-cycles/{cycleId}/harvests";

    public Task<HarvestOverview> GetAsync(Guid farmId, Guid cycleId, Guid actor,
        CancellationToken ct = default) =>
        _api.ReadAsync<HarvestOverview>(Entries(farmId, cycleId), actor, ct);

    public Task<HarvestEntry> RecordAsync(Guid farmId, Guid cycleId, Guid actor,
        RecordHarvestInput input, CancellationToken ct = default) =>
        _api.WriteAsync<HarvestEntry>(HttpMethod.Post, Entries(farmId, cycleId), actor, input, ct);

    public Task<HarvestEntry> VoidAsync(Guid farmId, Guid cycleId, Guid harvestId, Guid actor,
        VoidHarvestInput input, CancellationToken ct = default) =>
        _api.WriteAsync<HarvestEntry>(HttpMethod.Post,
            $"{Entries(farmId, cycleId)}/{harvestId}/void", actor, input, ct);
}
