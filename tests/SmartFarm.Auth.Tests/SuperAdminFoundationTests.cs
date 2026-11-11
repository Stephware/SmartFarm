using System.Security.Claims;
using Microsoft.AspNetCore.Mvc.Testing;
using SmartFarm.Web.Authentication;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class SuperAdminFoundationTests
{
    private static ClaimsPrincipal Principal(bool admin, bool mfa, bool authenticated = true)
    {
        var claims = new List<Claim> { new(ClaimTypes.NameIdentifier, Guid.NewGuid().ToString()) };
        if (admin) claims.Add(new Claim(ClaimTypes.Role, SuperAdminSecurity.Role));
        if (mfa) claims.Add(new Claim("amr", "mfa"));
        return new ClaimsPrincipal(new ClaimsIdentity(claims,
            authenticated ? "Application" : null));
    }

    [Theory]
    [InlineData(false, false, false)]
    [InlineData(true, false, false)]
    [InlineData(false, true, false)]
    [InlineData(true, true, true)]
    public void AdminDashboardNeedsBothRoleAndVerifiedAuthenticator(
        bool role, bool mfa, bool allowed) =>
        Assert.Equal(allowed, SuperAdminSecurity.HasVerifiedMfa(Principal(role, mfa)));

    [Fact]
    public void UnauthenticatedClaimsCannotBypassAdminPolicy() =>
        Assert.False(SuperAdminSecurity.HasVerifiedMfa(Principal(true, true, false)));

    [Fact]
    public void BootstrapRequiresExplicitFlagExactCommandAndEmail()
    {
        Assert.False(SuperAdminProvisioner.IsExplicitBootstrap(null,
            ["--provision-super-admin", "example@example.test"]));
        Assert.False(SuperAdminProvisioner.IsExplicitBootstrap("I_UNDERSTAND_ADMIN_ACCESS",
            ["--provision-super-admin"]));
        Assert.False(SuperAdminProvisioner.IsExplicitBootstrap("I_UNDERSTAND_ADMIN_ACCESS",
            ["--other", "example@example.test"]));
        Assert.True(SuperAdminProvisioner.IsExplicitBootstrap("I_UNDERSTAND_ADMIN_ACCESS",
            ["--provision-super-admin", "example@example.test"]));
    }

    [Fact]
    public async Task AdminRoutesRequireAuthenticationAndDoNotExposeOnboardingToGuests()
    {
        await using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var dashboard = await client.GetAsync("/admin");
        using var setup = await client.GetAsync("/admin/security/setup");
        Assert.Equal(System.Net.HttpStatusCode.Redirect, dashboard.StatusCode);
        Assert.Equal(System.Net.HttpStatusCode.Redirect, setup.StatusCode);
        Assert.Contains("/Account/Login", dashboard.Headers.Location?.ToString(),
            StringComparison.OrdinalIgnoreCase);
    }
}
