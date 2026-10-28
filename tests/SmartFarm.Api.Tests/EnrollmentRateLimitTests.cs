using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class EnrollmentRateLimitTests
{
    [Theory]
    [InlineData("/api/auth/register", 5)]
    [InlineData("/api/auth/login", 20)]
    public async Task AuthenticationAttemptsAreLimitedByIpAndIgnoreForwardedHeaders(string path, int permits)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        for (var attempt = 0; attempt < permits; attempt++)
        {
            using var response = await client.PostAsJsonAsync(path, new { });
            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }

        client.DefaultRequestHeaders.Add("X-Forwarded-For", "198.51.100.88");
        using var rejected = await client.PostAsJsonAsync(path, new { });
        Assert.Equal(HttpStatusCode.TooManyRequests, rejected.StatusCode);
        Assert.True(rejected.Headers.RetryAfter?.Delta > TimeSpan.Zero);
        Assert.Contains("message", await rejected.Content.ReadAsStringAsync());

        using var body = new MemoryStream("{}"u8.ToArray());
        var otherIp = await app.Server.SendAsync(context =>
        {
            context.Connection.RemoteIpAddress = IPAddress.Parse("198.51.100.89");
            context.Request.Method = "POST";
            context.Request.Path = path;
            context.Request.ContentType = "application/json";
            context.Request.ContentLength = body.Length;
            context.Request.Body = body;
        });
        Assert.Equal(400, otherIp.Response.StatusCode);

        using var status = await client.GetAsync("/api/status");
        Assert.Equal(HttpStatusCode.OK, status.StatusCode);
        var other = path.EndsWith("register") ? "/api/auth/login" : "/api/auth/register";
        using var separatePolicy = await client.PostAsJsonAsync(other, new { });
        Assert.Equal(HttpStatusCode.BadRequest, separatePolicy.StatusCode);
    }

    [Fact]
    public async Task EnrollmentWritesShareUserBudgetWhileOtherUsersAndAnonymousChallengesStayIndependent()
    {
        using var app = new WebApplicationFactory<Program>();
        using var first = SignedIn(app, Guid.NewGuid());
        for (var attempt = 0; attempt < 10; attempt++)
        {
            using var response = await first.PostAsJsonAsync("/api/farm-join-requests", new { });
            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }
        using var rejected = await first.PostAsJsonAsync("/api/farm-join-requests", new { });
        Assert.Equal(HttpStatusCode.TooManyRequests, rejected.StatusCode);
        Assert.True(rejected.Headers.RetryAfter?.Delta > TimeSpan.Zero);

        var farm = Guid.NewGuid();
        var request = Guid.NewGuid();
        using var cancel = await first.PostAsync($"/api/farms/{farm}/join-requests/{request}/cancel", null);
        Assert.Equal(HttpStatusCode.TooManyRequests, cancel.StatusCode);
        using var regenerate = await first.PostAsJsonAsync($"/api/farms/{farm}/join-code/regenerate", new { currentCode = "ABCD12345678" });
        Assert.Equal(HttpStatusCode.TooManyRequests, regenerate.StatusCode);
        using var decision = await first.PostAsJsonAsync($"/api/farms/{farm}/join-requests/{request}/decision", new { approve = true });
        Assert.Equal(HttpStatusCode.TooManyRequests, decision.StatusCode);

        using var second = SignedIn(app, Guid.NewGuid());
        using var allowed = await second.PostAsJsonAsync("/api/farm-join-requests", new { });
        Assert.Equal(HttpStatusCode.BadRequest, allowed.StatusCode);
        using var anonymous = app.CreateClient();
        using var challenge = await anonymous.PostAsJsonAsync("/api/farm-join-requests", new { });
        Assert.Equal(HttpStatusCode.Unauthorized, challenge.StatusCode);
    }

    private static HttpClient SignedIn(WebApplicationFactory<Program> app, Guid user)
    {
        var options = app.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>()
            .Get(IdentityConstants.BearerScheme);
        var principal = new ClaimsPrincipal(new ClaimsIdentity(
            [new Claim(ClaimTypes.NameIdentifier, user.ToString())], IdentityConstants.BearerScheme));
        var ticket = new AuthenticationTicket(principal, new AuthenticationProperties
        {
            ExpiresUtc = DateTimeOffset.UtcNow.AddMinutes(5)
        }, IdentityConstants.BearerScheme);
        var client = app.CreateClient();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer",
            options.BearerTokenProtector.Protect(ticket));
        return client;
    }
}
