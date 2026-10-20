using System.Net;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
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
            builder.ConfigureAppConfiguration((_, configuration) =>
                configuration.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["ConnectionStrings:SmartFarm"] = ""
                }));
        });
        using var client = factory.CreateClient();
        using var response = await client.GetAsync("/health/database");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        var text = await response.Content.ReadAsStringAsync();
        Assert.Contains("ConnectionStrings:SmartFarm", text);
        Assert.DoesNotContain("Password=", text);
    }
}
