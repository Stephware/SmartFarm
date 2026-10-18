using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Infrastructure;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Integrations.Weather;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Services.Crops;
using SmartFarm.Infrastructure.Services.Monitoring;
using SmartFarm.Infrastructure.Services.Farms;
using SmartFarm.Infrastructure.Services.Inventory;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.HealthChecks;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews();
builder.Services.AddSmartFarmPersistence(
    builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmWeather(builder.Configuration);
builder.Services.AddSmartFarmIdentity(builder.Environment);
builder.Services.AddScoped<AccountRegistrationService>();
builder.Services.AddScoped<IFarmAccessService, FarmAccessService>();
builder.Services.AddScoped<IFarmManagementService, FarmManagementService>();
builder.Services.AddScoped<ICropManagementService, CropManagementService>();
builder.Services.AddScoped<IMonitoringService, MonitoringService>();
builder.Services.AddScoped<IInventoryService, InventoryService>();
builder.Services.AddSingleton<IObservationPhotoStore>(_ =>
    new LocalObservationPhotoStore(
        builder.Configuration["ObservationPhotos:StoragePath"] ??
        Path.Combine(builder.Environment.ContentRootPath, "App_Data", "observation-photos")));
builder.Services.AddDataProtection();

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




