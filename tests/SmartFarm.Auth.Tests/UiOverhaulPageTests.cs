using System.Net;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Auth.Tests;

/// <summary>Protects the accessible public UI and real authentication entry points.</summary>
public sealed class UiOverhaulPageTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;

    public UiOverhaulPageTests(WebApplicationFactory<Program> factory) =>
        _client = factory.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });

    [Theory]
    [InlineData("/", "Every farm, crop, and harvest in one place.")]
    [InlineData("/Account/Login", "Log in to AniGo")]
    [InlineData("/Account/Register", "Create your account")]
    public async Task PublicScreensKeepSkipLinkMainLandmarkAndAccessibleNavigation(
        string path, string expectedHeading)
    {
        using var response = await _client.GetAsync(path);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();

        Assert.Contains(expectedHeading, html);
        Assert.Contains("href=\"#main-content\"", html);
        Assert.Contains("id=\"main-content\"", html);
        Assert.Contains("tabindex=\"-1\"", html);
        Assert.Contains("aria-label=\"Main navigation\"", html);
        Assert.Contains("/css/site.css", html);
        Assert.Contains("/Account/Login", html);
        Assert.Contains("/Account/Register", html);

        // Avoid broken label associations and duplicate targets on public forms.
        var ids = Regex.Matches(html, "\\bid=\"([^\"]+)\"")
            .Select(m => m.Groups[1].Value).ToList();
        Assert.Equal(ids.Count, ids.Distinct().Count());
    }

    [Theory]
    [InlineData("/Account/Login", new[] { "Email", "Password" })]
    [InlineData("/Account/Register", new[] { "FullName", "Email", "Password" })]
    public async Task AuthFormsKeepTheirFieldsAndAntiforgeryProtection(
        string path, string[] fields)
    {
        var html = await _client.GetStringAsync(path);

        foreach (var field in fields)
        {
            Assert.Contains($"name=\"{field}\"", html);
            Assert.Contains($"id=\"{field}\"", html);
            Assert.Contains($"for=\"{field}\"", html);
        }

        Assert.Contains("name=\"__RequestVerificationToken\"", html);
        Assert.Contains("type=\"submit\"", html);
        Assert.Contains("type=\"password\"", html);
    }

    [Fact]
    public async Task PublicStartChoicesExplainOwnerAndWorkerApproval()
    {
        var landing = await _client.GetStringAsync("/");
        Assert.Contains("How would you like to use AniGo?", landing);
        Assert.Contains("I manage a farm", landing);
        Assert.Contains("I work on a farm", landing);
        Assert.Contains("wait for the owner to approve", landing);
        var registration = await _client.GetStringAsync("/Account/Register");
        Assert.Contains("Getting started", registration);
        Assert.Contains("A farm owner must approve join requests", registration);
        Assert.DoesNotContain("name=\"FarmCode\"", registration);
    }

    [Fact]
    public async Task PublicLayoutHasNoFarmPrivateNavigation()
    {
        var html = await _client.GetStringAsync("/");
        Assert.DoesNotContain("class=\"app-sidebar\"", html);
        Assert.DoesNotContain("class=\"farm-navigation\"", html);
        Assert.DoesNotContain(">Log out<", html);
    }
}
