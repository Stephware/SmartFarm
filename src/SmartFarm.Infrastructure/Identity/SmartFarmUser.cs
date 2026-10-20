using Microsoft.AspNetCore.Identity;

namespace SmartFarm.Infrastructure.Identity;

public sealed class SmartFarmUser : IdentityUser<Guid>
{
    public SmartFarmUser() => Id = Guid.NewGuid();

    public string FullName { get; set; } = string.Empty;
    public DateTime? DisabledAtUtc { get; set; }
}
