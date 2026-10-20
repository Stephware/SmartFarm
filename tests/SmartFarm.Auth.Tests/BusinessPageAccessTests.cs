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

    public static TheoryData<string> ProtectedPages
    {
        get
        {
            var farmId = Guid.NewGuid();
            var cycleId = Guid.NewGuid();
            var itemId = Guid.NewGuid();
            var farmQuery = $"farmId={farmId}";
            var cycleQuery = $"{farmQuery}&cycleId={cycleId}";

            return new TheoryData<string>
            {
                { "/Notifications" },
                { $"/Tasks?{farmQuery}" },
                { "/Farms" },
                { "/Farms/Create" },
                { $"/Farms/Details/{farmId}" },
                { $"/Farms/Edit/{farmId}" },
                { $"/Crops?{farmQuery}" },
                { $"/Monitoring?{cycleQuery}" },
                { $"/Inventory?{farmQuery}" },
                { $"/Inventory/Details?{farmQuery}&itemId={itemId}" },
                { $"/Harvests?{cycleQuery}" }
            };
        }
    }

    [Theory]
    [MemberData(nameof(ProtectedPages))]
    public async Task BusinessPagesRequireWebLogin(string path)
    {
        using var response = await _client.GetAsync(path);

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/Account/Login", response.Headers.Location?.AbsolutePath);
    }
}
