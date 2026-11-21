namespace SmartFarm.Core.Entities;

// A platform support case, separate from crop-health issues and farm permissions.
public sealed class SupportTicket
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid UserId { get; set; }
    public Guid? FarmId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Category { get; set; } = "Other";
    public string Status { get; set; } = "Open";
    public string Priority { get; set; } = "Normal";
    public DateTime CreatedAtUtc { get; set; }
    public DateTime UpdatedAtUtc { get; set; }
    public DateTime? ResolvedAtUtc { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
