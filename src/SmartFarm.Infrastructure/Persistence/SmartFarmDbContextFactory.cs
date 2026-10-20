using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace SmartFarm.Infrastructure.Persistence;

// Tooling builds the model offline. No credentials or connection are required.
public sealed class SmartFarmDbContextFactory : IDesignTimeDbContextFactory<SmartFarmDbContext>
{
    public SmartFarmDbContext CreateDbContext(string[] args) => new(
        new DbContextOptionsBuilder<SmartFarmDbContext>().UseSqlServer().Options);
}
