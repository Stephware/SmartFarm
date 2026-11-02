using System.Net;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class DatabaseHealthTests
{
    [Fact]
    public async Task DevelopmentHealthExplainsMissingDatabaseConnection()
    {
        using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.UseEnvironment("Development");
            // Local user-secrets fallback must not supply a real database to this test.
            builder.ConfigureServices(services => services.AddScoped(_ =>
                new SmartFarmDbContext(new DbContextOptionsBuilder<SmartFarmDbContext>()
                    .UseSqlServer().Options)));
        });
        using var client = factory.CreateClient();
        using var response = await client.GetAsync("/health/database");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        var text = await response.Content.ReadAsStringAsync();
        Assert.Contains("ConnectionStrings:SmartFarm", text);
        Assert.DoesNotContain("Password=", text);
    }
}
