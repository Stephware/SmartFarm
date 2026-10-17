using Microsoft.EntityFrameworkCore;

namespace SmartFarm.Infrastructure.Persistence;

// Entity mappings will be added after alignment with the existing SQL schema.
public sealed class SmartFarmDbContext(DbContextOptions<SmartFarmDbContext> options)
    : DbContext(options)
{
}
