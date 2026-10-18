namespace SmartFarm.Core.Services.Monitoring;

public static class MonitoringRules
{
    public const int MaxPhotoBytes = 5 * 1024 * 1024;

    public static bool ValidActivity(byte kind) => kind is >= 1 and <= 5;
    public static bool ValidHealth(byte health) => health <= 3;
    public static bool CanRecord(byte cropStatus) => cropStatus == 1;

    // Content type alone is untrusted. Accept only recognized file signatures
    // and match the declared type to prevent arbitrary executable uploads.
    public static string? PhotoExtension(ReadOnlySpan<byte> data, string? mime) =>
        mime?.Trim().ToLowerInvariant() switch
        {
            "image/jpeg" when data.Length >= 3 &&
                data[0] == 0xff && data[1] == 0xd8 && data[2] == 0xff => ".jpg",
            "image/png" when data.Length >= 8 &&
                data[..8].SequenceEqual(new byte[] {137,80,78,71,13,10,26,10}) => ".png",
            "image/webp" when data.Length >= 12 &&
                data[..4].SequenceEqual("RIFF"u8) &&
                data.Slice(8,4).SequenceEqual("WEBP"u8) => ".webp",
            _ => null
        };
}

