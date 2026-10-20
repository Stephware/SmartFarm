using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Interfaces.Repositories;

public interface ICropTypeRepository
{
Task<CropType?> GetActiveAsync(Guid cropTypeId, CancellationToken ct = default);
    Task<IReadOnlyList<CropType>> ListActiveAsync(CancellationToken ct = default);
    Task<bool> NameExistsAsync(string name, CancellationToken ct = default);
    void Add(CropType cropType);
    Task<string> GetNameAsync(Guid cropTypeId, CancellationToken ct = default);

}
