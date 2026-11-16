using System.Net;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed partial class FarmEnrollmentWebTests
{
    private static readonly InventoryItemDetails Phase5Supply = new(
        Guid.Parse("6bdc2103-16d3-43bb-b5cb-710420202020"), FarmId, "Rice seed",
        InventoryCategory.Seeds, "kg", "Keep dry", 20m, 5m, false, null, "AQIDBAUGBwg=");

    [Theory]
    [InlineData("Owner")]
    [InlineData("Manager")]
    public async Task Phase5InvalidOptionalSupplyFieldKeepsDraftAndOpensDisclosure(string role)
    {
        using var app = App(role, inventoryItem: Phase5Supply);
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var route = $"/Inventory/Edit?farmId={FarmId}&itemId={Phase5Supply.Id}";
        var html = await client.GetStringAsync(route);
        using var response = await client.PostAsync(route, Form(html,
            ("Input.Name", "Changed supply"), ("Input.Category", "1"), ("Input.Unit", "kg"),
            ("Input.Description", "Preserve this draft"), ("Input.ReorderLevel", "-1"),
            ("Input.RowVersion", Phase5Supply.RowVersion)));
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var draft = await response.Content.ReadAsStringAsync();
        Assert.Contains("Changed supply", draft);
        Assert.Contains("Preserve this draft", draft);
        Assert.Contains("value=\"-1\"", draft);
        Assert.Matches("<details[^>]+open", draft);
        Assert.Contains("#Input_ReorderLevel", draft);
        Assert.Contains(Phase5Supply.RowVersion, draft);
    }

    [Fact]
    public async Task Phase5RejectedSupplyEditPreservesDraftAndSubmittedVersion()
    {
        const string message = "The supply changed. Reload its current details before trying again.";
        using var parent = App(inventoryItem: Phase5Supply);
        using var app = parent.WithWebHostBuilder(builder => builder.ConfigureTestServices(services =>
            services.AddScoped(_ => Stub<IInventoryService>((name, _) => name switch {
                "GetAsync" => Task.FromResult<InventoryItemDetails?>(Phase5Supply),
                "UpdateAsync" => throw new FarmOperationException(FarmError.Conflict, message),
                _ => throw new NotSupportedException(name)
            }))));
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var route = $"/Inventory/Edit?farmId={FarmId}&itemId={Phase5Supply.Id}";
        var html = await client.GetStringAsync(route);
        const string submittedVersion = "CAcGBQQDAgE=";
        using var response = await client.PostAsync(route, Form(html,
            ("Input.Name", "Changed supply"), ("Input.Category", "1"), ("Input.Unit", "kg"),
            ("Input.Description", "Preserve this draft"), ("Input.ReorderLevel", "2"),
            ("Input.RowVersion", submittedVersion)));
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var draft = await response.Content.ReadAsStringAsync();
        Assert.Contains("Changed supply", draft);
        Assert.Contains("Preserve this draft", draft);
        Assert.Contains(submittedVersion, draft);
        Assert.DoesNotContain(Phase5Supply.RowVersion, draft);
        Assert.Single(Regex.Matches(draft, Regex.Escape(message)));
    }

    [Fact]
    public async Task Phase5SupplyEditRemainsUnavailableToFieldWorker()
    {
        using var app = App("FieldWorker", inventoryItem: Phase5Supply);
        using var client = app.CreateClient();
        Assert.Equal(HttpStatusCode.NotFound,
            (await client.GetAsync($"/Inventory/Edit?farmId={FarmId}&itemId={Phase5Supply.Id}")).StatusCode);
    }

    [Fact]
    public async Task Phase5AuthenticatedBrowserFixtures()
    {
        if (Environment.GetEnvironmentVariable("ANIGO_CAPTURE_AUTH_FIXTURES") != "1") return;
        var root = new DirectoryInfo(AppContext.BaseDirectory);
        while (root is not null && !File.Exists(Path.Combine(root.FullName, "SmartFarm.slnx"))) root = root.Parent;
        Assert.NotNull(root);
        var directory = Path.Combine(root.FullName, "tests", "browser", "authenticated-fixtures");
        Directory.CreateDirectory(directory);
        foreach (var role in new[] { "Owner", "Manager" })
        {
            using var app = App(role, inventoryItem: Phase5Supply);
            using var client = app.CreateClient();
            var html = await client.GetStringAsync($"/Inventory/Edit?farmId={FarmId}&itemId={Phase5Supply.Id}");
            await File.WriteAllTextAsync(Path.Combine(directory, $"phase5-supply-edit-{role}.html"), html);
            if (role == "Manager")
                await File.WriteAllTextAsync(Path.Combine(directory, "phase5-tasks-Manager.html"),
                    await client.GetStringAsync($"/Tasks?farmId={FarmId}&view=all"));
        }
    }
}
