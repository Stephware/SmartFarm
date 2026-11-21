using System.Net;
using System.Text.RegularExpressions;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Configuration.Json;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed partial class FarmEnrollmentWebTests
{
    [Fact]
    public void ImmutableMvcTestConfigurationDoesNotAllocateReloadWatchers()
    {
        using var app = App();
        var configuration = (IConfigurationRoot)app.Services.GetRequiredService<IConfiguration>();
        var files = configuration.Providers.OfType<JsonConfigurationProvider>().ToList();
        Assert.NotEmpty(files);
        Assert.All(files, file => Assert.False(file.Source.ReloadOnChange));
    }

    private static InventoryWorkspace Phase4Supplies(int count) => new(
        Enumerable.Range(1, count).Select(i => new InventoryItemDetails(
            new Guid(i, 0, 0, new byte[8]), FarmId, $"Supply {i:000}",
            InventoryCategory.Seeds, "kg", null, i, count > 25 && i <= 60 ? 100m : null,
            count > 25 && i <= 60, null, "v")).ToList(), count > 25 ? Math.Min(count, 60) : 0);

    private static List<NotificationDetails> Phase4Notices(int count) => Enumerable.Range(1, count)
        .Select(i => new NotificationDetails(new Guid(i, 0, 0, new byte[8]), 1,
            $"Notice {i:000}", "Check the farm's saved fieldwork records.", null, null,
            new DateTime(2026, 11, 15, 8, 0, 0, DateTimeKind.Utc), null)).ToList();

    [Theory]
    [InlineData("Owner")]
    [InlineData("Manager")]
    [InlineData("FieldWorker")]
    public async Task Phase4SupplyPagesKeepWholeScopeAndRoleGates(string role)
    {
        using var app = App(role, inventory: Phase4Supplies(76));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Inventory?farmId={FarmId}&page=2&filter=seeds&search=Supply");
        Assert.Equal(25, Regex.Matches(html, "<tr class=\"sf-inventory-row").Count);
        Assert.Contains("26–50 of 76 matching", WebUtility.HtmlDecode(html));
        Assert.Contains("Supply 026", html);
        Assert.DoesNotContain("Supply 001</a>", html);
        Assert.Contains("filter=seeds", html);
        Assert.Contains("search=Supply", html);
        Assert.Equal(role != "FieldWorker", html.Contains("id=\"add-item\""));
    }

    [Theory]
    [InlineData("Owner")]
    [InlineData("Manager")]
    [InlineData("FieldWorker")]
    public async Task Phase4FirstSupplyGuidanceOmitsUselessEmptyFilters(string role)
    {
        using var app = App(role, inventory: Phase4Supplies(0));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Inventory?farmId={FarmId}");
        Assert.Contains("No supplies recorded yet.", html);
        Assert.DoesNotContain("id=\"inventory-search\"", html);
        Assert.Equal(role != "FieldWorker", html.Contains("Add your first supply"));
    }

    [Fact]
    public async Task Phase4LowStockTotalsStayFarmWideAcrossFilteredPages()
    {
        using var app = App(inventory: Phase4Supplies(76));
        using var client = app.CreateClient();
        var html = WebUtility.HtmlDecode(await client.GetStringAsync($"/Inventory?farmId={FarmId}&filter=low&page=2"));
        Assert.Equal(25, Regex.Matches(html, "<tr class=\"sf-inventory-row").Count);
        Assert.Contains("26–50 of 60 matching", html);
        Assert.Contains("60 low-stock supply items", html);
        Assert.Contains("76 total", html);
    }

    [Theory]
    [InlineData("-1", "Supply 001", "1–25 of 26")]
    [InlineData("invalid", "Supply 001", "1–25 of 26")]
    [InlineData("2147483647", "Supply 026", "26–26 of 26")]
    public async Task Phase4SupplyPageBoundsNeverHideExistingRecords(string page, string name, string range)
    {
        using var app = App(inventory: Phase4Supplies(26));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Inventory?farmId={FarmId}&page={page}");
        Assert.Contains(name, html);
        Assert.Contains(range, WebUtility.HtmlDecode(html));
        Assert.DoesNotContain("No supplies recorded", html);
    }

    [Theory]
    [InlineData("all")]
    [InlineData("unread")]
    public async Task Phase4ReadingNotificationPreservesPageAndFilter(string view)
    {
        var notices = Phase4Notices(41);
        using var app = App(notifications: notices);
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Notifications?view={view}&page=2");
        Assert.Contains("name=\"returnPage\" value=\"2\"", html);
        using var response = await client.PostAsync($"/Notifications/Read?id={notices[20].Id}",
            Form(html, ("returnView", view), ("returnPage", "2")));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        var location = response.Headers.Location!.ToString();
        Assert.Contains("page=2", location);
        Assert.Equal(view == "unread", location.Contains("view=unread"));
        notices.RemoveRange(20, 21);
        var reduced = await client.GetStringAsync(location);
        Assert.Contains("Notice 001", reduced);
        Assert.DoesNotContain("You're caught up", reduced);
    }

    [Theory]
    [InlineData(0, false)]
    [InlineData(100, true)]
    public async Task Phase4NotificationEmptyStateDoesNotClaimAllHistoryIsRead(int count, bool atLimit)
    {
        var notices = Phase4Notices(count).Select(n => n with { ReadAtUtc = n.CreatedAtUtc }).ToList();
        using var app = App(notifications: notices);
        using var client = app.CreateClient();
        var html = WebUtility.HtmlDecode(await client.GetStringAsync("/Notifications?view=unread"));
        Assert.Contains("No unread recent notices.", html);
        Assert.DoesNotContain("You're caught up.", html);
        Assert.Equal(atLimit, html.Contains("100 most recent notices"));
    }

    [Fact]
    public async Task Phase4NotificationTiesHaveStablePages()
    {
        var notices = Phase4Notices(41);
        using var app = App(notifications: notices);
        using var client = app.CreateClient();
        var first = await client.GetStringAsync("/Notifications?page=2");
        notices.Reverse();
        var second = await client.GetStringAsync("/Notifications?page=2");
        string[] Titles(string html) => Regex.Matches(html, "<strong>(Notice [0-9]+)</strong>")
            .Select(m => m.Groups[1].Value).ToArray();
        Assert.Equal(20, Titles(first).Length);
        Assert.Equal(Titles(first), Titles(second));
    }

    [Fact]
    public async Task Phase4AuthenticatedBrowserFixtures()
    {
        if (Environment.GetEnvironmentVariable("ANIGO_CAPTURE_AUTH_FIXTURES") != "1") return;
        var root = new DirectoryInfo(AppContext.BaseDirectory);
        while (root is not null && !File.Exists(Path.Combine(root.FullName, "SmartFarm.slnx"))) root = root.Parent;
        Assert.NotNull(root);
        var directory = Path.Combine(root.FullName, "tests", "browser", "authenticated-fixtures");
        Directory.CreateDirectory(directory);
        foreach (var role in new[] { "Owner", "Manager", "FieldWorker" })
        foreach (var state in new[] { "empty", "populated", "large", "filtered" })
        {
            var count = state == "empty" ? 0 : state == "populated" ? 1 : 76;
            var notices = Phase4Notices(state == "large" ? 100 : count);
            if (state == "filtered") notices = notices.Select(n => n with { ReadAtUtc = n.CreatedAtUtc }).ToList();
            using var app = App(role, inventory: Phase4Supplies(count), notifications: notices);
            using var client = app.CreateClient();
            foreach (var area in new[] { "inventory", "notifications" })
            {
                var route = area == "inventory" ? $"/Inventory?farmId={FarmId}" : "/Notifications?view=unread";
                route += state == "large" ? "&page=2" : state == "filtered" ? "&search=Missing&filter=low&page=999" : "";
                var html = await client.GetStringAsync(route);
                await File.WriteAllTextAsync(Path.Combine(directory, $"phase4-{area}-{role}-{state}.html"), html);
            }
        }
    }
}
