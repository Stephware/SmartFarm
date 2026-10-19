using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class ApiSmokeTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;

    public ApiSmokeTests(WebApplicationFactory<Program> factory)
    {
        _client = factory.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });
    }

    [Fact]
    public async Task StatusWorksWithoutDatabase()
    {
        using var response = await _client.GetAsync("/api/status");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Contains("\"status\":\"ok\"", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task FarmListRequiresBearerAuthentication()
    {
        using var response = await _client.GetAsync("/api/farms");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }
}
