using SmartFarm.Core.Contracts.Monitoring;

namespace SmartFarm.Core.Interfaces.Monitoring;

public interface IFieldworkStockService
{
    Task<ActivityWithStockDetails> RecordAsync(Guid farmId, Guid cycleId, Guid actor,
        RecordActivityWithStockInput input, CancellationToken ct = default);
}
