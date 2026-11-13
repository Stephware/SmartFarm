using System.Net;
using System.Security.Claims;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Abstractions;
using Microsoft.AspNetCore.Mvc.ModelBinding;
using Microsoft.AspNetCore.Mvc.Razor;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.AspNetCore.Mvc.Routing;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.Mvc.ViewFeatures;
using Microsoft.AspNetCore.Routing;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Web.ViewModels.Admin;
using SmartFarm.Web.ViewModels.Announcements;
using SmartFarm.Web.ViewModels.Auth;
using Xunit;

namespace SmartFarm.Auth.Tests;

// Test-only Razor rendering. No HTTP route, privileged cookie, production credentials,
// or database connection is exposed. The model contains synthetic platform records.
public sealed class Phase12FAdminViewTests
{
    private static readonly DateTime Snapshot = new(2026, 11, 13, 8, 0, 0, DateTimeKind.Utc);
    private static readonly Guid ExampleFarm = Guid.Parse("00000000-0000-0000-0000-00000000f001");
    private static readonly Guid ExampleUser = Guid.Parse("00000000-0000-0000-0000-00000000a001");

    private static IReadOnlyList<(string Name, string Controller, string Action, object Model)> Pages()
    {
        var farm = new AdminFarmRow(ExampleFarm, "Green Farm", "Pampanga, Philippines",
            "Test Owner", "owner@example.test", false, 3, 4, Snapshot, "AAAAAAAABAA=");
        var user = new AdminUserRow(ExampleUser, "Test Operator", "operator@example.test",
            false, false, 1, 2);
        var worker = new AdminWorkerStatus("Active", "Synthetic monitoring status", Snapshot,
            0, 1, []);
        return
        [
            ("overview", "Dashboard", "Index", new AdminOverviewPage(12, 11, 1,
                5, 4, 1, 8, 2, [farm]) { Worker = worker }),
            ("users", "Users", "Index", new AdminUserIndexPage("", "all", 1, 1, [user])),
            ("farms", "Farms", "Index", new AdminFarmIndexPage("", "all", 1, 1, [farm])),
            ("user-detail", "Users", "Details", new AdminUserDetailPage(user, [farm])),
            ("farm-detail", "Farms", "Details", new AdminFarmDetailPage(farm, 0, 0)),
            ("monitoring", "Monitoring", "Index", new AdminMonitoringPage(Snapshot,
                new AdminServiceStatus("Available", "Synthetic test database status"),
                new AdminServiceStatus("Available", "Synthetic test API status"),
                worker, new AdminSyncSummary(true, 3, 0, Snapshot, "Synthetic receipt counts"))),
            ("sync", "Monitoring", "Sync", new AdminSyncPage("", "all", "all", 1, 1,
                true, [new AdminSyncRow(Guid.NewGuid(), "Green Farm", "inventory",
                    "Succeeded", null, Snapshot)])),
            ("audit", "Audit", "Index", new AdminAuditListPage("", "all", "all",
                null, null, null, 1, 1, 1,
                [new AdminAuditRow(Guid.NewGuid(), Snapshot, "Test Admin",
                    "admin@example.test", "Web", "user-disabled", "user", ExampleUser)])),
            ("reports", "Reports", "Index", new AdminPlatformReportPage(30,
                new DateOnly(2026, 10, 15), new DateOnly(2026, 11, 13), Snapshot,
                12, 1, 5, 4, 8, 3, 2, 1, 2, 4, 1, 3,
                [new AdminReportDay(new DateOnly(2026, 11, 13), 1, 2, 1, 3)])),
            ("announcements", "Announcements", "Index",
                new AdminAnnouncementIndexPage("All", 1, 1, 1, 0,
                    [new AnnouncementRow(Guid.NewGuid(), "Scheduled maintenance",
                        "Test only", "Maintenance", "Draft", Snapshot, null, null, "AAAAAAAABAA=")])),
            ("announcement-new", "Announcements", "Editor",
                new AnnouncementEditorPage(null, new AnnouncementInput())),
            ("security", "Security", "Setup",
                new AdminMfaSetupPage("TEST-ONLY-KEY-DO-NOT-USE", "test-admin@example.test"))
        ];
    }

    private static async Task<string> RenderAsync(IServiceProvider services, string controller,
        string action, object model)
    {
        await using var scope = services.CreateAsyncScope();
        var http = new DefaultHttpContext
        {
            RequestServices = scope.ServiceProvider,
            User = new ClaimsPrincipal(new ClaimsIdentity(
                [new Claim(ClaimTypes.Name, "Test Super Admin"), new Claim(ClaimTypes.Role, "SuperAdmin")],
                "Test"))
        };
        http.Request.Scheme = "http";
        http.Request.Host = new HostString("127.0.0.1:5082");
        http.Request.Path = "/admin/" + controller.ToLowerInvariant();
        http.Response.Body = new MemoryStream();
        var routes = new RouteData();
        routes.Values["area"] = "Admin";
        routes.Values["controller"] = controller;
        routes.Values["action"] = action;
        var context = new ActionContext(http, routes, new ActionDescriptor());
        var engine = scope.ServiceProvider.GetRequiredService<IRazorViewEngine>();
        var viewPath = $"/Areas/Admin/Views/{controller}/{(action == "Editor" ? "Editor" : action)}.cshtml";
        var lookup = engine.GetView(null, viewPath, true);
        Assert.True(lookup.Success, "Missing admin view: " + viewPath);
        var metadata = scope.ServiceProvider.GetRequiredService<IModelMetadataProvider>();
        var viewData = new ViewDataDictionary(metadata, new ModelStateDictionary()) { Model = model };
        var tempProvider = scope.ServiceProvider.GetRequiredService<ITempDataProvider>();
        var tempData = new TempDataDictionary(http, tempProvider);
        using var writer = new StringWriter();
        var viewContext = new ViewContext(context, lookup.View, viewData, tempData, writer,
            new HtmlHelperOptions());
        await lookup.View.RenderAsync(viewContext);
        return writer.ToString();
    }

    [Fact]
    public async Task AdminPagesRenderSharedNavigationWithoutExposingFarmRecords()
    {
        using var app = new WebApplicationFactory<Program>();
        foreach (var page in Pages())
        {
            var html = await RenderAsync(app.Services, page.Controller, page.Action, page.Model);
            Assert.Contains("ani12f-admin-shell", html);
            Assert.Contains("id=\"main-content\"", html);
            Assert.Contains("Skip to content", html);
            Assert.Contains("Mobile platform administration", html);
            Assert.Contains("aria-current=\"page\"", html);
            Assert.DoesNotContain("Farm joining code", html, StringComparison.OrdinalIgnoreCase);
            if (page.Name == "security")
                Assert.Contains("TEST-ONLY-KEY-DO-NOT-USE", html);
            if (page.Name == "reports")
            {
                Assert.Contains("Daily platform report data", html);
                Assert.Contains("Aggregate activity trends only", html);
            }
        }
    }

    [Fact]
    public async Task Phase12FAdminBrowserFixtures()
    {
        if (Environment.GetEnvironmentVariable("ANIGO_CAPTURE_ADMIN_FIXTURES") != "1") return;
        var root = new DirectoryInfo(AppContext.BaseDirectory);
        while (root is not null && !File.Exists(Path.Combine(root.FullName, "SmartFarm.slnx")))
            root = root.Parent;
        Assert.NotNull(root);
        var folder = Path.Combine(root.FullName, "tests", "browser", "admin-fixtures");
        Directory.CreateDirectory(folder);
        using var app = new WebApplicationFactory<Program>();
        foreach (var page in Pages())
        {
            var html = await RenderAsync(app.Services, page.Controller, page.Action, page.Model);
            Assert.Contains("ani12f-admin-shell", html);
            await File.WriteAllTextAsync(Path.Combine(folder, page.Name + ".html"), html);
        }
    }
}
