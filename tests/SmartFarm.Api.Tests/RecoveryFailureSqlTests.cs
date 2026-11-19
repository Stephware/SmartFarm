using System.Net;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Email;
using SmartFarm.Core.Contracts.Accounts;
using Xunit;
namespace SmartFarm.Api.Tests;
public sealed class RecoveryFailureSqlTests(BackendSqlFixture fixture) : IClassFixture<BackendSqlFixture>
{
    [Fact]
    public async Task ExpiredTokensFailAndDeliveryFailureRemainsNeutral()
    {
        if (fixture.Factory is null) return;
        using var expired = fixture.Factory.WithWebHostBuilder(builder => builder.ConfigureServices(services =>
            services.Configure<PasswordResetTokenOptions>(o => o.TokenLifespan = TimeSpan.FromSeconds(-1))));
        using var scope = expired.Services.CreateScope();
        var users = scope.ServiceProvider.GetRequiredService<UserManager<SmartFarmUser>>();
        var email = $"expired-{Guid.NewGuid():N}@example.test";
        Assert.True((await users.CreateAsync(new SmartFarmUser { UserName = email, Email = email, FullName = "Expiry Test" }, "ExpiryTest2026!")).Succeeded);
        var recovery = scope.ServiceProvider.GetRequiredService<AccountRecoveryService>();
        await recovery.RequestAsync(email);
        var mail = Assert.Single(expired.Services.GetRequiredService<CaptureEmailSender>().Messages, m => m.To == email);
        var uri = new Uri(mail.Body.Split('\n').Single(s => s.StartsWith("https://")));
        var token = Microsoft.AspNetCore.WebUtilities.QueryHelpers.ParseQuery(uri.Query)["token"].ToString();
        Assert.False(await recovery.ResetAsync(new ResetPasswordInput { Email = email, Token = token, Password = "ChangedTest2026!" }));
        using var failing = fixture.Factory.WithWebHostBuilder(builder => builder.ConfigureServices(services =>
        {
            services.RemoveAll<ISecurityEmailSender>(); services.AddSingleton<ISecurityEmailSender, FailingSender>();
        }));
        using var client = failing.CreateClient();
        Assert.Equal(HttpStatusCode.OK, (await client.PostAsJsonAsync("/api/auth/forgot-password", new { email })).StatusCode);
    }
    private sealed class FailingSender : ISecurityEmailSender
    {
        public Task<bool> SendAsync(SecurityEmail message, CancellationToken ct = default) => throw new System.Net.Mail.SmtpException("provider unavailable");
    }
}
