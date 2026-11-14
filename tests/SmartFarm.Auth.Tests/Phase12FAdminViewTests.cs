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
            ("announcements", "Announcements", "Index",
                new AdminAnnouncementIndexPage("All", 1, 1, 1, 0,
                    [new AnnouncementRow(Guid.NewGuid(), "Scheduled maintenance",
                        "Test only", "Maintenance", "Draft", Snapshot, null, null, "AAAAAAAABAA=")])),
            ("support", "Support", "Index", new SmartFarm.Web.ViewModels.Support.AdminSupportListPage(
                "", "All", "All", 1, 1, 1, 0, 0,
                [new SmartFarm.Web.ViewModels.Support.SupportRow(ExampleFarm, ExampleUser, "Previous request",
                    "Account", "Open", "Normal", "Test Farmer", "farmer@example.test", "Green Farm", Snapshot, Snapshot, "AAAAAAAABAA=")])),
            ("support-detail", "Support", "Details", new SmartFarm.Web.ViewModels.Support.SupportDetailsPage(
                new SmartFarm.Web.ViewModels.Support.SupportRow(ExampleFarm, ExampleUser, "Previous request",
                    "Account", "Open", "Normal", "Test Farmer", "farmer@example.test", "Green Farm", Snapshot, Snapshot, "AAAAAAAABAA="),
                "Retained original description", [new SmartFarm.Web.ViewModels.Support.SupportMessageRow(
                    Guid.NewGuid(), "Test Admin", "Retained internal note", true, Snapshot)])),
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
        // Razor link tag helpers need an IRouter when called outside a real request.
        routes.Routers.Add(new FixtureRouter());
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

    // Link generation only; never matches HTTP requests or grants authentication.
    private sealed class FixtureRouter : IRouter
    {
        public Task RouteAsync(RouteContext context) => Task.CompletedTask;

        public VirtualPathData? GetVirtualPath(VirtualPathContext context)
        {
            var values = context.Values;
            var controller = values.TryGetValue("controller", out var c) ? c?.ToString() : "Dashboard";
            var action = values.TryGetValue("action", out var a) ? a?.ToString() : "Index";
            var area = values.TryGetValue("area", out var r) ? r?.ToString() : "Admin";
            var prefix = string.Equals(area, "Admin", StringComparison.OrdinalIgnoreCase)
                ? "/admin" : "";
            var path = prefix + (controller == "Dashboard" && action == "Index"
                ? "" : "/" + controller?.ToLowerInvariant());
            if (action is not ("Index" or null)) path += "/" + action?.ToLowerInvariant();
            var extras = values
                .Where(kv => kv.Key is not ("area" or "controller" or "action") && kv.Value is not null)
                .Select(kv => Uri.EscapeDataString(kv.Key) + "=" +
                    Uri.EscapeDataString(kv.Value!.ToString() ?? ""));
            var query = string.Join("&", extras);
            return new VirtualPathData(this, path + (query.Length > 0 ? "?" + query : ""));
        }
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
            Assert.DoesNotContain("/admin/reports", html);
            if (page.Name == "support-detail")
            {
                Assert.Contains("Retained internal note", html);
                Assert.Contains("Internal only", html);
                Assert.Contains("Read-only history", html);
                Assert.DoesNotContain("asp-action=", html);
                Assert.DoesNotContain("/reply", html);
                Assert.DoesNotContain("/status", html);
            }
            if (page.Name == "announcements")
            {
                Assert.Contains("Announcement history", html);
                Assert.DoesNotContain("New announcement", html);
                Assert.DoesNotContain("View public", html);
            }
        }
    }

    [Fact]
    public async Task EmptyAnnouncementHistoryDoesNotOfferRetiredPublishing()
    {
        using var app = new WebApplicationFactory<Program>();
        var html = await RenderAsync(app.Services, "Announcements", "Index",
            new AdminAnnouncementIndexPage("All", 1, 0, 0, 0, []));
        Assert.Contains("No retained announcements match these filters.", html);
        Assert.DoesNotContain("Create a draft", html);
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
