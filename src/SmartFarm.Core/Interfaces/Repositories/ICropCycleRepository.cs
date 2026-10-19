using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface ICropCycleRepository
{
Task<CropCycle?> GetForFarmAsync(Guid farmId, Guid cycleId, bool requireActivePlot,
        CancellationToken ct = default);
    Task<IReadOnlyList<CropCycle>> ListForFarmAsync(Guid farmId, CancellationToken ct = default);
    Task<bool> HasStatusOnPlotAsync(Guid plotId, byte status, CancellationToken ct = default);
    void Add(CropCycle cycle);
    void SetOriginalRowVersion(CropCycle cycle, byte[] rowVersion);
}
