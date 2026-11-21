namespace SmartFarm.Core.Entities;

// A global, text-only announcement. It is not a per-farm task or a push notification.
public sealed class PlatformAnnouncement
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid CreatedByUserId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Body { get; set; } = string.Empty;
    public string Category { get; set; } = "General";
    public string State { get; set; } = "Draft";
    public DateTime CreatedAtUtc { get; set; }
    public DateTime UpdatedAtUtc { get; set; }
    public DateTime? PublishedAtUtc { get; set; }
    public DateTime? ExpiresAtUtc { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
