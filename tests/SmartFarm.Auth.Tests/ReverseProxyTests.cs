using System.Net;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class ReverseProxyTests
{
    [Theory]
    [InlineData("127.0.0.1", "https", HttpStatusCode.OK)]
    [InlineData("::1", "https", HttpStatusCode.OK)]
    [InlineData("203.0.113.10", "https", HttpStatusCode.TemporaryRedirect)]
    [InlineData("127.0.0.1", "http", HttpStatusCode.TemporaryRedirect)]
    public async Task OnlyLocalProxyCanSetHttps(string address, string scheme, HttpStatusCode expected)
    {
        using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.UseEnvironment("Production");
            builder.UseSetting("ObservationPhotos:StoragePath", Path.GetTempPath());
            builder.UseSetting("https_port", "443");
            builder.ConfigureServices(services => services.AddSingleton<IStartupFilter>(new RemoteAddress(address)));
        });
        using var client = factory.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
        using var request = new HttpRequestMessage(HttpMethod.Get, "/Account/Login");
        request.Headers.Add("X-Forwarded-Proto", scheme);
        using var response = await client.SendAsync(request);
        Assert.Equal(expected, response.StatusCode);
    }

    private sealed class RemoteAddress(string address) : IStartupFilter
    {
        public Action<IApplicationBuilder> Configure(Action<IApplicationBuilder> next) => app =>
        {
            app.Use((context, proceed) =>
            {
                context.Connection.RemoteIpAddress = IPAddress.Parse(address);
                return proceed(context);
            });
            next(app);
        };
    }
}
