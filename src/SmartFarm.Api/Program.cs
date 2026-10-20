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

builder.Services.AddControllers();
builder.Services.AddSmartFarmPersistence(builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmRepositories();
builder.Services.AddSmartFarmWeather(builder.Configuration);
builder.Services.AddSmartFarmApiIdentity();
builder.Services.AddScoped<AccountRegistrationService>();
builder.Services.AddScoped<IFarmAccessService, FarmAccessService>();
builder.Services.AddScoped<IFarmManagementService, FarmManagementService>();
builder.Services.AddScoped<ICropManagementService, CropManagementService>();
builder.Services.AddScoped<IMonitoringService, MonitoringService>();
builder.Services.AddScoped<IInventoryService, InventoryService>();
builder.Services.AddScoped<IHarvestService, HarvestService>();
builder.Services.AddScoped<IFarmReportService, FarmReportService>();
builder.Services.AddScoped<IFarmTaskService, FarmTaskService>();
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
