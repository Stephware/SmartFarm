using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Interfaces.Harvests;

namespace SmartFarm.Web.Clients;

public sealed class HarvestApiService(SmartFarmApiClient api) : IHarvestService
{
    private static string Entries(Guid farmId, Guid cycleId) =>
        $"api/farms/{farmId}/crop-cycles/{cycleId}/harvests";

    public Task<HarvestOverview> GetAsync(Guid farmId, Guid cycleId, Guid actor,
        CancellationToken ct = default) =>
        api.ReadAsync<HarvestOverview>(Entries(farmId, cycleId), actor, ct);

    public Task<HarvestEntry> RecordAsync(Guid farmId, Guid cycleId, Guid actor,
        RecordHarvestInput input, CancellationToken ct = default) =>
        api.WriteAsync<HarvestEntry>(HttpMethod.Post, Entries(farmId, cycleId), actor, input, ct);

    public Task<HarvestEntry> VoidAsync(Guid farmId, Guid cycleId, Guid harvestId, Guid actor,
        VoidHarvestInput input, CancellationToken ct = default) =>
        api.WriteAsync<HarvestEntry>(HttpMethod.Post,
            $"{Entries(farmId, cycleId)}/{harvestId}/void", actor, input, ct);
}
