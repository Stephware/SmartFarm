using System.Net;
using System.Reflection;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Testing;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.Areas.Admin.Controllers;
using SmartFarm.Web.ViewModels.Admin;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class SuperAdminPhase4Tests
{
    [Theory]
    [InlineData("/admin/monitoring")]
    [InlineData("/admin/monitoring/sync")]
    [InlineData("/admin/monitoring/sync?status=failed&type=inventory")]
    public async Task MonitoringRoutesRejectAnonymousUsers(string path)
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync(path);
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Contains("/Account/Login", response.Headers.Location?.ToString(),
            StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void MonitoringControllerRequiresMfaAndOnlyExposesReadOnlyGetRoutes()
    {
        var controller = typeof(MonitoringController);
        Assert.Equal("Admin", controller.GetCustomAttribute<AreaAttribute>()?.RouteValue);
        Assert.Equal(SuperAdminSecurity.Policy,
            controller.GetCustomAttribute<AuthorizeAttribute>()?.Policy);
        Assert.Equal("admin/monitoring",
            controller.GetCustomAttribute<RouteAttribute>()?.Template);

        var actions = controller.GetMethods(BindingFlags.Public | BindingFlags.Instance |
            BindingFlags.DeclaredOnly).Where(m => m.ReturnType.Name.StartsWith("Task")).ToArray();
        Assert.Equal(2, actions.Length);
        Assert.All(actions, a => Assert.NotNull(a.GetCustomAttribute<HttpGetAttribute>()));
    }

    [Theory]
    [InlineData(null, "No workers")]
    [InlineData(-2, "Active")]
    [InlineData(-6, "Stale")]
    [InlineData(2, "Stale")]
    public void WorkerHeartbeatHasExplicitFreshnessRules(int? minutes, string expected)
    {
        var now = new DateTime(2026, 11, 12, 10, 0, 0, DateTimeKind.Utc);
        var beat = minutes is null ? (DateTime?)null : now.AddMinutes(minutes.Value);
        Assert.Equal(expected, AdminMonitoringRules.WorkerState(beat, now));
    }

    [Fact]
    public void SyncHistoryHasBoundedPagingAndSafeFilters()
    {
        var current = new AdminSyncPage("", "all", "all", 1, 25, true, []);
        Assert.False(current.HasPrevious);
        Assert.False(current.HasNext);
        Assert.Equal("failed", AdminMonitoringRules.FilterStatus("failed"));
        Assert.Equal("all", AdminMonitoringRules.FilterStatus("bogus"));
        Assert.Equal("Succeeded", AdminMonitoringRules.SyncOutcome(1));
        Assert.Equal("Recorded failure", AdminMonitoringRules.SyncOutcome(2));
        Assert.Equal("Unknown", AdminMonitoringRules.SyncOutcome(3));
    }
}
