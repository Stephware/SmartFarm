# SmartFarm backend-current source capture

> Source: local repository at ac624be7e5e721f00d990d01f720c682f1977930
> Collected: 2026-11-11
> Published: Unknown

Source evidence only. No build, deployment, SQL or device success is implied.

## `SmartFarm.slnx`

````text
<Solution>
  <Folder Name="/src/">
    <Project Path="src/SmartFarm.Api/SmartFarm.Api.csproj" />
    <Project Path="src/SmartFarm.Core/SmartFarm.Core.csproj" />
    <Project Path="src/SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj" />
    <Project Path="src/SmartFarm.Web/SmartFarm.Web.csproj" />
    <Project Path="src/SmartFarm.Worker/SmartFarm.Worker.csproj" />
  </Folder>
  <Folder Name="/tests/">
    <Project Path="tests/SmartFarm.Api.Tests/SmartFarm.Api.Tests.csproj" />
    <Project Path="tests/SmartFarm.Auth.Tests/SmartFarm.Auth.Tests.csproj" />
    <Project Path="tests/SmartFarm.SchemaVerification/SmartFarm.SchemaVerification.csproj" />
    <Project Path="tests/SmartFarm.Weather.Tests/SmartFarm.Weather.Tests.csproj" />
    <Project Path="tests/SmartFarm.Worker.Tests/SmartFarm.Worker.Tests.csproj" />
  </Folder>
</Solution>
````

## `Directory.Build.props`

````text
<Project>
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>
</Project>
````

## `global.json`

````text
{
  "sdk": {
    "version": "10.0.100",
    "rollForward": "latestFeature"
  }
}
````

## `.config/dotnet-tools.json`

````text
{
  "version": 1,
  "isRoot": true,
  "tools": {
    "dotnet-ef": {
      "version": "10.0.13",
      "commands": [
        "dotnet-ef"
      ],
      "rollForward": false
    }
  }
}
````

## `src/SmartFarm.Api/Program.cs`

````text
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
builder.Services.AddEnrollmentRateLimiting();
builder.Services.AddSmartFarmPersistence(builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmBusinessServices();
builder.Services.AddSmartFarmWeather(builder.Configuration);
builder.Services.AddSmartFarmApiIdentity();
builder.Services.AddScoped<AccountRegistrationService>();
builder.Services.AddSharedObservationPhotoStore(
    builder.Configuration, builder.Environment.IsDevelopment(),
    Path.GetFullPath(Path.Combine(builder.Environment.ContentRootPath, "..",
        "SmartFarm.Web", "App_Data", "observation-photos")));
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
app.UseRateLimiter();

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
````

## `src/SmartFarm.Web/Program.cs`

````text
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.HealthChecks;
using SmartFarm.Infrastructure.Services.Monitoring;
using SmartFarm.Infrastructure.Security;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews(options => options.Filters.AddService<SmartFarm.Web.Filters.ApiUnavailableFilter>());
builder.Services.AddScoped<SmartFarm.Web.Filters.ApiUnavailableFilter>();
builder.Services.AddEnrollmentRateLimiting();
builder.Services.AddSmartFarmPersistence(
    builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmWebIdentity(builder.Environment);
builder.Services.AddScoped<AccountRegistrationService>();

// MVC and API run independently using the same Infrastructure business services.
builder.Services.AddSmartFarmBusinessServices();
// In Development this matches the API's sibling SmartFarm.Web/App_Data photo store.
// Configure ObservationPhotos:StoragePath to the same shared directory on each host
// before deploying the MVC and API to separate instances.
builder.Services.AddSharedObservationPhotoStore(
    builder.Configuration, builder.Environment.IsDevelopment(),
    Path.GetFullPath(Path.Combine(builder.Environment.ContentRootPath,
        "App_Data", "observation-photos")));
// Web and API must use the same protected key ring when hosted on different machines.
var dataProtection = builder.Services.AddDataProtection()
    .SetApplicationName($"SmartFarm.SharedIdentity.{builder.Environment.EnvironmentName}");
if (builder.Configuration["DataProtection:KeyRingPath"] is { Length: > 0 } keyRingPath)
{
    if (!Path.IsPathFullyQualified(keyRingPath))
        throw new InvalidOperationException("DataProtection:KeyRingPath must be an absolute path.");
    dataProtection.PersistKeysToFileSystem(new DirectoryInfo(keyRingPath));
}

if (builder.Environment.IsDevelopment())
{
    builder.Services.AddHealthChecks()
        .AddCheck<DatabaseConnectivityHealthCheck>("database", timeout: TimeSpan.FromSeconds(5));
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

app.UseStaticFiles();
app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();
app.UseRateLimiter();

if (app.Environment.IsDevelopment())
{
    app.MapHealthChecks("/health/database", new HealthCheckOptions
    {
        Predicate = check => check.Name == "database"
    });
}

app.MapControllers();
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();

public partial class Program { }
````

## `src/SmartFarm.Infrastructure/DependencyInjection.cs`

````text
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Infrastructure.Services.Finance;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Services.Crops;
using SmartFarm.Infrastructure.Services.Farms;
using SmartFarm.Infrastructure.Services.Harvests;
using SmartFarm.Infrastructure.Services.Inventory;
using SmartFarm.Infrastructure.Services.Monitoring;
using SmartFarm.Infrastructure.Services.Reports;
using SmartFarm.Infrastructure.Services.Tasks;
using SmartFarm.Infrastructure.Services.Weather;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Repositories;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddSmartFarmPersistence(
        this IServiceCollection services,
        string? connectionString)
    {
        // Registration does not open a connection or change the existing schema.
        services.AddDbContext<SmartFarmDbContext>(options =>
            options.UseSqlServer(connectionString));

        return services;
    }

    public static IServiceCollection AddSmartFarmRepositories(this IServiceCollection services)
    {
        services.AddScoped<IFarmRepository, FarmRepository>();
        services.AddScoped<IFarmEnrollmentRepository, FarmEnrollmentRepository>();
        services.AddScoped<IUserRepository, UserRepository>();
        services.AddScoped<IPlotRepository, PlotRepository>();
        services.AddScoped<IFarmMemberRepository, FarmMemberRepository>();
        services.AddScoped<ICropTypeRepository, CropTypeRepository>();
        services.AddScoped<ICropCycleRepository, CropCycleRepository>();
        services.AddScoped<ICropActivityRepository, CropActivityRepository>();
        services.AddScoped<ICropObservationRepository, CropObservationRepository>();
        services.AddScoped<IObservationPhotoRepository, ObservationPhotoRepository>();
        services.AddScoped<IInventoryItemRepository, InventoryItemRepository>();
        services.AddScoped<IInventoryTransactionRepository, InventoryTransactionRepository>();
        services.AddScoped<IHarvestRepository, HarvestRepository>();
        services.AddScoped<IFarmTaskRepository, FarmTaskRepository>();
        services.AddScoped<IUnitOfWork, EfUnitOfWork>();
        return services;
    }

    // MVC and Web API use the same service implementations, without host-to-host HTTP calls.
    public static IServiceCollection AddSmartFarmBusinessServices(this IServiceCollection services)
    {
        services.AddSmartFarmRepositories();
        services.AddScoped<IFarmAccessService, FarmAccessService>();
        services.AddScoped<IFarmManagementService, FarmManagementService>();
        services.AddScoped<IFarmEnrollmentService, FarmEnrollmentService>();
        services.AddScoped<ICropManagementService, CropManagementService>();
        services.AddScoped<IMonitoringService, MonitoringService>();
        services.AddScoped<IFieldworkStockService, FieldworkStockService>();
        services.AddScoped<IInventoryService, InventoryService>();
        services.AddScoped<IHarvestService, HarvestService>();
        services.AddScoped<IFarmReportService, FarmReportService>();
        services.AddScoped<IFarmFinanceService, FarmFinanceService>();
        services.AddScoped<IFarmTaskService, FarmTaskService>();
        services.AddScoped<IFarmWeatherService, FarmWeatherService>();
        return services;
    }
}
````

## `src/SmartFarm.Infrastructure/Identity/IdentitySetup.cs`

````text
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Identity;

public static class IdentitySetup
{
    // The website uses cookies. Keep bearer support here temporarily until
    // Android is switched to SmartFarm.Api.
    public static IServiceCollection AddSmartFarmWebIdentity(
        this IServiceCollection services, IWebHostEnvironment environment)
    {
        services.AddIdentity<SmartFarmUser, SmartFarmRole>(ConfigureIdentity)
            .AddEntityFrameworkStores<SmartFarmDbContext>()
            .AddDefaultTokenProviders();

        services.AddAuthentication().AddBearerToken(IdentityConstants.BearerScheme, ConfigureBearer);

        services.ConfigureApplicationCookie(options =>
        {
            options.LoginPath = "/Account/Login";
            options.AccessDeniedPath = "/Account/AccessDenied";
            options.Cookie.Name = "SmartFarm.Auth";
            options.Cookie.HttpOnly = true;
            options.Cookie.SameSite = SameSiteMode.Lax;
            options.Cookie.SecurePolicy = environment.IsDevelopment()
                ? CookieSecurePolicy.SameAsRequest : CookieSecurePolicy.Always;
            options.SlidingExpiration = true;
            options.ExpireTimeSpan = TimeSpan.FromHours(8);
            options.Events.OnValidatePrincipal = async context =>
            {
                await SecurityStampValidator.ValidatePrincipalAsync(context);
                if (context.Principal?.Identity?.IsAuthenticated != true) return;

                var users = context.HttpContext.RequestServices
                    .GetRequiredService<UserManager<SmartFarmUser>>();
                var user = await users.GetUserAsync(context.Principal);
                if (user is null || user.DisabledAtUtc is not null)
                {
                    context.RejectPrincipal();
                    await context.HttpContext.SignOutAsync(IdentityConstants.ApplicationScheme);
                }
            };
        });

        services.AddAuthorization();
        return services;
    }

    // The API accepts bearer tokens only: no MVC cookie scheme or login redirects.
    public static IServiceCollection AddSmartFarmApiIdentity(this IServiceCollection services)
    {
        services.AddIdentityCore<SmartFarmUser>(ConfigureIdentity)
            .AddRoles<SmartFarmRole>()
            .AddEntityFrameworkStores<SmartFarmDbContext>()
            .AddSignInManager()
            .AddDefaultTokenProviders();

        services.AddHttpContextAccessor();
        services.AddAuthentication(IdentityConstants.BearerScheme)
            .AddBearerToken(IdentityConstants.BearerScheme, ConfigureBearer);
        services.AddAuthorization();
        return services;
    }

    private static void ConfigureIdentity(IdentityOptions options)
    {
        options.User.RequireUniqueEmail = true;
        options.Password.RequiredLength = 10;
        options.Password.RequireDigit = true;
        options.Password.RequireLowercase = true;
        options.Password.RequireUppercase = true;
        options.Password.RequireNonAlphanumeric = false;
        options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(10);
        options.Lockout.MaxFailedAccessAttempts = 5;
        options.Lockout.AllowedForNewUsers = true;
        options.SignIn.RequireConfirmedEmail = false;
    }

    private static void ConfigureBearer(Microsoft.AspNetCore.Authentication.BearerToken.BearerTokenOptions options)
    {
        // ASP.NET Core Identity tokens, not JWTs.
        options.BearerTokenExpiration = TimeSpan.FromMinutes(30);
        options.RefreshTokenExpiration = TimeSpan.FromDays(7);
    }
}
````

## `src/SmartFarm.Infrastructure/Security/FarmAccessService.cs`

````text
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Services.Security;

namespace SmartFarm.Infrastructure.Security;

public sealed class FarmAccessService(IUserRepository people, IFarmRepository farms,
    IFarmMemberRepository members) : IFarmAccessService
{
    public async Task<FarmAccessLevel> GetAccessLevelAsync(
        Guid farmId, Guid userId, CancellationToken cancellationToken = default)
    {
        if (farmId == Guid.Empty || userId == Guid.Empty ||
            !await people.IsActiveAsync(userId, cancellationToken))
            return FarmAccessLevel.None;

        var ownerId = await farms.GetOwnerIdAsync(farmId, cancellationToken);

        if (ownerId is null) return FarmAccessLevel.None;
        if (ownerId == userId) return FarmAccessLevel.Owner;

        var role = await members.GetActiveRoleAsync(farmId, userId, cancellationToken);

        return role switch
        {
            1 => FarmAccessLevel.Manager,
            2 => FarmAccessLevel.FieldWorker,
            _ => FarmAccessLevel.None
        };
    }

    public async Task<bool> CanAsync(
        Guid farmId, Guid userId, FarmAction action, CancellationToken cancellationToken = default) =>
        FarmAccessRules.Allows(await GetAccessLevelAsync(farmId, userId, cancellationToken), action);
}
````

## `src/SmartFarm.Infrastructure/Security/EnrollmentRateLimiting.cs`

````text
using System.Globalization;
using System.Security.Claims;
using System.Threading.RateLimiting;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;

namespace SmartFarm.Infrastructure.Security;

public static class EnrollmentRateLimiting
{
    public const string Registration = "registration";
    public const string Login = "login";
    public const string Enrollment = "enrollment";

    public static IServiceCollection AddEnrollmentRateLimiting(this IServiceCollection services) =>
        services.AddRateLimiter(options =>
        {
            AddPolicy(Registration, 5, TimeSpan.FromMinutes(10));
            AddPolicy(Login, 20, TimeSpan.FromMinutes(1));
            AddPolicy(Enrollment, 10, TimeSpan.FromMinutes(1), byUser: true);
            options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
            options.OnRejected = async (context, ct) =>
            {
                var response = context.HttpContext.Response;
                if (context.Lease.TryGetMetadata(MetadataName.RetryAfter, out var retryAfter))
                    response.Headers.RetryAfter = Math.Max(1, (long)Math.Ceiling(retryAfter.TotalSeconds))
                        .ToString(CultureInfo.InvariantCulture);
                await response.WriteAsJsonAsync(new
                {
                    message = "Too many attempts. Please wait and try again."
                }, ct);
            };

            void AddPolicy(string name, int permits, TimeSpan window, bool byUser = false) =>
                options.AddPolicy(name, context =>
                {
                    var user = context.User.Identity?.IsAuthenticated == true
                        ? context.User.FindFirstValue(ClaimTypes.NameIdentifier) : null;
                    var key = byUser && !string.IsNullOrEmpty(user)
                        ? "user:" + user
                        : "ip:" + (context.Connection.RemoteIpAddress?.ToString() ?? "unknown");
                    return RateLimitPartition.GetFixedWindowLimiter(key, _ => new FixedWindowRateLimiterOptions
                    {
                        PermitLimit = permits, Window = window, QueueLimit = 0, AutoReplenishment = true
                    });
                });
        });
}
````

## `src/SmartFarm.Infrastructure/Services/Farms/FarmEnrollmentService.cs`

````text
using System.Data;
using System.Security.Cryptography;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;

namespace SmartFarm.Infrastructure.Services.Farms;

public sealed class FarmEnrollmentService(IFarmEnrollmentRepository requests, IFarmMemberRepository members,
    IUnitOfWork unit) : IFarmEnrollmentService
{
    private static FarmOperationException Missing() => new(FarmError.NotFound, "Farm or request not found.");
    private static FarmOperationException InvalidCode() => new(FarmError.InvalidInput, "Enter a valid code for an active farm.");

    public async Task<FarmJoinRequestDetails> RequestAsync(Guid actor, string code, CancellationToken ct = default)
    {
        var normalized = (code ?? "").Trim().Replace("-", "").ToUpperInvariant();
        if (normalized.Length != 12 || normalized.Any(c => !char.IsAsciiHexDigit(c))) throw InvalidCode();
        await using var transaction = unit.HasActiveTransaction ? null :
            await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        // Every writer locks the farm first, so requests and decisions for it are serialized.
        var farm = await requests.FindFarmByCodeForUpdateAsync(normalized, ct) ?? throw InvalidCode();
        if (!await requests.IsActiveUserAsync(actor, ct)) throw Missing();
        if (farm.OwnerUserId == actor || await members.GetActiveAsync(farm.Id, actor, ct) is not null)
            throw new FarmOperationException(FarmError.Conflict, "You already belong to this farm.");
        var request = await requests.FindPendingAsync(farm.Id, actor, ct);
        if (request is null)
        {
            request = new FarmJoinRequest { FarmId = farm.Id, UserId = actor, RequestedAtUtc = DateTime.UtcNow };
            requests.Add(request);
            Notify(farm.OwnerUserId, request.Id, "requested", "New farm join request",
                $"A farmer requested to join {farm.Name}. Review join requests in My Farms.");
            await Save(ct);
        }
        var result = await requests.DetailsAsync(request.Id, ct);
        if (transaction is not null) await transaction.CommitAsync(ct);
        return result;
    }

    public async Task<IReadOnlyList<FarmJoinRequestDetails>> MyRequestsAsync(Guid actor, CancellationToken ct = default)
    {
        if (!await requests.IsActiveUserAsync(actor, ct)) return [];
        return await requests.ListForUserAsync(actor, ct);
    }

    private async Task<Farm> RequireOwner(Guid farmId, Guid actor, CancellationToken ct)
    {
        var farm = await requests.FindFarmForUpdateAsync(farmId, ct) ?? throw Missing();
        if (farm.OwnerUserId != actor || !await requests.IsActiveUserAsync(actor, ct)) throw Missing();
        return farm;
    }

    public async Task<FarmEnrollmentOverview> OwnerOverviewAsync(Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var farm = await RequireOwner(farmId, actor, ct);
        var history = await requests.ListForFarmAsync(farmId, ct);
        var result = new FarmEnrollmentOverview(farm.JoinCode,
            history.Where(r => r.Status == "Pending").ToList(),
            history.Where(r => r.Status != "Pending").OrderByDescending(r => r.RequestedAtUtc).ToList());
        await transaction.CommitAsync(ct);
        return result;
    }

    public async Task DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var farm = await RequireOwner(farmId, actor, ct);
        var request = await requests.FindAsync(farmId, requestId, ct) ?? throw Missing();
        if (request.Status != "Pending" && request.Status != (approve ? "Approved" : "Rejected"))
            throw new FarmOperationException(FarmError.Conflict, "This request has already been decided. Reload to see its status.");
        if (request.Status == "Pending")
        {
            if (approve)
            {
                if (!await requests.IsActiveUserAsync(request.UserId, ct)) throw Missing();
                if (await members.GetActiveAsync(farmId, request.UserId, ct) is null)
                    members.Add(new FarmMember { FarmId = farmId, UserId = request.UserId, Role = 2, JoinedAtUtc = DateTime.UtcNow });
            }
            request.Status = approve ? "Approved" : "Rejected";
            request.ReviewedByUserId = actor;
            request.ReviewedAtUtc = DateTime.UtcNow;
            var decision = approve ? "approved" : "rejected";
            Notify(request.UserId, request.Id, "decided", "Farm join request " + decision,
                $"Your request to join {farm.Name} was {decision}." +
                (approve ? " Open My Farms to access the farm." : " You can submit a new request with a valid farm code."));
            await Save(ct);
        }
        await transaction.CommitAsync(ct);
    }

    public async Task CancelAsync(Guid farmId, Guid requestId, Guid actor, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        if (await requests.FindFarmForUpdateAsync(farmId, ct) is null ||
            !await requests.IsActiveUserAsync(actor, ct)) throw Missing();
        var request = await requests.FindAsync(farmId, requestId, ct) ?? throw Missing();
        if (request.UserId != actor) throw Missing();
        if (request.Status is not ("Pending" or "Canceled"))
            throw new FarmOperationException(FarmError.Conflict, "This request has already been decided. Reload to see its status.");
        if (request.Status == "Pending")
        {
            request.Status = "Canceled";
            request.ReviewedByUserId = actor;
            request.ReviewedAtUtc = DateTime.UtcNow;
            await Save(ct);
        }
        await transaction.CommitAsync(ct);
    }

    public async Task<string> RegenerateCodeAsync(Guid farmId, Guid actor, string currentCode, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var farm = await RequireOwner(farmId, actor, ct);
        if (!string.Equals(farm.JoinCode, currentCode, StringComparison.Ordinal))
            throw new FarmOperationException(FarmError.Conflict, "The farm code changed. Reload before regenerating it.");
        for (var attempt = 0; ; attempt++)
        {
            do { farm.JoinCode = Convert.ToHexString(RandomNumberGenerator.GetBytes(6)); }
            while (farm.JoinCode == currentCode);
            try { await unit.SaveChangesAsync(ct); break; }
            catch (DbUpdateException error) when (attempt < 4 &&
                error.InnerException is SqlException { Number: 2601 or 2627 } sql &&
                sql.Message.Contains("UX_Farm_JoinCode", StringComparison.Ordinal)) { }
        }
        await transaction.CommitAsync(ct);
        return farm.JoinCode;
    }

    private void Notify(Guid userId, Guid requestId, string action, string title, string message) =>
        requests.Add(new Notification
        {
            UserId = userId, Type = 4, DeduplicationKey = $"enrollment:{requestId:N}:{action}",
            Title = title, Message = message, CreatedAtUtc = DateTime.UtcNow
        });

    private async Task Save(CancellationToken ct)
    {
        try { await unit.SaveChangesAsync(ct); }
        catch (DbUpdateException error) when (error.InnerException is SqlException { Number: 2601 or 2627 })
        {
            throw new FarmOperationException(FarmError.Conflict, "This enrollment changed. Reload and try again.");
        }
    }
}
````

## `src/SmartFarm.Infrastructure/Services/Monitoring/FieldworkStockService.cs`

````text
using System.Data;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Services.Inventory;

namespace SmartFarm.Infrastructure.Services.Monitoring;

/// <summary>
/// A fertilizer/pesticide field entry, matching task completion, and stock
/// consumption succeed or fail together. The sync endpoint provides stable
/// operation IDs; this service also respects its ambient SQL transaction.
/// </summary>
public sealed class FieldworkStockService(
    IMonitoringService monitoring,
    IInventoryService inventory,
    IUnitOfWork unit) : IFieldworkStockService
{
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);

    public async Task<ActivityWithStockDetails> RecordAsync(Guid farmId, Guid cycleId,
        Guid actor, RecordActivityWithStockInput input, CancellationToken ct = default)
    {
        if (farmId == Guid.Empty || cycleId == Guid.Empty || input is null ||
            input.Activity is null || input.InventoryItemId == Guid.Empty ||
            input.Activity.ActivityType is not (2 or 3) ||
            !StockRules.IsValidQuantity(input.QuantityUsed) ||
            input.Reason?.Trim().Length > 300)
            throw Invalid("Select a fertilizer/pesticide activity, valid supply and quantity.");

        var transaction = !unit.HasActiveTransaction
            ? await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct)
            : null;
        try
        {
            var stock = await inventory.GetAsync(farmId, input.InventoryItemId, actor, ct);
            if (stock is null || stock.ArchivedAtUtc is not null)
                throw Invalid("The selected supply is no longer available.");
            var requiredCategory = input.Activity.ActivityType == 2
                ? InventoryCategory.Fertilizers : InventoryCategory.Pesticides;
            if (stock.Category != requiredCategory)
                throw Invalid("The selected supply category does not match the field activity.");

            // Both operations use the same DbContext and transaction. If the
            // stock rowVersion or balance is stale, this activity (including
            // any automatically fulfilled task) is rolled back as well.
            var activity = await monitoring.RecordActivityAsync(
                farmId, cycleId, actor, input.Activity, ct);
            var updated = await inventory.PostAsync(farmId, input.InventoryItemId, actor,
                new InventoryMovementInput
                {
                    TransactionType = StockTransactionType.Consumption,
                    QuantityChange = -input.QuantityUsed,
                    CropActivityId = activity.Id,
                    Reason = input.Reason,
                    RowVersion = input.RowVersion
                }, ct);
            if (transaction is not null)
                await transaction.CommitAsync(ct);
            return new ActivityWithStockDetails(activity, updated);
        }
        finally
        {
            if (transaction is not null)
                await transaction.DisposeAsync();
        }
    }
}
````

## `src/SmartFarm.Core/Contracts/Monitoring/MonitoringContracts.cs`

````text
using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;
using SmartFarm.Core.Contracts;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts.Monitoring;

// CropActivity type: 1 Watering, 2 Fertilizing, 3 Pesticide,
// 4 Weeding, 5 Other. Inspections are CropObservations.
public sealed class RecordActivityInput
{
    [Range(1, 5)]
    [JsonConverter(typeof(ActivityTypeJsonConverter))]
    public byte ActivityType { get; set; }

    [StringLength(1000)]
    public string? Notes { get; set; }

    // Optional: link a field entry to a specific scheduled watering/fertilizing task.
    public Guid? FarmTaskId { get; set; }
}

public sealed class RecordObservationInput
{
    [Range(0, 3)]
    [JsonConverter(typeof(HealthStatusJsonConverter))]
    public byte HealthStatus { get; set; }

    [StringLength(40)]
    public string? GrowthStage { get; set; }

    [StringLength(1000)]
    public string? Notes { get; set; }

    [Range(0, 5)]
    public byte IssueCategory { get; set; }

    [Range(0, 3)]
    public byte IssueSeverity { get; set; }
}

public sealed class UpdateObservationFollowUpInput
{
    [Range(1, 2)]
    public byte FollowUpStatus { get; set; }

    public Guid? FollowUpTaskId { get; set; }

    [Required]
    public string RowVersion { get; set; } = string.Empty;
}

public sealed record ActivityDetails(
    Guid Id, byte ActivityType, DateTime OccurredAtUtc,
    DateTime RecordedAtUtc, Guid RecordedByUserId, string? Notes,
    Guid? FarmTaskId = null)
{
    public string ActivityTypeName => FarmCodeLabels.Activity(ActivityType);
}

public sealed record ObservationDetails(
    Guid Id, byte HealthStatus, string? GrowthStage,
    DateTime ObservedAtUtc, DateTime RecordedAtUtc,
    Guid RecordedByUserId, string? Notes,
    IReadOnlyList<PhotoDetails> Photos,
    byte IssueCategory = 0, byte IssueSeverity = 0,
    byte FollowUpStatus = 0, Guid? FollowUpTaskId = null,
    string RowVersion = "")
{
    public string HealthStatusName => FarmCodeLabels.Health(HealthStatus);
    public string IssueCategoryName => IssueCategory switch
    {
        1 => "Pest", 2 => "Disease", 3 => "Nutrient deficiency",
        4 => "Environmental stress", 5 => "Other", _ => "None"
    };
    public string IssueSeverityName => IssueSeverity switch
    {
        1 => "Low", 2 => "Moderate", 3 => "High", _ => "None"
    };
}

public sealed record PhotoDetails(
    Guid Id, Guid ObservationId, string ContentType,
    long FileSizeBytes, DateTime UploadedAtUtc);

public sealed record MonitoringDetails(
    Guid FarmId, Guid CycleId, bool CanRecord,
    IReadOnlyList<ActivityDetails> Activities,
    IReadOnlyList<ObservationDetails> Observations);
````

## `src/SmartFarm.Core/Contracts/Monitoring/ActivityStockContracts.cs`

````text
using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Contracts.Inventory;

namespace SmartFarm.Core.Contracts.Monitoring;

// A single field entry with stock consumption; not two independent writes.
// InventoryItemId identifies the stock, while the server supplies CropActivityId.
public sealed class RecordActivityWithStockInput
{
    [Required]
    public RecordActivityInput? Activity { get; set; }

    [Required]
    public Guid InventoryItemId { get; set; }

    [Range(typeof(decimal), "0.001", "999999999999999.999")]
    public decimal QuantityUsed { get; set; }

    [Required]
    public string RowVersion { get; set; } = string.Empty;

    [StringLength(300)]
    public string? Reason { get; set; }
}

public sealed record ActivityWithStockDetails(
    ActivityDetails Activity, InventoryItemDetails Inventory);
````

## `src/SmartFarm.Core/Contracts/NamedCodeJsonConverters.cs`

````text
using System.Text.Json;
using System.Text.Json.Serialization;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts;

// Allow clear symbolic JSON input while preserving the existing numeric JSON
// representation and SQL codes used by deployed/offline clients.
public abstract class NamedByteCodeConverter<TCode> : JsonConverter<byte>
    where TCode : struct, Enum
{
    public override byte Read(ref Utf8JsonReader reader, Type typeToConvert,
        JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.Number && reader.TryGetByte(out var numeric))
            return numeric;

        if (reader.TokenType == JsonTokenType.String)
        {
            var name = reader.GetString()?.Trim();
            if (!string.IsNullOrWhiteSpace(name))
            {
                // Web JSON options already accepted numeric strings; preserve that behavior.
                if (byte.TryParse(name, System.Globalization.NumberStyles.None,
                        System.Globalization.CultureInfo.InvariantCulture, out var numericString))
                    return numericString;
                foreach (var value in Enum.GetValues<TCode>())
                {
                    if (string.Equals(Normalize(value.ToString()), Normalize(name),
                            StringComparison.OrdinalIgnoreCase))
                        return Convert.ToByte(value);
                }
            }
        }

        throw new JsonException($"Expected a valid {typeof(TCode).Name} name or numeric code.");
    }

    public override void Write(Utf8JsonWriter writer, byte value, JsonSerializerOptions options) =>
        writer.WriteNumberValue(value);

    private static string Normalize(string value) =>
        new(value.Where(char.IsLetterOrDigit).ToArray());
}

public sealed class ActivityTypeJsonConverter : NamedByteCodeConverter<FarmActivityType> { }
public sealed class HealthStatusJsonConverter : NamedByteCodeConverter<CropHealthStatus> { }
public sealed class TaskTypeJsonConverter : NamedByteCodeConverter<FarmTaskType> { }
public sealed class TaskCloseStatusJsonConverter : NamedByteCodeConverter<FarmTaskStatus> { }
public sealed class MemberRoleJsonConverter : NamedByteCodeConverter<FarmMemberRole> { }

public sealed class NamedEnumCodeConverter<TCode> : JsonConverter<TCode>
    where TCode : struct, Enum
{
    public override TCode Read(ref Utf8JsonReader reader, Type typeToConvert,
        JsonSerializerOptions options)
    {
        if (reader.TokenType == JsonTokenType.Number && reader.TryGetByte(out var numeric))
            return (TCode)Enum.ToObject(typeof(TCode), numeric);

        if (reader.TokenType == JsonTokenType.String)
        {
            var name = reader.GetString()?.Trim();
            if (!string.IsNullOrWhiteSpace(name) &&
                Enum.TryParse<TCode>(name, ignoreCase: true, out var parsed) &&
                Enum.IsDefined(parsed))
                return parsed;
        }

        throw new JsonException($"Expected a valid {typeof(TCode).Name} name or numeric code.");
    }

    public override void Write(Utf8JsonWriter writer, TCode value, JsonSerializerOptions options) =>
        writer.WriteNumberValue(Convert.ToByte(value));
}
````

## `src/SmartFarm.Api/Controllers/SyncOperationsController.cs`

````text
using System.Security.Cryptography;
using System.Text.Json;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using System.Data;

namespace SmartFarm.Api.Controllers;

// A stable client operation ID makes retries safe even after a lost HTTP response.
public sealed class SyncWriteInput
{
    public Guid FarmId { get; set; }
    public Guid TargetId { get; set; }
    public string OperationType { get; set; } = "";
    public RecordActivityInput? Activity { get; set; }
    public RecordActivityWithStockInput? ActivityStock { get; set; }
    public RecordObservationInput? Observation { get; set; }
    public InventoryMovementInput? Movement { get; set; }
    public RecordHarvestInput? Harvest { get; set; }
}

public sealed record SyncWriteResult(Guid OperationId, string OperationType, JsonElement Result, bool Replayed);

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/sync/operations")]
public sealed class SyncOperationsController(
    UserManager<SmartFarmUser> users,
    SmartFarmDbContext db,
    IFarmAccessService access,
    IMonitoringService monitoring,
    IFieldworkStockService fieldwork,
    IInventoryService inventory,
    IHarvestService harvests) : FarmApiControllerBase(users)
{
    [HttpPost("{operationId:guid}")]
    public Task<IActionResult> Submit(Guid operationId, [FromBody] SyncWriteInput input,
        CancellationToken ct) =>
        Result(async () =>
        {
            if (operationId == Guid.Empty || input.FarmId == Guid.Empty || input.TargetId == Guid.Empty)
                return BadRequest(new { message = "Valid operation, farm and target IDs are required." });
            if (input.OperationType is not ("activity" or "activity_stock" or "observation" or "inventory" or "harvest"))
                return BadRequest(new { message = "Unsupported sync operation." });

            // Never reveal another farm's ledger or apply writes under another account.
            if (!await access.CanAsync(input.FarmId, ActorId, FarmAction.View, ct))
                return NotFound(new { message = "Resource not found." });

            var valid = input.OperationType switch
            {
                "activity" => input.Activity is not null && input.ActivityStock is null &&
                    input.Observation is null && input.Movement is null && input.Harvest is null,
                "activity_stock" => input.ActivityStock is not null && input.Activity is null &&
                    input.Observation is null && input.Movement is null && input.Harvest is null,
                "observation" => input.Observation is not null && input.Activity is null &&
                    input.ActivityStock is null && input.Movement is null && input.Harvest is null,
                "inventory" => input.Movement is not null && input.Activity is null &&
                    input.ActivityStock is null && input.Observation is null && input.Harvest is null,
                _ => input.Harvest is not null && input.Activity is null &&
                    input.ActivityStock is null && input.Observation is null && input.Movement is null
            };
            if (!valid)
                return BadRequest(new { message = "Provide exactly one matching operation payload." });

            var hash = SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(input));
            await using var transaction = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable, ct);

            // UPDLOCK + HOLDLOCK also locks the gap when this GUID is not present.
            // Concurrent requests with the same ID cannot both execute the business write.
            var previous = await db.SyncOperations
                .FromSqlInterpolated($"""
                    SELECT * FROM dbo.SyncOperation WITH (UPDLOCK, HOLDLOCK)
                    WHERE OperationId = {operationId}
                    """)
                .AsNoTracking().SingleOrDefaultAsync(ct);
            if (previous is not null)
            {
                if (previous.UserId != ActorId || previous.FarmId != input.FarmId ||
                    previous.OperationType != input.OperationType ||
                    !CryptographicOperations.FixedTimeEquals(previous.RequestHash, hash))
                    return Conflict(new { message = "Operation ID was already used with different data." });

                var saved = JsonSerializer.Deserialize<JsonElement>(previous.ResultJson!);
                return Ok(new SyncWriteResult(operationId, input.OperationType, saved, true));
            }

            object result = input.OperationType switch
            {
                "activity" => await monitoring.RecordActivityAsync(
                    input.FarmId, input.TargetId, ActorId, input.Activity!, ct),
                "activity_stock" => await fieldwork.RecordAsync(
                    input.FarmId, input.TargetId, ActorId, input.ActivityStock!, ct),
                "observation" => await monitoring.RecordObservationAsync(
                    input.FarmId, input.TargetId, ActorId, input.Observation!, ct),
                "inventory" => await inventory.PostAsync(
                    input.FarmId, input.TargetId, ActorId, input.Movement!, ct),
                _ => await harvests.RecordAsync(
                    input.FarmId, input.TargetId, ActorId, input.Harvest!, ct)
            };

            var resultJson = JsonSerializer.Serialize(result, new JsonSerializerOptions(JsonSerializerDefaults.Web));
            db.SyncOperations.Add(new SyncOperation
            {
                OperationId = operationId,
                FarmId = input.FarmId,
                UserId = ActorId,
                TargetEntityId = input.TargetId,
                OperationType = input.OperationType,
                RequestHash = hash,
                Status = 1,
                ResultJson = resultJson,
                ReceivedAtUtc = DateTime.UtcNow,
                CompletedAtUtc = DateTime.UtcNow
            });
            await db.SaveChangesAsync(ct);
            await transaction.CommitAsync(ct);

            return Ok(new SyncWriteResult(operationId, input.OperationType,
                JsonSerializer.Deserialize<JsonElement>(resultJson), false));
        });
}
````

## `src/SmartFarm.Worker/Program.cs`

````text
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using SmartFarm.Infrastructure;
using SmartFarm.Worker.Services;
using SmartFarm.Infrastructure.Integrations.Weather;
using SmartFarm.Worker;

var builder = Host.CreateApplicationBuilder(args);
builder.Services.AddSmartFarmWeather(builder.Configuration);
if (builder.Configuration.GetValue<bool>("Hangfire:Enabled"))
{
    var appDatabase = builder.Configuration.GetConnectionString("SmartFarm");
    if (string.IsNullOrWhiteSpace(appDatabase))
        throw new InvalidOperationException("Set ConnectionStrings:SmartFarm to run farm automation.");

    builder.Services.AddSmartFarmPersistence(appDatabase);
    builder.Services.AddScoped<IFarmAutomationService, FarmAutomationService>();
}
builder.Services.AddSmartFarmBackgroundProcessing(builder.Configuration);

await builder.Build().RunAsync();
````

## `src/SmartFarm.Worker/appsettings.json`

````text
{
  "Hangfire": {
    "Enabled": true,
    "WorkerCount": 1,
    "InitializeSchema": false,
    "RunJobsOnStartup": false
  },
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.Hosting.Lifetime": "Information",
      "Hangfire": "Information"
    }
  },
  "Weather": {
    "Provider": "OpenMeteo",
    "BaseAddress": "https://api.open-meteo.com/",
    "TimeoutSeconds": 15
  }
}
````

## `src/SmartFarm.Worker/BackgroundProcessingRegistration.cs`

````text
using Hangfire;
using Hangfire.SqlServer;
using Microsoft.Data.SqlClient;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Scheduling;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker;

public static class BackgroundProcessingRegistration
{
    public static IServiceCollection AddSmartFarmBackgroundProcessing(
        this IServiceCollection services, IConfiguration configuration)
    {
        var options = configuration.GetSection("Hangfire").Get<HangfireOptions>() ?? new();
        if (!options.Enabled)
        {
            return services;
        }

        if (options.WorkerCount is < 1 or > 64)
        {
            throw new InvalidOperationException("Hangfire:WorkerCount must be between 1 and 64.");
        }

        var connectionString = configuration.GetConnectionString("Hangfire");
        ValidateConnectionString(connectionString, configuration.GetConnectionString("SmartFarm"));

        services.AddHangfire(configuration => configuration
            .SetDataCompatibilityLevel(CompatibilityLevel.Version_180)
            .UseSimpleAssemblyNameTypeSerializer()
            .UseRecommendedSerializerSettings()
            .UseSqlServerStorage(connectionString!, CreateStorageOptions(options.InitializeSchema)));
        services.AddHangfireServer(server =>
        {
            server.WorkerCount = options.WorkerCount;
            server.Queues = ["default"];
        });
        services.AddScoped<IWorkerHealthService, WorkerHealthService>();
        services.AddScoped<WorkerHeartbeatJob>();
        services.AddScoped<TaskReminderJob>();
        services.AddScoped<WeatherCheckJob>();
        services.AddHostedService<WorkerJobScheduler>();

        return services;
    }

    internal static SqlServerStorageOptions CreateStorageOptions(bool initializeSchema = false) => new()
    {
        SqlClientFactory = SqlClientFactory.Instance,
        PrepareSchemaIfNecessary = initializeSchema,
        EnableHeavyMigrations = false,
        TryAutoDetectSchemaDependentOptions = false,
        CommandBatchMaxTimeout = TimeSpan.FromMinutes(5),
        SlidingInvisibilityTimeout = TimeSpan.FromMinutes(5),
        QueuePollInterval = TimeSpan.FromSeconds(15),
        UseRecommendedIsolationLevel = true
    };

    private static void ValidateConnectionString(string? connectionString, string? applicationConnectionString)
    {
        const string message = "Set ConnectionStrings:Hangfire to a separate, pre-provisioned jobs database " +
            "(recommended: SmartFarmJobsDB). Install its Hangfire schema separately, or explicitly set Hangfire:InitializeSchema=true for initial setup.";
        if (string.IsNullOrWhiteSpace(connectionString))
        {
            throw new InvalidOperationException(message);
        }

        SqlConnectionStringBuilder jobs;
        SqlConnectionStringBuilder? application;
        try
        {
            jobs = new(connectionString);
            application = string.IsNullOrWhiteSpace(applicationConnectionString)
                ? null : new(applicationConnectionString);
        }
        catch (ArgumentException)
        {
            throw new InvalidOperationException("Invalid SQL Server connection configuration. " + message);
        }

        var catalog = jobs.InitialCatalog.Trim();
        string[] reservedCatalogs = ["master", "model", "msdb", "tempdb", "SmartFarm", "SmartFarmDB"];
        if (string.IsNullOrWhiteSpace(jobs.DataSource) || string.IsNullOrWhiteSpace(catalog)
            || !string.IsNullOrWhiteSpace(jobs.AttachDBFilename)
            || reservedCatalogs.Contains(catalog, StringComparer.OrdinalIgnoreCase)
            || string.Equals(catalog, application?.InitialCatalog.Trim(), StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException(message);
        }
    }
}
````

## `src/SmartFarm.Worker/Scheduling/WorkerJobScheduler.cs`

````text
using Hangfire;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using SmartFarm.Worker.Jobs;

namespace SmartFarm.Worker.Scheduling;

public sealed class WorkerJobScheduler : IHostedService
{
    public const string HeartbeatJobId = "smartfarm-worker-check";
    public const string TaskJobId = "smartfarm-tasks-and-reminders";
    public const string WeatherJobId = "smartfarm-weather-check";

    private readonly IRecurringJobManager _recurringJobs;
    private readonly IBackgroundJobClient _jobs;
    private readonly bool _runJobsOnStartup;
    private readonly ILogger<WorkerJobScheduler> _logger;

    public WorkerJobScheduler(
        IRecurringJobManager recurringJobs,
        IBackgroundJobClient jobs,
        IConfiguration configuration,
        ILogger<WorkerJobScheduler> logger)
    {
        _recurringJobs = recurringJobs;
        _jobs = jobs;
        _runJobsOnStartup = configuration.GetValue<bool>("Hangfire:RunJobsOnStartup");
        _logger = logger;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();

        _recurringJobs.AddOrUpdate<WorkerHeartbeatJob>(
            HeartbeatJobId,
            job => job.ExecuteAsync(),
            Cron.Hourly(),
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _recurringJobs.AddOrUpdate<TaskReminderJob>(
            TaskJobId,
            job => job.ExecuteAsync(),
            Cron.Hourly(),
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _recurringJobs.AddOrUpdate<WeatherCheckJob>(
            WeatherJobId,
            job => job.ExecuteAsync(),
            "0 */6 * * *",
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        // Enqueue a lightweight check immediately so the server can prove it processes jobs,
        // without waiting for the first hourly recurring run.
        _jobs.Enqueue<WorkerHeartbeatJob>(job => job.ExecuteAsync());

        // Use only for initial/local verification: real farm jobs are normally cron-driven.
        if (_runJobsOnStartup)
        {
            _jobs.Enqueue<TaskReminderJob>(job => job.ExecuteAsync());
            _jobs.Enqueue<WeatherCheckJob>(job => job.ExecuteAsync());
        }

        _logger.LogInformation(
            "Scheduled heartbeat (hourly), tasks/reminders (hourly) and weather (every 6 hours). " +
            "Startup heartbeat queued. Immediate farm-job checks: {RunJobsOnStartup}.",
            _runJobsOnStartup);
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}
````

## `tests/SmartFarm.SchemaVerification/SchemaModelTests.cs`

````text
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.EntityFrameworkCore.Migrations;
using Microsoft.EntityFrameworkCore.Migrations.Operations;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.SchemaVerification;

// Offline checks: no connection string, database calls, or DDL execution.
public sealed class SchemaModelTests
{
    [Fact]
    public void EnrollmentPersistsUniqueCodesAndPendingRequestsWithDecisionIntegrity()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var model = context.GetService<IDesignTimeModel>().Model;
        var farm = model.FindEntityType(typeof(Farm))!;
        var code = farm.FindProperty("JoinCode");
        Assert.NotNull(code);
        Assert.False(code.IsNullable);
        Assert.Equal(12, code.GetMaxLength());
        Assert.Contains(farm.GetIndexes(), index => index.IsUnique && index.Properties.Count == 1 && index.Properties[0].Name == "JoinCode");
        var request = model.GetEntityTypes().Single(entity => entity.GetTableName() == "FarmJoinRequest");
        Assert.Equal(7, request.GetProperties().Count());
        Assert.Equal(3, request.GetForeignKeys().Count());
        Assert.All(request.GetForeignKeys(), key => Assert.Equal(DeleteBehavior.NoAction, key.DeleteBehavior));
        var pending = request.GetIndexes().Single(index => index.IsUnique);
        Assert.Equal(new[] { "FarmId", "UserId" }, pending.Properties.Select(property => property.Name));
        Assert.Equal("Status = N'Pending'", pending.GetFilter());
        Assert.Contains(request.GetCheckConstraints(), constraint => constraint.Name == "CK_FarmJoinRequest_Status");
        Assert.Contains(request.GetCheckConstraints(), constraint => constraint.Name == "CK_FarmJoinRequest_Review");
        Assert.Contains(request.GetCheckConstraints(), constraint => constraint.Name == "CK_FarmJoinRequest_Status" && constraint.Sql.Contains("Canceled"));
        Assert.Contains(model.FindEntityType(typeof(Notification))!.GetCheckConstraints(), constraint => constraint.Name == "CK_Notification_Type" && constraint.Sql.Contains("4"));
        var codes = Enumerable.Range(0, 100).Select(_ => new Farm().JoinCode).ToArray();
        Assert.All(codes, value => Assert.Matches("^[0-9A-F]{12}$", value));
        Assert.Equal(codes.Length, codes.Distinct().Count());
    }

    [Fact]
    public void SqlFirstModelContainsOnlyExpectedTablesColumnsAndConstraints()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entities = context.GetService<IDesignTimeModel>().Model.GetEntityTypes().ToArray();
        Assert.Equal(25, entities.Length); // Includes seven Identity tables and optional finance ledger.
        Assert.Equal(219, entities.Sum(entity => entity.GetProperties().Count())); // Includes 13 finance ledger columns.
        Assert.Equal(47, entities.Sum(entity => entity.GetForeignKeys().Count()));
        Assert.Equal(52, entities.Sum(entity => entity.GetIndexes().Count()));
        Assert.Equal(42, entities.Sum(entity => entity.GetCheckConstraints().Count()));
        Assert.Equal(34, entities.SelectMany(entity => entity.GetProperties()).Count(property =>
            property.GetDefaultValueSql() is not null || property.TryGetDefaultValue(out _)));
        Assert.All(entities, entity => Assert.Equal("dbo", entity.GetSchema()));
    }

    [Fact]
    public void GuidKeysAreClientAssignedAndSqlRowVersionsProvideConcurrency()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entities = context.Model.GetEntityTypes().ToArray();
        var guidKeys = entities.SelectMany(entity => entity.FindPrimaryKey()!.Properties)
            .Where(property => property.ClrType == typeof(Guid));
        Assert.All(guidKeys, property => Assert.Equal(ValueGenerated.Never, property.ValueGenerated));
        var rowVersions = entities.SelectMany(entity => entity.GetProperties())
            .Where(property => property.Name == "RowVersion").ToArray();
        Assert.Equal(6, rowVersions.Length);
        Assert.All(rowVersions, property =>
        {
            Assert.True(property.IsConcurrencyToken);
            Assert.Equal(ValueGenerated.OnAddOrUpdate, property.ValueGenerated);
        });
        Assert.False(context.Model.FindEntityType(typeof(InventoryItem))!
            .FindProperty(nameof(InventoryItem.MetadataVersion))!.IsConcurrencyToken);
        Assert.NotEqual(Guid.Empty, new Farm().Id);
        Assert.NotEqual(Guid.Empty, new SyncOperation().OperationId);
    }

    [Fact]
    public void ExplicitFalseCropTypeValueDoesNotUseTrueDatabaseDefault()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var inactiveCrop = new CropType { IsActive = false };
        context.Add(inactiveCrop);
        var entry = context.Entry(inactiveCrop).Property(crop => crop.IsActive);
        Assert.False(entry.IsTemporary);
        Assert.Equal(true, entry.Metadata.Sentinel);
        Assert.NotEqual(entry.CurrentValue, entry.Metadata.Sentinel);
        Assert.True(new CropType().IsActive);
        Assert.Equal("Asia/Manila", new Farm().TimeZoneId);
    }

    [Fact]
    public void AdoptionBaselineContainsNoDdlAndSnapshotHasNoDrift()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        Assert.False(context.Database.HasPendingModelChanges());
        var assembly = context.GetService<IMigrationsAssembly>();
        Assert.Equal(4, assembly.Migrations.Count);
        var migrationType = assembly.Migrations.Single(entry => entry.Key.EndsWith("_ExistingSchemaBaseline", StringComparison.Ordinal)).Value;
        var migration = assembly.CreateMigration(migrationType, context.Database.ProviderName!);
        Assert.Empty(migration.UpOperations);
        Assert.Empty(migration.DownOperations);
        var enrollmentType = assembly.Migrations.Single(entry => entry.Key.EndsWith("_FarmerRegistrationApproval", StringComparison.Ordinal)).Value;
        var enrollment = assembly.CreateMigration(enrollmentType, context.Database.ProviderName!);
        var upgrade = Assert.IsType<SqlOperation>(Assert.Single(enrollment.UpOperations));
        var script = File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "Database", "04_FarmerRegistrationApproval.sql"));
        Assert.Equal(script.Trim(), upgrade.Sql.Trim());
        Assert.Throws<NotSupportedException>(() => enrollment.DownOperations);
        var workflowType = assembly.Migrations.Single(entry => entry.Key.EndsWith("_FarmEnrollmentWorkflow", StringComparison.Ordinal)).Value;
        var workflow = assembly.CreateMigration(workflowType, context.Database.ProviderName!);
        var workflowSql = Assert.IsType<SqlOperation>(Assert.Single(workflow.UpOperations));
        Assert.Equal(File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "Database", "05_FarmEnrollmentWorkflow.sql")).Trim(), workflowSql.Sql.Trim());
        Assert.Throws<NotSupportedException>(() => workflow.DownOperations);

    }
}
````

## `database/03_VerifySchema.sql`

````text
/* Read-only SmartFarmDB verification. Optional EF baseline history is ignored. */
SET NOCOUNT ON;

IF DB_NAME() <> N'SmartFarmDB'
    THROW 50010, 'Select SmartFarmDB before checking its schema.', 1;

-- Check the base schema and the optional Phase 3 finance extension without editing data.
DECLARE @financeTable int = CASE WHEN OBJECT_ID(N'dbo.FarmFinanceEntry', N'U') IS NULL THEN 0 ELSE 1 END;
IF EXISTS (
    SELECT 1 FROM sys.tables
    WHERE is_ms_shipped = 0 AND schema_id <> SCHEMA_ID(N'dbo')
)
    THROW 50011, 'Unexpected user tables outside dbo.', 1;

IF (SELECT COUNT(*) FROM sys.tables
    WHERE schema_id = SCHEMA_ID(N'dbo') AND name <> N'__EFMigrationsHistory') <> 24 + @financeTable
    THROW 50012, 'Unexpected SmartFarm table count.', 1;

IF (SELECT COUNT(*) FROM sys.indexes AS i
    JOIN sys.tables AS t ON t.object_id = i.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND i.is_primary_key = 0 AND i.name IS NOT NULL) <> 49 + (3 * @financeTable)
    THROW 50013, 'Expected 49 application indexes.', 1;

IF (SELECT COUNT(*) FROM sys.foreign_keys AS fk
    JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 43 + (4 * @financeTable)
    THROW 50014, 'Expected 43 foreign keys.', 1;

IF (SELECT COUNT(*) FROM sys.check_constraints AS ck
    JOIN sys.tables AS t ON t.object_id = ck.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 39 + (3 * @financeTable)
    THROW 50015, 'Expected 39 check constraints.', 1;

IF (SELECT COUNT(*) FROM sys.columns AS c
    JOIN sys.tables AS t ON t.object_id = c.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND c.system_type_id = TYPE_ID(N'timestamp')) <> 6
    THROW 50016, 'Expected six SQL rowversion columns.', 1;

-- Disabled or untrusted constraints can look present while not protecting data.
IF EXISTS (
    SELECT 1 FROM sys.foreign_keys AS fk
    JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND (fk.is_disabled = 1 OR fk.is_not_trusted = 1)
)
    THROW 50018, 'Foreign key checks are disabled or untrusted.', 1;

IF EXISTS (
    SELECT 1 FROM sys.check_constraints AS ck
    JOIN sys.tables AS t ON t.object_id = ck.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND (ck.is_disabled = 1 OR ck.is_not_trusted = 1)
)
    THROW 50019, 'Check constraints are disabled or untrusted.', 1;

IF EXISTS (
    SELECT 1 FROM sys.indexes AS i
    JOIN sys.tables AS t ON t.object_id = i.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND i.name IS NOT NULL AND i.is_disabled = 1
)
    THROW 50020, 'An application index is disabled.', 1;

-- These unique indexes protect important business rules and retry safety.
IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'Farm', N'UX_Farm_JoinCode', 0),
        (N'FarmJoinRequest', N'UX_FarmJoinRequest_Pending', 1),
        (N'FarmMember', N'UX_FarmMember_Active', 1),
        (N'CropCycle', N'UX_CropCycle_OneActivePerPlot', 1),
        (N'FarmTask', N'UX_FarmTask_Dedupe', 1),
        (N'InventoryTransaction', N'UX_InventoryTransaction_Reverses', 1),
        (N'Notification', N'UX_Notification_User_Dedupe', 0),
        (N'WeatherAdvisory', N'UX_WeatherAdvisory_Daily', 0)
    ) AS expected(TableName, IndexName, MustBeFiltered)
    LEFT JOIN sys.tables AS t ON t.schema_id = SCHEMA_ID(N'dbo')
        AND t.name = expected.TableName
    LEFT JOIN sys.indexes AS i ON i.object_id = t.object_id
        AND i.name = expected.IndexName
    WHERE i.index_id IS NULL OR i.is_unique <> 1
        OR (expected.MustBeFiltered = 1 AND i.has_filter <> 1)
)
    THROW 50017, 'A required unique business index is missing or incorrectly configured.', 1;

SELECT N'PASS' AS SchemaCheck, DB_NAME() AS ConnectedDatabase,
       24 + @financeTable AS AppAndIdentityTables,
       49 + 3 * @financeTable AS AppIndexes,
       43 + 4 * @financeTable AS ForeignKeys,
       39 + 3 * @financeTable AS CheckConstraints,
       6 AS RowVersionColumns;

SELECT name AS TableName
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo')
ORDER BY name;

SELECT fk.name AS ForeignKeyName,
       OBJECT_NAME(fk.parent_object_id) AS ChildTable,
       OBJECT_NAME(fk.referenced_object_id) AS ParentTable
FROM sys.foreign_keys AS fk
WHERE OBJECT_SCHEMA_NAME(fk.parent_object_id) = N'dbo'
ORDER BY ChildTable, ForeignKeyName;
````
