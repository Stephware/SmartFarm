using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class BusinessPageAccessTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;

    public BusinessPageAccessTests(WebApplicationFactory<Program> factory)
    {
        _client = factory.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });
    }

    [Theory]
    [InlineData("/Farms")]
    [InlineData("/Farms/Create")]
    [InlineData("/Farms/Details/11111111-1111-1111-1111-111111111111")]
    [InlineData("/Farms/Edit/11111111-1111-1111-1111-111111111111")]
    [InlineData("/Crops?farmId=11111111-1111-1111-1111-111111111111")]
    [InlineData("/Monitoring?farmId=11111111-1111-1111-1111-111111111111&cycleId=33333333-3333-3333-3333-333333333333")]
    [InlineData("/Inventory?farmId=11111111-1111-1111-1111-111111111111")]
    [InlineData("/Inventory/Details?farmId=11111111-1111-1111-1111-111111111111&itemId=44444444-4444-4444-4444-444444444444")]
    [InlineData("/Harvests?farmId=11111111-1111-1111-1111-111111111111&cycleId=33333333-3333-3333-3333-333333333333")]
    public async Task BusinessPagesRequireWebLogin(string path)
    {
        using var response = await _client.GetAsync(path);

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.StartsWith("/Account/Login", response.Headers.Location?.ToString());
    }
}
