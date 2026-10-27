using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class HomePageSmokeTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;

    public HomePageSmokeTests(WebApplicationFactory<Program> factory) =>
        _client = factory.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });

    [Fact]
    public async Task PublicHomepageShowsUsableNavigationWithoutBrokenApiLink()
    {
        using var response = await _client.GetAsync("/");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("Keep your farm organized", html);
        Assert.Contains("AniGo", html);
        Assert.DoesNotContain(">SmartFarm</a>", html);
        Assert.Contains("/Account/Login", html);
        Assert.Contains("/Account/Register", html);
        Assert.DoesNotContain("href=\"/api/status\"", html);
        Assert.DoesNotContain("Development preview", html);
    }
}
