using SmartFarm.Core.Interfaces.Monitoring;

namespace SmartFarm.Infrastructure.Services.Monitoring;

// Local, private storage (outside wwwroot). Configure shared durable storage
// before using multiple production instances; files are not served statically.
public sealed class LocalObservationPhotoStore(string root) : IObservationPhotoStore
{
    private readonly string _directory = Path.GetFullPath(root);

    private static bool IsSafeKey(string key)
    {
        if (key.Length is not (36 or 37)) return false; // 32 hex chars + extension
        var dot = key.IndexOf('.');
        return dot == 32 &&
            key[..32].All(Uri.IsHexDigit) &&
            key[dot..] is ".jpg" or ".png" or ".webp";
    }

    private string FilePath(string key) =>
        IsSafeKey(key) ? Path.Combine(_directory, key) :
        throw new FileNotFoundException("Photo was not found.");

    public async Task<string> SaveAsync(byte[] bytes, string extension, CancellationToken ct = default)
    {
        if (extension is not (".jpg" or ".png" or ".webp"))
            throw new ArgumentException("Unsupported photo extension.", nameof(extension));
        Directory.CreateDirectory(_directory);
        var key = Guid.NewGuid().ToString("N") + extension;
        var full = FilePath(key);
        await using var output = new FileStream(full, FileMode.CreateNew,
            FileAccess.Write, FileShare.None, 81920, useAsync: true);
        try { await output.WriteAsync(bytes, ct); }
        catch
        {
            await output.DisposeAsync();
            File.Delete(full);
            throw;
        }
        return key;
    }

    public Task<Stream> OpenAsync(string storageKey, CancellationToken ct = default) =>
        Task.FromResult<Stream>(new FileStream(FilePath(storageKey),
            FileMode.Open, FileAccess.Read, FileShare.Read, 81920, useAsync: true));

    public Task DeleteAsync(string storageKey)
    {
        File.Delete(FilePath(storageKey));
        return Task.CompletedTask;
    }
}

