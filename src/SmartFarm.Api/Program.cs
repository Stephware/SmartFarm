using Microsoft.AspNetCore.DataProtection;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Security;
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
builder.Services.AddSingleton<IObservationPhotoStore>(_ =>
    new LocalObservationPhotoStore(
        builder.Configuration["ObservationPhotos:StoragePath"] ??
        (builder.Environment.IsDevelopment()
            ? Path.GetFullPath(Path.Combine(builder.Environment.ContentRootPath, "..", "SmartFarm.Web",
                "App_Data", "observation-photos"))
            : Path.Combine(builder.Environment.ContentRootPath, "App_Data", "observation-photos"))));
builder.Services.AddDataProtection().SetApplicationName($"SmartFarm.SharedIdentity.{builder.Environment.EnvironmentName}");

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

app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

if (app.Environment.IsDevelopment())
{
    app.MapGet("/health/database", async (SmartFarmDbContext db, CancellationToken ct) =>
    {
        try
        {
            if (string.IsNullOrWhiteSpace(db.Database.GetConnectionString()))
                return Results.StatusCode(StatusCodes.Status503ServiceUnavailable);

            return await db.Database.CanConnectAsync(ct)
                ? Results.Ok(new { status = "ok" })
                : Results.StatusCode(StatusCodes.Status503ServiceUnavailable);
        }
        catch (Exception)
        {
            return Results.StatusCode(StatusCodes.Status503ServiceUnavailable);
        }
    });
}

app.MapControllers();
app.Run();

public partial class Program { }
