using Microsoft.Extensions.DependencyInjection;
using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class ApiSmokeTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;
    private readonly IServiceScopeFactory _clientServiceScopeFactory;

    public ApiSmokeTests(WebApplicationFactory<Program> factory)
    {
        _clientServiceScopeFactory = factory.Services.GetRequiredService<IServiceScopeFactory>();
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
    public async Task AccountDetailsRequireBearerAuthentication()
    {
        using var response = await _client.GetAsync("/api/auth/me");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.Null(response.Headers.Location);
    }

    [Fact]
    public async Task InvalidRefreshTokenIsRejected()
    {
        using var request = new StringContent(
            "{\"refreshToken\":\"invalid-token\"}",
            System.Text.Encoding.UTF8, "application/json");
        using var response = await _client.PostAsync("/api/auth/refresh", request);
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task InvalidLoginIsRejectedBeforeDatabaseAccess()
    {
        using var request = new StringContent(
            "{\"email\":\"not-an-email\",\"password\":\"\"}",
            System.Text.Encoding.UTF8, "application/json");
        using var response = await _client.PostAsync("/api/auth/login", request);
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task ApiOnlyRegistersBearerAuthentication()
    {
        using var scope = _clientServiceScopeFactory.CreateScope();
        var schemes = scope.ServiceProvider
            .GetRequiredService<Microsoft.AspNetCore.Authentication.IAuthenticationSchemeProvider>();
        var all = await schemes.GetAllSchemesAsync();

        Assert.Contains(all, scheme => scheme.Name == Microsoft.AspNetCore.Identity.IdentityConstants.BearerScheme);
        Assert.DoesNotContain(all, scheme => scheme.Name == Microsoft.AspNetCore.Identity.IdentityConstants.ApplicationScheme);
        Assert.Equal(Microsoft.AspNetCore.Identity.IdentityConstants.BearerScheme,
            (await schemes.GetDefaultAuthenticateSchemeAsync())?.Name);
    }

    [Fact]
    public async Task FarmListRequiresBearerAuthentication()
    {
        using var response = await _client.GetAsync("/api/farms");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }
}
