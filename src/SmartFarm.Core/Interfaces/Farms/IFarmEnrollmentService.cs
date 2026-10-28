using SmartFarm.Core.Contracts.Farms;

namespace SmartFarm.Core.Interfaces.Farms;

public interface IFarmEnrollmentService
{
    Task<FarmJoinRequestDetails> RequestAsync(Guid actor, string code, CancellationToken ct = default);
    Task<IReadOnlyList<FarmJoinRequestDetails>> MyRequestsAsync(Guid actor, CancellationToken ct = default);
    Task<FarmEnrollmentOverview> OwnerOverviewAsync(Guid farmId, Guid actor, CancellationToken ct = default);
    Task DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, CancellationToken ct = default);
    Task CancelAsync(Guid farmId, Guid requestId, Guid actor, CancellationToken ct = default);
    Task<string> RegenerateCodeAsync(Guid farmId, Guid actor, string currentCode, CancellationToken ct = default);
}
