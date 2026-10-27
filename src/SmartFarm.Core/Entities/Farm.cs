using System.Security.Cryptography;

namespace SmartFarm.Core.Entities;

public sealed class Farm
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid OwnerUserId { get; set; }
    public string JoinCode { get; set; } = Convert.ToHexString(RandomNumberGenerator.GetBytes(6));
    public string Name { get; set; } = string.Empty;
    public string LocationText { get; set; } = string.Empty;
    public decimal? Latitude { get; set; }
    public decimal? Longitude { get; set; }
    public string TimeZoneId { get; set; } = "Asia/Manila";
    public DateTime CreatedAtUtc { get; set; }
    public DateTime? ArchivedAtUtc { get; set; }
    public byte[] RowVersion { get; set; } = [];
}
