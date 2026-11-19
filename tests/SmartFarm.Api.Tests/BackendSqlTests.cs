using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using SmartFarm.Core.Contracts.Query;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Infrastructure.Development;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class BackendSqlFixture : IAsyncLifetime
{
    public WebApplicationFactory<Program>? Factory { get; private set; }
    public string? Connection => Environment.GetEnvironmentVariable("SMARTFARM_TEST_CONNECTION");
    public async Task InitializeAsync()
    {
        if (string.IsNullOrWhiteSpace(Connection))
        {
            Assert.NotEqual("1", Environment.GetEnvironmentVariable("SMARTFARM_REQUIRE_SQL_TESTS"));
            return;
        }
        Assert.StartsWith("SmartFarmEnrollmentBackend_", new SqlConnectionStringBuilder(Connection).InitialCatalog);
        Factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.UseEnvironment("Development");
            builder.ConfigureAppConfiguration((_, config) => config.AddInMemoryCollection(new Dictionary<string, string?>
            { ["ConnectionStrings:SmartFarm"] = Connection }));
            builder.ConfigureServices(services =>
            {
                services.RemoveAll<SmartFarmDbContext>(); services.RemoveAll<DbContextOptions<SmartFarmDbContext>>();
                services.AddDbContext<SmartFarmDbContext>(o => o.UseSqlServer(Connection));
            });
        });
        using var scope = Factory.Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();
        if (await db.Farms.AnyAsync(f => f.Id == DemoDataset.Id("farm0"))) return;
        var rows = DemoDataset.Create(DateTime.UtcNow);
        foreach (var user in rows.OfType<SmartFarmUser>())
            user.PasswordHash = new PasswordHasher<SmartFarmUser>().HashPassword(user, "BackendTest2026!");
        db.AddRange(rows); await db.SaveChangesAsync();
    }
    public async Task<HttpClient> Client(string role)
    {
        var client = Factory!.CreateClient();
        using var response = await client.PostAsJsonAsync("/api/auth/login", new { email = $"anigo-demo-{role}@example.test", password = "BackendTest2026!" });
        response.EnsureSuccessStatusCode();
        var json = await response.Content.ReadFromJsonAsync<JsonElement>();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", json.GetProperty("accessToken").GetString());
        return client;
    }
    public Task DisposeAsync() { Factory?.Dispose(); return Task.CompletedTask; }
}

public sealed class BackendSqlTests(BackendSqlFixture fixture) : IClassFixture<BackendSqlFixture>
{
    [Fact]
    public async Task PagesAreBoundedStableFilteredAndFarmAuthorized()
    {
        if (fixture.Factory is null) return;
        using var client = await fixture.Client("owner");
        var root = $"/api/farms/{DemoDataset.Id("farm0")}/records/";
        var page = await client.GetFromJsonAsync<PageResult<FarmTaskDetails>>(root + "tasks?pageSize=1&sort=asc&status=0");
        Assert.Equal(3, page!.TotalCount); Assert.True(page.HasMore); Assert.Single(page.Items);
        var second = await client.GetFromJsonAsync<PageResult<FarmTaskDetails>>(root + "tasks?pageSize=1&page=2&sort=asc&status=0");
        Assert.NotEqual(page.Items[0].Id, second!.Items[0].Id);
        Assert.Equal(page.Items[0].Id, (await client.GetFromJsonAsync<PageResult<FarmTaskDetails>>(root + "tasks?pageSize=1&sort=asc&status=0"))!.Items[0].Id);
        Assert.Empty((await client.GetFromJsonAsync<PageResult<FarmTaskDetails>>(root + "tasks?page=1000"))!.Items);
        Assert.Equal(HttpStatusCode.BadRequest, (await client.GetAsync(root + "tasks?pageSize=101")).StatusCode);
        foreach (var resource in new[] { "activities", "observations", "harvests", "inventory", "movements", "members", "enrollment" })
        {
            using var response = await client.GetAsync(root + resource + "?pageSize=1&search=demo");
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
            var result = await response.Content.ReadFromJsonAsync<JsonElement>();
            Assert.InRange(result.GetProperty("items").GetArrayLength(), 0, 1);
        }
        using var worker = await fixture.Client("worker");
        Assert.Equal(HttpStatusCode.NotFound, (await worker.GetAsync(root + "members")).StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, (await worker.GetAsync(root + "enrollment")).StatusCode);
        using var outsider = await fixture.Client("applicant");
        Assert.Equal(HttpStatusCode.NotFound, (await outsider.GetAsync(root + "tasks")).StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, (await client.GetAsync($"/api/farms/{Guid.NewGuid()}/records/tasks")).StatusCode);
        Assert.Equal(4, (await client.GetFromJsonAsync<List<FarmTaskDetails>>($"/api/farms/{DemoDataset.Id("farm0")}/tasks"))!.Count);
    }

    [Fact]
    public async Task LanguagePreferencePersistsAndOldTimestampsHaveUtcSuffix()
    {
        if (fixture.Factory is null) return;
        using var client = await fixture.Client("manager");
        using var put = await client.PutAsJsonAsync("/api/auth/preferences", new { language = "fil" });
        Assert.Equal(HttpStatusCode.OK, put.StatusCode);
        var pref = await client.GetFromJsonAsync<JsonElement>("/api/auth/preferences");
        Assert.Equal("fil", pref.GetProperty("language").GetString());
        Assert.Equal(HttpStatusCode.BadRequest, (await client.PutAsJsonAsync("/api/auth/preferences", new { language = "sql" })).StatusCode);
        var data = await client.GetFromJsonAsync<JsonElement>($"/api/farms/{DemoDataset.Id("farm0")}/records/activities");
        Assert.EndsWith("Z", data.GetProperty("items")[0].GetProperty("record").GetProperty("occurredAtUtc").GetString());
    }
}
