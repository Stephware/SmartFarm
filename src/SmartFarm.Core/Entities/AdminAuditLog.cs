namespace SmartFarm.Core.Entities;

// Write-only security events. Do not store passwords, OTPs, tokens or private farm data.
public sealed class AdminAuditLog
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid? ActorUserId { get; set; }
    public string ActorSource { get; set; } = "web";
    public string Action { get; set; } = string.Empty;
    public string TargetType { get; set; } = string.Empty;
    public Guid? TargetId { get; set; }
    public string? Reason { get; set; }
    public DateTime OccurredAtUtc { get; set; } = DateTime.UtcNow;
}
