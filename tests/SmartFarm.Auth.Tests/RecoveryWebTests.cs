using System.Net;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class RecoveryWebTests
{
    [Theory]
    [InlineData("/Account/ForgotPassword", "Email")]
    [InlineData("/Account/ResetPassword?email=test%40example.test&token=not-real", "ConfirmPassword")]
    public async Task RecoveryPagesAreUsableFormsAndNeverCached(string getPath, string inputName)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });

        using var get = await client.GetAsync(getPath);
        Assert.Equal(HttpStatusCode.OK, get.StatusCode);
        Assert.Equal("text/html", get.Content.Headers.ContentType?.MediaType);
        Assert.True(get.Headers.CacheControl?.NoStore);
        Assert.Equal("no-referrer", Assert.Single(get.Headers.GetValues("Referrer-Policy")));
        var html = await get.Content.ReadAsStringAsync();
        Assert.Contains("<form", html);
        Assert.Contains("name=\"__RequestVerificationToken\"", html);
        Assert.Contains("name=\"" + inputName + "\"", html);
        Assert.Contains("method=\"post\"", html);

        // A browser form still requires antiforgery for both write actions.
        var postPath = getPath.Split('?')[0];
        using var post = await client.PostAsync(postPath,
            new FormUrlEncodedContent(new Dictionary<string, string> { ["Email"] = "test@example.test" }));
        Assert.Equal(HttpStatusCode.BadRequest, post.StatusCode);
    }

    [Fact]
    public async Task ResetWithoutTokenShowsHelpfulRecoveryErrorWithoutAForm()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        using var get = await client.GetAsync("/Account/ResetPassword");
        Assert.Equal(HttpStatusCode.OK, get.StatusCode);
        var html = await get.Content.ReadAsStringAsync();
        Assert.Contains("Invalid reset link", html);
        Assert.DoesNotContain("name=\"Password\"", html);
    }

    [Fact]
    public async Task UnconfirmedEmailIsNotExposedOnNeutralRequestConfirmation()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Account/ForgotPassword/Sent");
        Assert.Contains("cannot confirm whether an account exists", html);
        Assert.DoesNotContain("test@example.test", html);
    }
}
