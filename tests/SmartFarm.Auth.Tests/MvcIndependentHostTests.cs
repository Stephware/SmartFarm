using System.Net;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class MvcIndependentHostTests
{
    [Fact]
    public async Task PublicMvcHomepageWorksWithoutAnApiEndpoint()
    {
        // The MVC host must not depend on a running/configured Web API.
        using var app = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            builder.ConfigureAppConfiguration((_, config) =>
                config.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["SmartFarmApi:BaseUrl"] = "not-a-valid-api-address"
                })));

        using var client = app.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });
        using var response = await client.GetAsync("/");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }
}
