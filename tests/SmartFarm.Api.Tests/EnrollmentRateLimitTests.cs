using Microsoft.AspNetCore.Hosting;
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
    [InlineData("/api/auth/forgot-password", 5)]
    [InlineData("/api/auth/reset-password", 10)]
    public async Task AuthenticationBudgetsRejectSpoofingAndSeparateProxiedClients(string path, int permits)
    {
        using var app = new WebApplicationFactory<Program>();
        // TestServer otherwise has no peer address. Model an actual untrusted TCP
        // peer so the forwarding trust boundary is exercised, as it is in production.
        using var client = new HttpClient(app.Server.CreateHandler(context =>
            context.Connection.RemoteIpAddress = IPAddress.Parse("203.0.113.10")))
        { BaseAddress = new Uri("http://localhost") };
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

        // Caddy's trusted loopback connection must retain separate client budgets.
        using var proxy = new HttpClient(app.Server.CreateHandler(context =>
            context.Connection.RemoteIpAddress = IPAddress.Loopback))
        { BaseAddress = new Uri("http://localhost") };
        proxy.DefaultRequestHeaders.Add("X-Forwarded-For", "198.51.100.120");
        for (var attempt = 0; attempt < permits; attempt++)
        {
            using var response = await proxy.PostAsJsonAsync(path, new { });
            Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        }
        using var proxyRejected = await proxy.PostAsJsonAsync(path, new { });
        Assert.Equal(HttpStatusCode.TooManyRequests, proxyRejected.StatusCode);
        proxy.DefaultRequestHeaders.Remove("X-Forwarded-For");
        proxy.DefaultRequestHeaders.Add("X-Forwarded-For", "198.51.100.121");
        using var anotherProxiedClient = await proxy.PostAsJsonAsync(path, new { });
        Assert.Equal(HttpStatusCode.BadRequest, anotherProxiedClient.StatusCode);
    }

    [Fact]
    public async Task EnrollmentWritesShareUserBudgetWhileOtherUsersAndAnonymousChallengesStayIndependent()
    {
        using var app = new WebApplicationFactory<Program>().WithWebHostBuilder(builder => builder.ConfigureServices(services =>
            services.AddScoped<SignInManager<SmartFarm.Infrastructure.Identity.SmartFarmUser>, RateLimitSignInManager>()));
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
    // These tests isolate rate limiting. Real stamp validation is covered by SQL recovery tests.
    private sealed class RateLimitSignInManager(
        UserManager<SmartFarm.Infrastructure.Identity.SmartFarmUser> users,
        Microsoft.AspNetCore.Http.IHttpContextAccessor accessor,
        IUserClaimsPrincipalFactory<SmartFarm.Infrastructure.Identity.SmartFarmUser> claims,
        IOptions<IdentityOptions> options,
        Microsoft.Extensions.Logging.ILogger<SignInManager<SmartFarm.Infrastructure.Identity.SmartFarmUser>> logger,
        IAuthenticationSchemeProvider schemes,
        IUserConfirmation<SmartFarm.Infrastructure.Identity.SmartFarmUser> confirmation)
        : SignInManager<SmartFarm.Infrastructure.Identity.SmartFarmUser>(users, accessor, claims, options, logger, schemes, confirmation)
    {
        public override Task<SmartFarm.Infrastructure.Identity.SmartFarmUser?> ValidateSecurityStampAsync(ClaimsPrincipal? principal) =>
            Task.FromResult<SmartFarm.Infrastructure.Identity.SmartFarmUser?>(new() { Id = Guid.Parse(principal!.FindFirstValue(ClaimTypes.NameIdentifier)!) });
    }

}
