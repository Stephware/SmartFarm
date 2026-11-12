namespace SmartFarm.Core.Entities;

public sealed class SupportTicketMessage
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid TicketId { get; set; }
    public Guid AuthorUserId { get; set; }
    public string Body { get; set; } = string.Empty;
    public bool IsInternal { get; set; }
    public DateTime CreatedAtUtc { get; set; }
}
