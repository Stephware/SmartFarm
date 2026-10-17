using Microsoft.AspNetCore.Identity;

namespace SmartFarm.Infrastructure.Identity;

public sealed class SmartFarmRole : IdentityRole<Guid>
{
    public SmartFarmRole() => Id = Guid.NewGuid();
}
