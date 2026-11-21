# Report route and missing-database health test fixes

> Source: SmartFarm working tree based on d4f7ab245547a06b62d099a82f91016bfc3b4f6f, local test execution
> Collected: 2026-10-27
> Published: Unknown

## Observed reproduction

The original DashboardAndHarvestReportUseTheProtectedApiRoutes test failed because expected report path included the query string but the recorded AbsolutePath did not. The original DevelopmentHealthExplainsMissingDatabaseConnection test expected ServiceUnavailable but got OK. The Development host reads Web user secrets as a fallback for an empty API connection string.

## Validation scope

The corrected targeted run passed eight tests. The full backend suite passed with SMARTFARM_TEST_CONNECTION unset and SMARTFARM_ENROLLMENT_TEST_CONNECTION absent. SQL test methods return early without those settings; no SQL integration coverage is claimed. No application source, schema, production configuration or business logic was changed. No Android or browser checks were run. MSBuild needed execution outside the sandbox for local named pipes; two stalled sandbox runs were terminated.

## Full suite result

Command: env -u SMARTFARM_TEST_CONNECTION dotnet test SmartFarm.slnx --no-restore --verbosity minimal -m:1 -nr:false --logger trx --results-directory /private/tmp/smartfarm-host-test-results

host-test-fixes_net10.0_20261027225613.trx: passed 71, failed 0, total 71.

host-test-fixes_net10.0_20261027225617.trx: passed 138, failed 0, total 138.

host-test-fixes_net10.0_20261027225618.trx: passed 14, failed 0, total 14.

host-test-fixes_net10.0_20261027225620.trx: passed 37, failed 0, total 37.

host-test-fixes_net10.0_20261027225621.trx: passed 47, failed 0, total 47.

Total passed: 307 = 71 + 138 + 14 + 37 + 47. All five backend test projects completed successfully. Existing xUnit2000 analyzer warnings in AuthorizationSchemeTests were observed during compilation and were not changed.


## Repository file: tests/SmartFarm.Auth.Tests/BusinessClientRouteTests.cs

using System.Net;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using SmartFarm.Web.Clients;
using Xunit;

namespace SmartFarm.Auth.Tests;

// Check MVC-to-API paths without needing a running SQL Server.
public sealed class BusinessClientRouteTests : IClassFixture<WebApplicationFactory<Program>>
{
    private static readonly Guid FarmId = Guid.NewGuid();
    private static readonly Guid PlotId = Guid.NewGuid();
    private static readonly Guid MemberId = Guid.NewGuid();
    private static readonly Guid CycleId = Guid.NewGuid();
    private static readonly Guid ObservationId = Guid.NewGuid();
    private static readonly Guid ItemId = Guid.NewGuid();
    private static readonly Guid TransactionId = Guid.NewGuid();
    private static readonly Guid HarvestId = Guid.NewGuid();
    private static readonly Guid TaskId = Guid.NewGuid();

    private static string FarmRoute => $"api/farms/{FarmId}";
    private static string PlotRoute => $"{FarmRoute}/plots/{PlotId}";
    private static string CropRoute => $"{FarmRoute}/crop-cycles/{CycleId}";
    private static string MonitoringRoute => $"{CropRoute}/monitoring";
    private static string InventoryRoute => $"{FarmRoute}/inventory";
    private static string ItemRoute => $"{InventoryRoute}/{ItemId}";
    private static string HarvestRoute => $"{CropRoute}/harvests";

    private readonly WebApplicationFactory<Program> _factory;

    public BusinessClientRouteTests(WebApplicationFactory<Program> factory) => _factory = factory;

    private async Task VerifyAsync(HttpMethod method, string path,
        Func<SmartFarmApiClient, Guid, Task> operation)
    {
        var actor = Guid.NewGuid();
        var identity = new ClaimsIdentity(
            [new Claim(ClaimTypes.NameIdentifier, actor.ToString())],
            IdentityConstants.ApplicationScheme);
        var accessor = new HttpContextAccessor
        {
            HttpContext = new DefaultHttpContext { User = new ClaimsPrincipal(identity) }
        };
        var handler = new RouteHandler();
        using var http = new HttpClient(handler) { BaseAddress = new Uri("https://api.example.test/") };
        var options = _factory.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>();
        var gateway = new SmartFarmApiClient(http, accessor, options);

        var error = await Assert.ThrowsAsync<FarmOperationException>(() => operation(gateway, actor));

        Assert.Equal(FarmError.NotFound, error.Error);
        Assert.Equal(1, handler.RequestCount);
        Assert.Equal(method, handler.Method);
        Assert.Equal("/" + path, handler.Path);
        Assert.Equal("Bearer", handler.AuthorizationScheme);
    }

    [Fact]
    public async Task FarmPlotAndMembershipRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, "api/farms",
            (api, actor) => new FarmApiService(api).ListAsync(actor));
        await VerifyAsync(HttpMethod.Post, "api/farms",
            (api, actor) => new FarmApiService(api).CreateAsync(actor, new FarmInput()));
        await VerifyAsync(HttpMethod.Put, FarmRoute,
            (api, actor) => new FarmApiService(api).UpdateAsync(FarmId, actor, new FarmInput()));
        await VerifyAsync(HttpMethod.Delete, FarmRoute,
            (api, actor) => new FarmApiService(api).ArchiveAsync(FarmId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/plots",
            (api, actor) => new FarmApiService(api).ListPlotsAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, FarmRoute + "/plots",
            (api, actor) => new FarmApiService(api).CreatePlotAsync(FarmId, actor, new PlotInput()));
        await VerifyAsync(HttpMethod.Put, PlotRoute,
            (api, actor) => new FarmApiService(api).UpdatePlotAsync(FarmId, PlotId, actor, new PlotInput()));
        await VerifyAsync(HttpMethod.Delete, PlotRoute,
            (api, actor) => new FarmApiService(api).ArchivePlotAsync(FarmId, PlotId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/members",
            (api, actor) => new FarmApiService(api).MembersAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, FarmRoute + "/members/invitations",
            (api, actor) => new FarmApiService(api).CreateInvitationAsync(FarmId, actor, new InviteMemberInput()));
        await VerifyAsync(HttpMethod.Post, "api/farm-invitations/accept",
            (api, actor) => new FarmApiService(api).AcceptInvitationAsync(actor, "token"));
        await VerifyAsync(HttpMethod.Put, FarmRoute + $"/members/{MemberId}",
            (api, actor) => new FarmApiService(api).ChangeMemberRoleAsync(FarmId, MemberId, actor, 1));
        await VerifyAsync(HttpMethod.Delete, FarmRoute + $"/members/{MemberId}",
            (api, actor) => new FarmApiService(api).RemoveMemberAsync(FarmId, MemberId, actor));
    }

    [Fact]
    public async Task CropRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/crop-types",
            (api, actor) => new CropApiService(api).ListCropTypesAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, FarmRoute + "/crop-types",
            (api, actor) => new CropApiService(api).CreateCropTypeAsync(FarmId, actor, new CropTypeInput()));
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/crop-cycles",
            (api, actor) => new CropApiService(api).ListCyclesAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, PlotRoute + "/crop-cycles",
            (api, actor) => new CropApiService(api).PlanAsync(FarmId, PlotId, actor, new CropCyclePlanInput()));
        await VerifyAsync(HttpMethod.Put, CropRoute,
            (api, actor) => new CropApiService(api).UpdatePlanAsync(FarmId, CycleId, actor, new CropCyclePlanInput()));
        await VerifyAsync(HttpMethod.Post, CropRoute + "/transitions",
            (api, actor) => new CropApiService(api).TransitionAsync(FarmId, CycleId, actor, new CropCycleTransitionInput()));
    }

    [Fact]
    public async Task MonitoringRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, MonitoringRoute,
            (api, actor) => new MonitoringApiService(api).GetAsync(FarmId, CycleId, actor));
        await VerifyAsync(HttpMethod.Post, MonitoringRoute + "/activities",
            (api, actor) => new MonitoringApiService(api).RecordActivityAsync(FarmId, CycleId, actor, new RecordActivityInput()));
        await VerifyAsync(HttpMethod.Post, MonitoringRoute + "/observations",
            (api, actor) => new MonitoringApiService(api).RecordObservationAsync(FarmId, CycleId, actor, new RecordObservationInput()));
        using var photo = new MemoryStream(new byte[] { 1, 2, 3 });
        await VerifyAsync(HttpMethod.Post, MonitoringRoute + $"/observations/{ObservationId}/photos",
            (api, actor) => new MonitoringApiService(api).AddPhotoAsync(
                FarmId, CycleId, ObservationId, actor, photo, photo.Length, "image/png"));
        var retryPhotoId = Guid.NewGuid();
        using var retryPhoto = new MemoryStream(new byte[] { 1, 2, 3 });
        await VerifyAsync(HttpMethod.Put,
            MonitoringRoute + $"/observations/{ObservationId}/photos/{retryPhotoId}",
            (api, actor) => new MonitoringApiService(api).AddPhotoWithIdAsync(
                FarmId, CycleId, ObservationId, retryPhotoId, actor,
                retryPhoto, retryPhoto.Length, "image/png"));
        await VerifyAsync(HttpMethod.Get, MonitoringRoute + $"/photos/{ObservationId}",
            (api, actor) => new MonitoringApiService(api).OpenPhotoAsync(FarmId, CycleId, ObservationId, actor));
    }

    [Fact]
    public async Task InventoryRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, InventoryRoute,
            (api, actor) => new InventoryApiService(api).ListAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, InventoryRoute,
            (api, actor) => new InventoryApiService(api).CreateAsync(FarmId, actor, new InventoryItemInput()));
        await VerifyAsync(HttpMethod.Put, ItemRoute,
            (api, actor) => new InventoryApiService(api).UpdateAsync(FarmId, ItemId, actor, new InventoryItemInput()));
        await VerifyAsync(HttpMethod.Delete, ItemRoute,
            (api, actor) => new InventoryApiService(api).ArchiveAsync(FarmId, ItemId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, ItemRoute + "/transactions",
            (api, actor) => new InventoryApiService(api).HistoryAsync(FarmId, ItemId, actor));
        await VerifyAsync(HttpMethod.Post, ItemRoute + "/transactions",
            (api, actor) => new InventoryApiService(api).PostAsync(FarmId, ItemId, actor, new InventoryMovementInput()));
        await VerifyAsync(HttpMethod.Post, ItemRoute + $"/transactions/{TransactionId}/reverse",
            (api, actor) => new InventoryApiService(api).ReverseAsync(FarmId, ItemId, TransactionId, actor, new InventoryReversalInput()));
        await VerifyAsync(HttpMethod.Get, InventoryRoute + "/activities",
            (api, actor) => new InventoryApiService(api).ActivityOptionsAsync(FarmId, actor));
    }

    [Fact]
    public async Task DashboardAndHarvestReportUseTheProtectedApiRoutes()
    {
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/dashboard",
            (api, actor) => new FarmReportApiService(api).DashboardAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Get,
            FarmRoute + "/reports/harvests?from=2026-10-01&to=2026-10-20" +
            $"&plotId={PlotId}&cropTypeId={CycleId}",
            (api, actor) => new FarmReportApiService(api).HarvestsAsync(
                FarmId, actor, new DateOnly(2026, 10, 1),
                new DateOnly(2026, 10, 20), PlotId, CycleId));
    }

    [Fact]
    public async Task TaskEditingUsesTheFarmApiRoute()
    {
        await VerifyAsync(HttpMethod.Put, FarmRoute + $"/tasks/{TaskId}",
            (api, actor) => new FarmTaskApiService(api).UpdateAsync(
                FarmId, TaskId, actor, new FarmTaskUpdateInput(
                    new DateOnly(2026, 10, 20), "Water plants", null, "version")));
    }

    [Fact]
    public async Task HarvestRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, HarvestRoute,
            (api, actor) => new HarvestApiService(api).GetAsync(FarmId, CycleId, actor));
        await VerifyAsync(HttpMethod.Post, HarvestRoute,
            (api, actor) => new HarvestApiService(api).RecordAsync(FarmId, CycleId, actor, new RecordHarvestInput()));
        await VerifyAsync(HttpMethod.Post, HarvestRoute + $"/{HarvestId}/void",
            (api, actor) => new HarvestApiService(api).VoidAsync(FarmId, CycleId, HarvestId, actor, new VoidHarvestInput()));
    }

    private sealed class RouteHandler : HttpMessageHandler
    {
        public int RequestCount { get; private set; }
        public HttpMethod? Method { get; private set; }
        public string? Path { get; private set; }
        public string? AuthorizationScheme { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request, CancellationToken cancellationToken)
        {
            RequestCount++;
            Method = request.Method;
            Path = request.RequestUri?.PathAndQuery;
            AuthorizationScheme = request.Headers.Authorization?.Scheme;
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.NotFound));
        }
    }
}


## Repository file: tests/SmartFarm.Api.Tests/DatabaseHealthTests.cs

using System.Net;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class DatabaseHealthTests
{
    [Fact]
    public async Task DevelopmentHealthExplainsMissingDatabaseConnection()
    {
        using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.UseEnvironment("Development");
            // Local user-secrets fallback must not supply a real database to this test.
            builder.ConfigureServices(services => services.AddScoped(_ =>
                new SmartFarmDbContext(new DbContextOptionsBuilder<SmartFarmDbContext>()
                    .UseSqlServer().Options)));
        });
        using var client = factory.CreateClient();
        using var response = await client.GetAsync("/health/database");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        var text = await response.Content.ReadAsStringAsync();
        Assert.Contains("ConnectionStrings:SmartFarm", text);
        Assert.DoesNotContain("Password=", text);
    }
}


## Repository file: src/SmartFarm.Web/Clients/FarmReportApiService.cs

using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Interfaces.Reports;

namespace SmartFarm.Web.Clients;

public sealed class FarmReportApiService(SmartFarmApiClient api) : IFarmReportService
{
    public Task<FarmDashboard> DashboardAsync(Guid farmId, Guid actor,
        CancellationToken ct = default) =>
        api.ReadAsync<FarmDashboard>($"api/farms/{farmId}/dashboard", actor, ct);

    public Task<HarvestReport> HarvestsAsync(Guid farmId, Guid actor,
        DateOnly? from = null, DateOnly? to = null, Guid? plotId = null,
        Guid? cropTypeId = null, CancellationToken ct = default)
    {
        var query = new List<string>();
        if (from is not null) query.Add($"from={from:yyyy-MM-dd}");
        if (to is not null) query.Add($"to={to:yyyy-MM-dd}");
        if (plotId is not null) query.Add($"plotId={plotId}");
        if (cropTypeId is not null) query.Add($"cropTypeId={cropTypeId}");
        var path = $"api/farms/{farmId}/reports/harvests";
        if (query.Count > 0) path += "?" + string.Join("&", query);
        return api.ReadAsync<HarvestReport>(path, actor, ct);
    }
}


## Repository file: src/SmartFarm.Api/Program.cs

using System.Data.Common;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Integrations.Weather;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Services.Crops;
using SmartFarm.Infrastructure.Services.Farms;
using SmartFarm.Infrastructure.Services.Harvests;
using SmartFarm.Infrastructure.Services.Inventory;
using SmartFarm.Infrastructure.Services.Monitoring;
using SmartFarm.Infrastructure.Services.Reports;
using SmartFarm.Infrastructure.Services.Tasks;

var builder = WebApplication.CreateBuilder(args);

// In local Development, Web and API share SmartFarmDB. If only Web's user
// secrets were configured, reuse just its database connection (no other secrets).
// An explicit API connection setting always wins. Production is unaffected.
if (builder.Environment.IsDevelopment() &&
    string.IsNullOrWhiteSpace(builder.Configuration.GetConnectionString("SmartFarm")))
{
    var webDatabase = new ConfigurationBuilder()
        .AddUserSecrets("SmartFarm.Web.Development")
        .Build()
        .GetConnectionString("SmartFarm");
    if (!string.IsNullOrWhiteSpace(webDatabase))
    {
        builder.Configuration.AddInMemoryCollection(new Dictionary<string, string?>
        {
            ["ConnectionStrings:SmartFarm"] = webDatabase
        });
    }
}

builder.Services.AddControllers();
builder.Services.AddSmartFarmPersistence(builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmBusinessServices();
builder.Services.AddSmartFarmWeather(builder.Configuration);
builder.Services.AddSmartFarmApiIdentity();
builder.Services.AddScoped<AccountRegistrationService>();
builder.Services.AddSingleton<IObservationPhotoStore>(_ =>
    new LocalObservationPhotoStore(
        builder.Configuration["ObservationPhotos:StoragePath"] ??
        (builder.Environment.IsDevelopment()
            ? Path.GetFullPath(Path.Combine(builder.Environment.ContentRootPath, "..", "SmartFarm.Web",
                "App_Data", "observation-photos"))
            : Path.Combine(builder.Environment.ContentRootPath, "App_Data", "observation-photos"))));
// Web and API must use the same protected key ring when hosted on different machines.
var dataProtection = builder.Services.AddDataProtection()
    .SetApplicationName($"SmartFarm.SharedIdentity.{builder.Environment.EnvironmentName}");
if (builder.Configuration["DataProtection:KeyRingPath"] is { Length: > 0 } keyRingPath)
{
    if (!Path.IsPathFullyQualified(keyRingPath))
        throw new InvalidOperationException("DataProtection:KeyRingPath must be an absolute path.");
    dataProtection.PersistKeysToFileSystem(new DirectoryInfo(keyRingPath));
}

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler(handler =>
    {
        handler.Run(async context =>
        {
            context.Response.StatusCode = StatusCodes.Status500InternalServerError;
            context.Response.ContentType = "text/plain";
            await context.Response.WriteAsync("An unexpected error occurred.");
        });
    });
    app.UseHsts();
    app.UseHttpsRedirection();
}

// Return a predictable response for database failures without exposing SQL details.
// The full provider error remains in the API terminal logs.
app.Use(async (context, next) =>
{
    try { await next(); }
    catch (DbException error) when (!context.Response.HasStarted)
    {
        context.RequestServices.GetRequiredService<ILoggerFactory>()
            .CreateLogger("SmartFarm.Api.Database")
            .LogError(error, "Database failure processing {Method} {Path}",
                context.Request.Method, context.Request.Path);
        context.Response.Clear();
        context.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;
        await context.Response.WriteAsJsonAsync(new
        {
            status = "unhealthy",
            message = "SmartFarm database request failed. Check the API terminal and /health/database."
        });
    }
});

app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

if (app.Environment.IsDevelopment())
{
    app.MapGet("/health/database", async (SmartFarmDbContext db,
        ILogger<Program> logger, CancellationToken ct) =>
    {
        static IResult Unhealthy(string message) =>
            Results.Json(new { status = "unhealthy", message },
                statusCode: StatusCodes.Status503ServiceUnavailable);

        try
        {
            if (string.IsNullOrWhiteSpace(db.Database.GetConnectionString()))
                return Unhealthy("Set ConnectionStrings:SmartFarm for SmartFarmDB.");

            if (!await db.Database.CanConnectAsync(ct))
                return Unhealthy("Cannot connect to SQL Server. Check the connection string and server.");

            // Connecting isn't enough: an empty or wrong database also opens successfully.
            // Probe the tables needed by authentication and the My Farms screen, read-only.
            await db.Users.AsNoTracking().Select(u => u.Id).Take(1).ToListAsync(ct);
            await db.Farms.AsNoTracking().Select(f => f.Id).Take(1).ToListAsync(ct);
            await db.FarmMembers.AsNoTracking().Select(m => m.Id).Take(1).ToListAsync(ct);

            return Results.Ok(new { status = "ok", message = "Database and main farm tables are ready." });
        }
        catch (Microsoft.Data.SqlClient.SqlException error) when (error.Number is 208 or 207)
        {
            logger.LogError(error, "Required SmartFarm schema is missing or incompatible");
            return Unhealthy("SmartFarmDB is missing required farm or identity tables. " +
                "Verify the schema; do not rerun initialization on an existing database.");
        }
        catch (Exception error)
        {
            logger.LogError(error, "SmartFarm database health check failed");
            return Unhealthy("Database check failed. Read the SmartFarm.Api terminal for the reason.");
        }
    });
}

app.MapControllers();
app.Run();

public partial class Program { }


## Reviewed patch

diff --git a/tests/SmartFarm.Api.Tests/DatabaseHealthTests.cs b/tests/SmartFarm.Api.Tests/DatabaseHealthTests.cs
index 9fe6014..6da1835 100644
--- a/tests/SmartFarm.Api.Tests/DatabaseHealthTests.cs
+++ b/tests/SmartFarm.Api.Tests/DatabaseHealthTests.cs
@@ -1,7 +1,9 @@
 using System.Net;
 using Microsoft.AspNetCore.Hosting;
 using Microsoft.AspNetCore.Mvc.Testing;
-using Microsoft.Extensions.Configuration;
+using Microsoft.EntityFrameworkCore;
+using Microsoft.Extensions.DependencyInjection;
+using SmartFarm.Infrastructure.Persistence;
 using Xunit;
 
 namespace SmartFarm.Api.Tests;
@@ -14,11 +16,10 @@ public sealed class DatabaseHealthTests
         using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
         {
             builder.UseEnvironment("Development");
-            builder.ConfigureAppConfiguration((_, configuration) =>
-                configuration.AddInMemoryCollection(new Dictionary<string, string?>
-                {
-                    ["ConnectionStrings:SmartFarm"] = ""
-                }));
+            // Local user-secrets fallback must not supply a real database to this test.
+            builder.ConfigureServices(services => services.AddScoped(_ =>
+                new SmartFarmDbContext(new DbContextOptionsBuilder<SmartFarmDbContext>()
+                    .UseSqlServer().Options)));
         });
         using var client = factory.CreateClient();
         using var response = await client.GetAsync("/health/database");
diff --git a/tests/SmartFarm.Auth.Tests/BusinessClientRouteTests.cs b/tests/SmartFarm.Auth.Tests/BusinessClientRouteTests.cs
index 06155be..bf5ab23 100644
--- a/tests/SmartFarm.Auth.Tests/BusinessClientRouteTests.cs
+++ b/tests/SmartFarm.Auth.Tests/BusinessClientRouteTests.cs
@@ -206,7 +206,7 @@ public sealed class BusinessClientRouteTests : IClassFixture<WebApplicationFacto
         {
             RequestCount++;
             Method = request.Method;
-            Path = request.RequestUri?.AbsolutePath;
+            Path = request.RequestUri?.PathAndQuery;
             AuthorizationScheme = request.Headers.Authorization?.Scheme;
             return Task.FromResult(new HttpResponseMessage(HttpStatusCode.NotFound));
         }
