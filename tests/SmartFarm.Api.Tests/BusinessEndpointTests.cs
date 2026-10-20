using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Api.Tests;

// Anonymous users must not access farm data or business operations.
public sealed class BusinessEndpointTests : IClassFixture<WebApplicationFactory<Program>>
{
    private static readonly Guid FarmId = Guid.NewGuid();
    private static readonly Guid PlotId = Guid.NewGuid();
    private static readonly Guid CycleId = Guid.NewGuid();
    private static readonly Guid ItemId = Guid.NewGuid();
    private static readonly Guid TransactionId = Guid.NewGuid();
    private static readonly Guid HarvestId = Guid.NewGuid();

    private readonly HttpClient _client;

    public BusinessEndpointTests(WebApplicationFactory<Program> factory)
    {
        _client = factory.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });
    }

    public static TheoryData<string, string> ProtectedRoutes
    {
        get
        {
            const string farms = "/api/farms";
            var farm = $"{farms}/{FarmId}";
            var plots = $"{farm}/plots";
            var cropCycles = $"{farm}/crop-cycles";
            var crop = $"{cropCycles}/{CycleId}";
            var monitoring = $"{crop}/monitoring";
            var inventory = $"{farm}/inventory";
            var item = $"{inventory}/{ItemId}";
            var harvests = $"{crop}/harvests";

            return new TheoryData<string, string>
            {
                { "GET", "/api/auth/me" },
                { "POST", $"/api/sync/operations/{Guid.NewGuid()}" },
                { "GET", farms },
                { "POST", farms },
                { "GET", "/api/notifications" },
                { "PUT", $"/api/notifications/{Guid.NewGuid()}/read" },
                { "GET", $"{farm}/tasks" },
                { "POST", $"{farm}/tasks" },
                { "PUT", $"{farm}/tasks/{Guid.NewGuid()}" },
                { "PUT", $"{farm}/tasks/{Guid.NewGuid()}/close" },
                { "GET", farm },
                { "GET", $"{farm}/weather" },
                { "GET", $"{farm}/dashboard" },
                { "GET", $"{farm}/reports/harvests" },
                { "PUT", farm },
                { "DELETE", farm },
                { "GET", plots },
                { "POST", plots },
                { "PUT", $"{plots}/{PlotId}" },
                { "GET", $"{farm}/access" },
                { "GET", $"{farm}/members" },
                { "POST", $"{farm}/members/invitations" },
                { "POST", "/api/farm-invitations/accept" },
                { "GET", $"{farm}/crop-types" },
                { "POST", $"{farm}/crop-types" },
                { "GET", cropCycles },
                { "POST", $"{plots}/{PlotId}/crop-cycles" },
                { "POST", $"{crop}/transitions" },
                { "GET", monitoring },
                { "POST", $"{monitoring}/activities" },
                { "POST", $"{monitoring}/observations" },
                { "PUT", $"{monitoring}/observations/{Guid.NewGuid()}/photos/{Guid.NewGuid()}" },
                { "GET", inventory },
                { "POST", inventory },
                { "POST", $"{item}/transactions" },
                { "POST", $"{item}/transactions/{TransactionId}/reverse" },
                { "GET", harvests },
                { "POST", harvests },
                { "POST", $"{harvests}/{HarvestId}/void" }
            };
        }
    }

    [Theory]
    [MemberData(nameof(ProtectedRoutes))]
    public async Task FarmRoutesRejectAnonymousRequests(string method, string path)
    {
        using var request = new HttpRequestMessage(new HttpMethod(method), path);
        using var response = await _client.SendAsync(request);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.Null(response.Headers.Location);
    }
}
