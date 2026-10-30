using SmartFarm.Core.Contracts.Finance;

namespace SmartFarm.Core.Interfaces.Finance;

public interface IFarmFinanceService
{
    Task<FarmFinanceOverview> ListAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task<FarmFinanceEntryDetails> RecordAsync(Guid farmId, Guid actor, FarmFinanceInput input,
        CancellationToken ct = default);
    Task VoidAsync(Guid farmId, Guid entryId, Guid actor, FarmFinanceVoidInput input,
        CancellationToken ct = default);
}
