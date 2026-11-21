using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;
namespace SmartFarm.Auth.Tests;
public class FrontendLocalizationTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly HttpClient client;
    public FrontendLocalizationTests(WebApplicationFactory<Program> app) => client = app.CreateClient(new() { AllowAutoRedirect = false });
    [Fact] public async Task FilipinoCookieChangesChromeAndLeavesRoutesStable()
    {
        client.DefaultRequestHeaders.Add("Cookie", ".AspNetCore.Culture=c%3Dfil%7Cuic%3Dfil");
        var html = await client.GetStringAsync("/Account/Login");
        Assert.Contains("lang=\"fil\"", html);
        Assert.Contains("Mag-log in", html);
        Assert.Contains("/Account/Register", html);
    }
    [Fact] public async Task UnsupportedLanguageFallsBackToEnglish()
    {
        client.DefaultRequestHeaders.Add("Cookie", ".AspNetCore.Culture=c%3Dunknown%7Cuic%3Dunknown");
        var html = await client.GetStringAsync("/Account/Login");
        Assert.Contains("lang=\"en\"", html);
        Assert.Contains("Log in", html);
    }
    [Fact] public async Task PreferenceCannotBeChangedWithoutAntiforgery()
    {
        var response = await client.PostAsync("/Language/Select",new FormUrlEncodedContent(new Dictionary<string,string>{{"language","fil"},{"returnUrl","https://example.org"}}));
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }
}
