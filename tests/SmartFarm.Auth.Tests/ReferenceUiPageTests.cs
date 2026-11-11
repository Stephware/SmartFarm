using System.Net;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class ReferenceUiPageTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient _client;

    public ReferenceUiPageTests(WebApplicationFactory<Program> factory) =>
        _client = factory.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });

    [Theory]
    [InlineData("/")]
    [InlineData("/Account/Login")]
    [InlineData("/Account/Register")]
    public async Task PublicPagesApplyTheReferenceThemeAfterSharedStyles(string path)
    {
        using var response = await _client.GetAsync(path);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        var styles = Regex.Matches(html, "<link\\b[^>]*rel=\"stylesheet\"[^>]*href=\"([^\"]+)\"")
            .Select(match => match.Groups[1].Value.Split('?')[0]).ToArray();
        Assert.NotEmpty(styles);
        Assert.Equal("/css/reference-ui.css", styles[^1]);
        Assert.Contains("/css/site.css", styles);
    }

    [Theory]
    [InlineData("/")]
    [InlineData("/Account/Login")]
    [InlineData("/Account/Register")]
    public async Task PublicPagesOfferAnAccessibleNativeThemeButton(string path)
    {
        var html = await _client.GetStringAsync(path);
        var buttons = Regex.Matches(html, "<button\\b[^>]*data-color-mode-toggle[^>]*>")
            .Select(match => match.Value).ToArray();
        Assert.NotEmpty(buttons);
        Assert.All(buttons, button =>
        {
            Assert.Contains("type=\"button\"", button);
            Assert.Contains("aria-pressed=\"false\"", button);
            Assert.Matches("aria-label=\"[^\"]+\"", button);
        });
    }
}
