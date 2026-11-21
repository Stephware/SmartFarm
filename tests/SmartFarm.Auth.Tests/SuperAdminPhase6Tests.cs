using System.Net;
using System.Reflection;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Admin;
using SmartFarm.Web.ViewModels.Announcements;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class SuperAdminPhase6Tests
{
    [Theory]
    [InlineData("/admin/announcements")]
    [InlineData("/admin/announcements/new")]
    [InlineData("/admin/announcements/00000000-0000-0000-0000-000000000001")]
    [InlineData("/admin/reports")]
    [InlineData("/admin/reports/csv?days=90")]
    [InlineData("/announcements")]
    [InlineData("/announcements/00000000-0000-0000-0000-000000000001")]
    public async Task AnnouncementsAndReportsRequireSignIn(string url)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync(url);
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Contains("/Account/Login", response.Headers.Location?.ToString(),
            StringComparison.OrdinalIgnoreCase);
    }

    [Theory]
    [InlineData(typeof(SmartFarm.Web.Areas.Admin.Controllers.AnnouncementsController))]
    [InlineData(typeof(SmartFarm.Web.Areas.Admin.Controllers.ReportsController))]
    public void RetainedAdminHistoryAndRetiredAnalyticsStillRequireVerifiedMfa(Type controller)
    {
        Assert.Equal(SuperAdminSecurity.Policy, controller.GetCustomAttribute<AuthorizeAttribute>()?.Policy);
        Assert.Equal("Admin", controller.GetCustomAttribute<AreaAttribute>()?.RouteValue);
        Assert.True(controller.GetCustomAttribute<ResponseCacheAttribute>()?.NoStore);
    }

    [Fact]
    public void AnnouncementMutationsAreGoneAndStillRequireCsrf()
    {
        var controller = new SmartFarm.Web.Areas.Admin.Controllers.AnnouncementsController(null!, null!);
        var results = new IActionResult[] { controller.Create(), controller.Create(null),
            controller.Edit(Guid.NewGuid()), controller.Edit(Guid.NewGuid(), null),
            controller.Publish(Guid.NewGuid()), controller.Archive(Guid.NewGuid()) };
        Assert.All(results, result => Assert.Equal(410, Assert.IsType<ObjectResult>(result).StatusCode));
        foreach (var action in controller.GetType().GetMethods().Where(m => m.GetCustomAttribute<HttpPostAttribute>() is not null))
            Assert.NotNull(action.GetCustomAttribute<ValidateAntiForgeryTokenAttribute>());
    }

    [Fact]
    public void RetiredPlatformAnalyticsNeverQueriesTheDatabase()
    {
        var controller = new SmartFarm.Web.Areas.Admin.Controllers.ReportsController();
        Assert.Equal(410, Assert.IsType<ObjectResult>(controller.Index()).StatusCode);
        Assert.Equal(410, Assert.IsType<ObjectResult>(controller.Csv()).StatusCode);
    }
}
