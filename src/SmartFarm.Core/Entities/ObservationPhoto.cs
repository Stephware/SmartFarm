namespace SmartFarm.Core.Entities;

public sealed class ObservationPhoto
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CropObservationId { get; set; }
    public string StorageKey { get; set; } = string.Empty;
    public string ContentType { get; set; } = string.Empty;
    public long FileSizeBytes { get; set; }
    public DateTime UploadedAtUtc { get; set; }
    public Guid UploadedByUserId { get; set; }
}
