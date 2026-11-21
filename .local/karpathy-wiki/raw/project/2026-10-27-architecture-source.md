# SmartFarm architecture source snapshot

> Source: Local SmartFarm repository at 850bfc7dab94675d5a70c13a95a9d00c481c1784
> Collected: 2026-10-27
> Published: Unknown

Read-only capture of repository file contents; not proof of runtime behavior.


## Repository file: SmartFarm.slnx

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


## Repository file: Directory.Build.props

<Project>
  <PropertyGroup>
    <TargetFramework>net10.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>
</Project>


## Repository file: global.json

{
  "sdk": {
    "version": "10.0.100",
    "rollForward": "latestFeature"
  }
}


## Repository file: .config/dotnet-tools.json

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


## Repository file: src/SmartFarm.Api/SmartFarm.Api.csproj

<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <UserSecretsId>SmartFarm.Api.Development</UserSecretsId>
  </PropertyGroup>
  <ItemGroup>
    <ProjectReference Include="../SmartFarm.Core/SmartFarm.Core.csproj" />
    <ProjectReference Include="../SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj" />
  </ItemGroup>
</Project>


## Repository file: src/SmartFarm.Core/SmartFarm.Core.csproj

<Project Sdk="Microsoft.NET.Sdk" />


## Repository file: src/SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj

<Project Sdk="Microsoft.NET.Sdk">
  <ItemGroup>
    <FrameworkReference Include="Microsoft.AspNetCore.App" />
    <PackageReference Include="Microsoft.AspNetCore.Identity.EntityFrameworkCore" Version="10.0.13" />
    <PackageReference Include="Microsoft.EntityFrameworkCore.Design" Version="10.0.13" PrivateAssets="all" />
    <PackageReference Include="Microsoft.EntityFrameworkCore.SqlServer" Version="10.0.13" />

    <PackageReference Include="System.Security.Cryptography.Pkcs" Version="10.0.13" />
  </ItemGroup>
  <ItemGroup>
    <ProjectReference Include="../SmartFarm.Core/SmartFarm.Core.csproj" />
  </ItemGroup>
</Project>



## Repository file: src/SmartFarm.Web/SmartFarm.Web.csproj

<Project Sdk="Microsoft.NET.Sdk.Web">
  <PropertyGroup>
    <UserSecretsId>SmartFarm.Web.Development</UserSecretsId>
  </PropertyGroup>
  <ItemGroup>
    <ProjectReference Include="../SmartFarm.Core/SmartFarm.Core.csproj" />
    <ProjectReference Include="../SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj" />
  </ItemGroup>
</Project>


## Repository file: src/SmartFarm.Worker/SmartFarm.Worker.csproj

<Project Sdk="Microsoft.NET.Sdk.Worker">
  <PropertyGroup>
    <UserSecretsId>SmartFarm.Worker.Development</UserSecretsId>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Hangfire.NetCore" Version="1.8.25" />
    <PackageReference Include="Hangfire.SqlServer" Version="1.8.25" />
    <PackageReference Include="Microsoft.Extensions.Hosting" Version="10.0.13" />
    <!-- Patch Hangfire's vulnerable transitive Newtonsoft.Json version. -->
    <PackageReference Include="Newtonsoft.Json" Version="13.0.4" />
  </ItemGroup>
  <ItemGroup>
    <InternalsVisibleTo Include="SmartFarm.Worker.Tests" />
  </ItemGroup>
  <ItemGroup>
    <ProjectReference Include="../SmartFarm.Core/SmartFarm.Core.csproj" />
    <ProjectReference Include="../SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj" />
  </ItemGroup>
</Project>


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


## Repository file: src/SmartFarm.Web/Program.cs

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

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews(options => options.Filters.AddService<SmartFarm.Web.Filters.ApiUnavailableFilter>());
builder.Services.AddScoped<SmartFarm.Web.Filters.ApiUnavailableFilter>();
builder.Services.AddSmartFarmPersistence(
    builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmWebIdentity(builder.Environment);
builder.Services.AddScoped<AccountRegistrationService>();

// MVC and API run independently using the same Infrastructure business services.
builder.Services.AddSmartFarmBusinessServices();
// In Development this matches the API's sibling SmartFarm.Web/App_Data photo store.
// Configure ObservationPhotos:StoragePath to the same shared directory on each host
// before deploying the MVC and API to separate instances.
builder.Services.AddSingleton<IObservationPhotoStore>(_ =>
    new LocalObservationPhotoStore(
        builder.Configuration["ObservationPhotos:StoragePath"] ??
        Path.GetFullPath(Path.Combine(builder.Environment.ContentRootPath,
            "App_Data", "observation-photos"))));
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


## Repository file: src/SmartFarm.Worker/Program.cs

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


## Repository file: src/SmartFarm.Api/Properties/launchSettings.json

{
  "$schema": "https://json.schemastore.org/launchsettings.json",
  "profiles": {
    "http": {
      "commandName": "Project",
      "dotnetRunMessages": true,
      "launchBrowser": false,
      "applicationUrl": "http://localhost:5081",
      "environmentVariables": {
        "ASPNETCORE_ENVIRONMENT": "Development"
      }
    }
  }
}


## Repository file: src/SmartFarm.Web/Properties/launchSettings.json

{
  "$schema": "https://json.schemastore.org/launchsettings.json",
  "profiles": {
    "http": {
      "commandName": "Project",
      "dotnetRunMessages": true,
      "launchBrowser": false,
      "applicationUrl": "http://localhost:5080",
      "environmentVariables": {
        "ASPNETCORE_ENVIRONMENT": "Development"
      }
    }
  }
}


## Repository file: src/SmartFarm.Infrastructure/DependencyInjection.cs

using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Interfaces.Crops;
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
        services.AddScoped<IInventoryService, InventoryService>();
        services.AddScoped<IHarvestService, HarvestService>();
        services.AddScoped<IFarmReportService, FarmReportService>();
        services.AddScoped<IFarmTaskService, FarmTaskService>();
        services.AddScoped<IFarmWeatherService, FarmWeatherService>();
        return services;
    }
}


## Repository file: src/SmartFarm.Infrastructure/Identity/IdentitySetup.cs

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


## Repository file: src/SmartFarm.Infrastructure/Identity/AccountRegistrationService.cs

using System.Data;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Persistence;

namespace SmartFarm.Infrastructure.Identity;

// Single registration path shared by MVC and Android API: never grant farm permissions on signup.
public sealed class AccountRegistrationService(UserManager<SmartFarmUser> users,
    IFarmEnrollmentService enrollment, IUnitOfWork unit)
{
    public async Task<IdentityResult> RegisterAsync(string name, string email, string password,
        bool joinFarm = false, string? farmCode = null)
    {
        var cleanName = (name ?? "").Trim();
        var cleanEmail = (email ?? "").Trim();
        if (cleanName.Length is < 2 or > 120 || cleanEmail.Length is < 3 or > 256)
            return IdentityResult.Failed(new IdentityError { Description = "Invalid name or email length." });

        var user = new SmartFarmUser
        {
            FullName = cleanName,
            UserName = cleanEmail,
            Email = cleanEmail,
            LockoutEnabled = true
        };
        if (!joinFarm) return await users.CreateAsync(user, password);

        // Identity uniqueness checks must release read locks before parallel inserts; enrollment locks the farm explicitly.
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.ReadCommitted);
        try
        {
            var result = await users.CreateAsync(user, password);
            if (!result.Succeeded) return result;
            await enrollment.RequestAsync(user.Id, farmCode ?? "");
            await transaction.CommitAsync();
            return result;
        }
        catch (FarmOperationException error)
        {
            return IdentityResult.Failed(new IdentityError { Code = "FarmCode", Description = error.Message });
        }
        catch (DbUpdateException)
        {
            return IdentityResult.Failed(new IdentityError { Code = "Registration", Description = "Registration could not be saved. Please try again." });
        }
    }
}


## Repository file: src/SmartFarm.Infrastructure/Security/FarmAccessService.cs

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



## Repository file: src/SmartFarm.Core/Services/Security/FarmAccessRules.cs

using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Security;

public static class FarmAccessRules
{
    public static bool Allows(FarmAccessLevel level, FarmAction action) => action switch
    {
        FarmAction.View => level is FarmAccessLevel.Owner or FarmAccessLevel.Manager or FarmAccessLevel.FieldWorker,
        FarmAction.Manage => level is FarmAccessLevel.Owner or FarmAccessLevel.Manager,
        FarmAction.ManageMembers => level == FarmAccessLevel.Owner,
        _ => false
    };
}



## Repository file: tests/SmartFarm.Auth.Tests/MvcIndependentHostTests.cs

using System.Net;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class MvcIndependentHostTests
{
    [Fact]
    public async Task PublicMvcHomepageWorksWithoutAnApiEndpoint()
    {
        // The MVC host must not depend on a running/configured Web API.
        using var app = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            builder.ConfigureAppConfiguration((_, config) =>
                config.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["SmartFarmApi:BaseUrl"] = "not-a-valid-api-address"
                })));

        using var client = app.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });
        using var response = await client.GetAsync("/");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }
}
