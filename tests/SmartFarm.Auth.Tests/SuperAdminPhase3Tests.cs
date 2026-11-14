using System.Net;
using System.Reflection;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Testing;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Support;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class SuperAdminPhase3Tests
{
    [Theory]
    [InlineData("/admin/support")]
    [InlineData("/admin/support/00000000-0000-0000-0000-000000000001")]
    [InlineData("/support")]
    [InlineData("/support/new")]
    [InlineData("/support/00000000-0000-0000-0000-000000000001")]
    public async Task SupportPagesRedirectGuestsToLogin(string path)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync(path);
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Contains("/Account/Login", response.Headers.Location?.ToString(),
            StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void AdminSupportWritesAreRetiredWithoutReadingOrChangingRecords()
    {
        var controller = new SmartFarm.Web.Areas.Admin.Controllers.SupportController(null!, null!);
        Assert.Equal(410, Assert.IsType<ObjectResult>(controller.ChangeStatus(Guid.NewGuid())).StatusCode);
        Assert.Equal(410, Assert.IsType<ObjectResult>(controller.Reply(Guid.NewGuid())).StatusCode);
    }

    [Fact]
    public void AdminSupportRequiresVerifiedMfa()
    {
        var controller = typeof(SmartFarm.Web.Areas.Admin.Controllers.SupportController);
        Assert.Equal(SuperAdminSecurity.Policy,
            controller.GetCustomAttribute<AuthorizeAttribute>()?.Policy);
        Assert.Equal("Admin", controller.GetCustomAttribute<AreaAttribute>()?.RouteValue);
        Assert.Equal("admin/support", controller.GetCustomAttribute<RouteAttribute>()?.Template);
        AssertAntiforgery(controller, "Reply");
        AssertAntiforgery(controller, "ChangeStatus");
    }

    [Fact]
    public void UserSupportUsesCookiesAndProtectedWrites()
    {
        var controller = typeof(SmartFarm.Web.Controllers.SupportController);
        Assert.Equal(SmartFarmAuthSchemes.Application,
            controller.GetCustomAttribute<AuthorizeAttribute>()?.AuthenticationSchemes);
        AssertAntiforgery(controller, "Reply");
        AssertAntiforgery(controller, "Create", 2);
    }

    [Fact]
    public void RetainedSupportStatusesAndPrioritiesStayFilterable()
    {
        Assert.Equal(["Open", "InProgress", "Resolved", "Closed"], SupportRules.Statuses);
        Assert.Equal(["Normal", "High"], SupportRules.Priorities);
    }

    private static void AssertAntiforgery(Type controller, string method, int? parameterCount = null)
    {
        var methods = controller.GetMethods().Where(x => x.Name == method).ToList();
        var action = parameterCount.HasValue
            ? methods.Single(m => m.GetParameters().Length == parameterCount)
            : methods.Single();
        Assert.NotNull(action.GetCustomAttribute<HttpPostAttribute>());
        Assert.NotNull(action.GetCustomAttribute<ValidateAntiForgeryTokenAttribute>());
    }
}
