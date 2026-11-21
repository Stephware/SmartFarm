using System.Net;
using System.Reflection;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Web.Areas.Admin.Controllers;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Admin;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class SuperAdminPhase5Tests
{
    [Theory]
    [InlineData("/admin/audit")]
    [InlineData("/admin/audit?action=user-disabled&targetType=user")]
    [InlineData("/admin/audit/00000000-0000-0000-0000-000000000001")]
    public async Task AuditHistoryRejectsAnonymousRequests(string path)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync(path);
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Contains("/Account/Login", response.Headers.Location?.ToString(),
            StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void AuditHistoryRequiresVerifiedMfaAndIsReadOnly()
    {
        var controller = typeof(AuditController);
        Assert.Equal("Admin", controller.GetCustomAttribute<AreaAttribute>()?.RouteValue);
        Assert.Equal("admin/audit", controller.GetCustomAttribute<RouteAttribute>()?.Template);
        Assert.Equal(SuperAdminSecurity.Policy,
            controller.GetCustomAttribute<AuthorizeAttribute>()?.Policy);
        Assert.True(controller.GetCustomAttribute<ResponseCacheAttribute>()?.NoStore);
        var actions = controller.GetMethods(BindingFlags.Public | BindingFlags.Instance |
                BindingFlags.DeclaredOnly)
            .Where(method => method.DeclaringType == controller)
            .ToArray();
        Assert.Equal(2, actions.Length);
        Assert.All(actions, action =>
        {
            Assert.NotNull(action.GetCustomAttribute<HttpGetAttribute>());
            Assert.Null(action.GetCustomAttribute<HttpPostAttribute>());
        });
    }

    [Theory]
    [InlineData(typeof(UsersController))]
    [InlineData(typeof(FarmsController))]
    public void SensitiveAdminStatusChangesRequirePasswordCsrfAndRateLimit(Type controller)
    {
        var action = controller.GetMethod("ChangeStatus");
        Assert.NotNull(action);
        Assert.NotNull(action.GetCustomAttribute<HttpPostAttribute>());
        Assert.NotNull(action.GetCustomAttribute<ValidateAntiForgeryTokenAttribute>());
        Assert.Equal(EnrollmentRateLimiting.AdminActions,
            action.GetCustomAttribute<EnableRateLimitingAttribute>()?.PolicyName);
        Assert.Contains(action.GetParameters(), parameter => parameter.Name == "currentPassword");
        Assert.Equal(SuperAdminSecurity.Policy,
            controller.GetCustomAttribute<AuthorizeAttribute>()?.Policy);
    }

    [Fact]
    public void PasswordStepUpRejectsEmptyAndOversizedInput()
    {
        Assert.False(AdminActionSecurity.ValidPasswordInput(null));
        Assert.False(AdminActionSecurity.ValidPasswordInput(""));
        Assert.True(AdminActionSecurity.ValidPasswordInput("test"));
        Assert.True(AdminActionSecurity.ValidPasswordInput(new string('a', 1024)));
        Assert.False(AdminActionSecurity.ValidPasswordInput(new string('a', 1025)));
    }

    [Fact]
    public void AuditFiltersAndLabelsRemainPredictable()
    {
        Assert.True(AdminAuditDisplay.IsKnownAction("user-disabled"));
        Assert.True(AdminAuditDisplay.IsKnownAction("support-internal-note"));
        Assert.False(AdminAuditDisplay.IsKnownAction("untrusted-filter"));
        Assert.Equal("Farm archived", AdminAuditDisplay.ActionLabel("farm-archived"));
        Assert.Equal("Support case", AdminAuditDisplay.TargetLabel("support-ticket"));
        var first = new AdminAuditListPage("", "all", "all", null,
            null, null, 1, 25, 0, []);
        Assert.False(first.HasPrevious);
        Assert.False(first.HasNext);
        var next = first with { Page = 2, Total = 27 };
        Assert.True(next.HasPrevious);
        Assert.False(next.HasNext);
    }
}
