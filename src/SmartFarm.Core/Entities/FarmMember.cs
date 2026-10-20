namespace SmartFarm.Core.Entities;

public sealed class FarmMember
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid FarmId { get; set; }
    public Guid UserId { get; set; }
    public byte Role { get; set; }
    public DateTime JoinedAtUtc { get; set; }
    public DateTime? RemovedAtUtc { get; set; }
}
