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

    [Fact]
    public void AdminPublishingAndReportsRequireVerifiedMfa()
    {
        var announcements = typeof(SmartFarm.Web.Areas.Admin.Controllers.AnnouncementsController);
        var reports = typeof(SmartFarm.Web.Areas.Admin.Controllers.ReportsController);
        Assert.Equal(SuperAdminSecurity.Policy,
            announcements.GetCustomAttribute<AuthorizeAttribute>()?.Policy);
        Assert.Equal(SuperAdminSecurity.Policy,
            reports.GetCustomAttribute<AuthorizeAttribute>()?.Policy);
        Assert.Equal("Admin", announcements.GetCustomAttribute<AreaAttribute>()?.RouteValue);
        Assert.Equal("Admin", reports.GetCustomAttribute<AreaAttribute>()?.RouteValue);
        Assert.True(reports.GetCustomAttribute<ResponseCacheAttribute>()?.NoStore);
        Assert.NotNull(reports.GetMethod("Csv")?.GetCustomAttribute<HttpGetAttribute>());

        foreach (var name in new[] { "Create", "Edit", "Publish", "Archive" })
        {
            var action = announcements.GetMethods()
                .Single(m => m.Name == name && m.GetCustomAttribute<HttpPostAttribute>() is not null);
            Assert.NotNull(action.GetCustomAttribute<ValidateAntiForgeryTokenAttribute>());
            Assert.Equal(EnrollmentRateLimiting.AdminActions,
                action.GetCustomAttribute<EnableRateLimitingAttribute>()?.PolicyName);
        }
        foreach (var name in new[] { "Publish", "Archive" })
        {
            var action = announcements.GetMethod(name)!;
            Assert.Contains(action.GetParameters(), p => p.Name == "currentPassword");
            Assert.Contains(action.GetParameters(), p => p.Name == "rowVersion");
            Assert.Contains(action.GetParameters(), p => p.Name == "reason");
        }
    }

    [Fact]
    public void PublicBulletinUsesCookieAuthAndNeverTreatsDraftsAsVisible()
    {
        var controller = typeof(SmartFarm.Web.Controllers.AnnouncementsController);
        Assert.Equal(SmartFarmAuthSchemes.Application,
            controller.GetCustomAttribute<AuthorizeAttribute>()?.AuthenticationSchemes);
        var now = new DateTime(2026, 11, 12, 9, 0, 0, DateTimeKind.Utc);
        var announcement = new PlatformAnnouncement { State = "Draft", PublishedAtUtc = null };
        Assert.False(AnnouncementRules.IsVisible(announcement, now));
        announcement.State = "Published";
        announcement.PublishedAtUtc = now;
        Assert.True(AnnouncementRules.IsVisible(announcement, now));
        announcement.ExpiresAtUtc = now;
        Assert.False(AnnouncementRules.IsVisible(announcement, now));
        announcement.ExpiresAtUtc = now.AddMinutes(1);
        Assert.True(AnnouncementRules.IsVisible(announcement, now));
        announcement.State = "Archived";
        Assert.False(AnnouncementRules.IsVisible(announcement, now));
    }

    [Fact]
    public void ReportWindowsAndConcurrencyTokensAreBounded()
    {
        Assert.Equal(30, AdminReportRules.NormalizeDays(null));
        Assert.Equal(30, AdminReportRules.NormalizeDays(3000));
        Assert.Equal(7, AdminReportRules.NormalizeDays(7));
        Assert.Equal(90, AdminReportRules.NormalizeDays(90));
        Assert.False(AnnouncementRules.ValidRowVersion("garbage", out _));
        var version = Convert.ToBase64String(new byte[8]);
        Assert.True(AnnouncementRules.ValidRowVersion(version, out var decoded));
        Assert.Equal(8, decoded.Length);
    }
}
