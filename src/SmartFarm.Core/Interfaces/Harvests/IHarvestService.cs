using SmartFarm.Core.Contracts.Harvests;

namespace SmartFarm.Core.Interfaces.Harvests;

public interface IHarvestService
{
    Task<HarvestOverview> GetAsync(Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default);
    Task<HarvestEntry> RecordAsync(Guid farmId, Guid cycleId, Guid actor, RecordHarvestInput input, CancellationToken ct = default);
    Task<HarvestEntry> VoidAsync(Guid farmId, Guid cycleId, Guid harvestId, Guid actor, VoidHarvestInput input, CancellationToken ct = default);
}

