using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Api.Tests;

// Each route must reject anonymous requests before accessing farm data.
public sealed class BusinessEndpointTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;

    public BusinessEndpointTests(WebApplicationFactory<Program> factory)
    {
        _client = factory.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });
    }

    public static TheoryData<string, string> ProtectedRoutes => new()
    {
        { "GET", "/api/auth/me" },
        { "GET", "/api/farms" },
        { "POST", "/api/farms" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111" },
        { "PUT", "/api/farms/11111111-1111-1111-1111-111111111111" },
        { "DELETE", "/api/farms/11111111-1111-1111-1111-111111111111" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/plots" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/plots" },
        { "PUT", "/api/farms/11111111-1111-1111-1111-111111111111/plots/22222222-2222-2222-2222-222222222222" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/access" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/members" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/members/invitations" },
        { "POST", "/api/farm-invitations/accept" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/crop-types" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/crop-types" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/plots/22222222-2222-2222-2222-222222222222/crop-cycles" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles/33333333-3333-3333-3333-333333333333/transitions" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles/33333333-3333-3333-3333-333333333333/monitoring" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles/33333333-3333-3333-3333-333333333333/monitoring/activities" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles/33333333-3333-3333-3333-333333333333/monitoring/observations" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/inventory" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/inventory" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/inventory/44444444-4444-4444-4444-444444444444/transactions" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/inventory/44444444-4444-4444-4444-444444444444/transactions/55555555-5555-5555-5555-555555555555/reverse" },
        { "GET", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles/33333333-3333-3333-3333-333333333333/harvests" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles/33333333-3333-3333-3333-333333333333/harvests" },
        { "POST", "/api/farms/11111111-1111-1111-1111-111111111111/crop-cycles/33333333-3333-3333-3333-333333333333/harvests/66666666-6666-6666-6666-666666666666/void" }
    };

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
