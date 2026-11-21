using System.Net;
using System.Reflection;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Testing;
using SmartFarm.Web.Areas.Admin.Controllers;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Admin;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class SuperAdminPhase2Tests
{
    [Theory]
    [InlineData("/admin/users")]
    [InlineData("/admin/farms")]
    [InlineData("/admin/users/00000000-0000-0000-0000-000000000001")]
    [InlineData("/admin/farms/00000000-0000-0000-0000-000000000001")]
    public async Task DirectoriesAndDetailsRequireSignIn(string route)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync(route);
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Contains("/Account/Login", response.Headers.Location?.ToString(),
            StringComparison.OrdinalIgnoreCase);
    }

    [Theory]
    [InlineData(typeof(UsersController))]
    [InlineData(typeof(FarmsController))]
    [InlineData(typeof(DashboardController))]
    public void AdminControllersRequireTheMfaPolicy(Type controller)
    {
        var authorization = controller.GetCustomAttribute<AuthorizeAttribute>();
        Assert.NotNull(authorization);
        Assert.Equal(SuperAdminSecurity.Policy, authorization.Policy);
    }

    [Theory]
    [InlineData(typeof(UsersController))]
    [InlineData(typeof(FarmsController))]
    public void StatusChangesRequireAntiforgery(Type controller)
    {
        var action = controller.GetMethod("ChangeStatus");
        Assert.NotNull(action);
        Assert.NotNull(action.GetCustomAttribute<ValidateAntiForgeryTokenAttribute>());
        Assert.NotNull(action.GetCustomAttribute<HttpPostAttribute>());
    }

    [Fact]
    public void PaginationDoesNotOfferNonexistentPages()
    {
        var first = new AdminUserIndexPage("", "all", 1, 20, []);
        var second = new AdminFarmIndexPage("", "all", 2, 21, []);
        Assert.False(first.HasPrevious);
        Assert.False(first.HasNext);
        Assert.True(second.HasPrevious);
        Assert.False(second.HasNext);
    }
}
