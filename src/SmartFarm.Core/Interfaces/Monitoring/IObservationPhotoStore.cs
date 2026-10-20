namespace SmartFarm.Core.Interfaces.Monitoring;

public interface IObservationPhotoStore
{
    Task<string> SaveAsync(byte[] bytes, string extension, CancellationToken ct = default);
    Task<Stream> OpenAsync(string storageKey, CancellationToken ct = default);
    Task DeleteAsync(string storageKey);
}

