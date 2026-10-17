namespace SmartFarm.Core.Entities;

public sealed class SyncOperation
{
    public Guid OperationId { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public Guid UserId { get; set; }
    public Guid? DeviceId { get; set; }
    public string OperationType { get; set; } = string.Empty;
    public Guid? TargetEntityId { get; set; }
    public byte[] RequestHash { get; set; } = [];
    public byte Status { get; set; }
    public string? ResultJson { get; set; }
    public string? ErrorCode { get; set; }
    public DateTime ReceivedAtUtc { get; set; }
    public DateTime? CompletedAtUtc { get; set; }
}
