using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;
namespace SmartFarm.Auth.Tests;
public sealed class RecoveryWebTests
{
    [Theory]
    [InlineData("/Account/ForgotPassword")]
    [InlineData("/Account/ResetPassword")]
    public async Task RecoveryPostsRequireAntiforgeryAndFormContractsAreNeverCached(string path)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
        using var post = await client.PostAsync(path, new FormUrlEncodedContent(new Dictionary<string,string> { ["Email"]="test@example.test" }));
        Assert.Equal(HttpStatusCode.BadRequest, post.StatusCode);
        using var get = await client.GetAsync(path);
        Assert.Equal(HttpStatusCode.OK, get.StatusCode);
        Assert.True(get.Headers.CacheControl?.NoStore);
        Assert.Equal("no-referrer", Assert.Single(get.Headers.GetValues("Referrer-Policy")));
        var data = await get.Content.ReadFromJsonAsync<JsonElement>();
        Assert.False(string.IsNullOrWhiteSpace(data.GetProperty("antiforgeryToken").GetString()));
    }
}
