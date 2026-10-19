using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface IPlotRepository
{
Task<Plot?> GetActiveAsync(Guid farmId, Guid plotId, CancellationToken ct = default);
    Task<Plot?> GetByIdAsync(Guid farmId, Guid plotId, CancellationToken ct = default);
    Task<IReadOnlyList<Plot>> ListActiveAsync(Guid farmId, CancellationToken ct = default);
    void Add(Plot plot);
    void SetOriginalRowVersion(Plot plot, byte[] rowVersion);
    Task<string> GetNameAsync(Guid plotId, CancellationToken ct = default);
    Task<bool> IsActiveAsync(Guid farmId, Guid plotId, CancellationToken ct = default);

}
