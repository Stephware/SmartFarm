using System.Net;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class FarmEnrollmentEndpointTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient client;
    public FarmEnrollmentEndpointTests(WebApplicationFactory<Program> factory) => client = factory.CreateClient();

    [Theory]
    [InlineData("/api/farm-invitations/accept")]
    [InlineData("/api/farms/11111111-1111-1111-1111-111111111111/members/invitations")]
    public async Task OldInvitationEndpointsAreGone(string path)
    {
        using var response = await client.PostAsJsonAsync(path, new { token = "old-token", email = "farmer@example.test", role = 1 });
        Assert.Equal(HttpStatusCode.Gone, response.StatusCode);
        Assert.Contains("approval", await response.Content.ReadAsStringAsync());
    }

    [Theory]
    [InlineData("GET", "/api/farm-join-requests")]
    [InlineData("POST", "/api/farm-join-requests")]
    [InlineData("GET", "/api/farms/11111111-1111-1111-1111-111111111111/join-requests")]
    [InlineData("POST", "/api/farms/11111111-1111-1111-1111-111111111111/join-requests/22222222-2222-2222-2222-222222222222/decision")]
    public async Task EnrollmentRequiresBearerAuthentication(string method, string path)
    {
        using var request = new HttpRequestMessage(new HttpMethod(method), path);
        using var response = await client.SendAsync(request);
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }
}
