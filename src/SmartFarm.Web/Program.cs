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
using SmartFarm.Web.Clients;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews();
builder.Services.AddSmartFarmPersistence(
    builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmWebIdentity(builder.Environment);
builder.Services.AddScoped<AccountRegistrationService>();

// The MVC pages use the same API as Android, not Infrastructure services.
builder.Services.AddHttpContextAccessor();
builder.Services.AddHttpClient<SmartFarmApiClient>(client =>
{
    var configured = builder.Configuration["SmartFarmApi:BaseUrl"];
    if (string.IsNullOrWhiteSpace(configured) && !builder.Environment.IsDevelopment())
        throw new InvalidOperationException("Configure SmartFarmApi:BaseUrl to use the MVC site.");

    var address = configured ?? "http://localhost:5081/";
    if (!Uri.TryCreate(address, UriKind.Absolute, out var uri) ||
        (uri.Scheme != Uri.UriSchemeHttps &&
            (!builder.Environment.IsDevelopment() || uri.Scheme != Uri.UriSchemeHttp)))
        throw new InvalidOperationException("SmartFarmApi:BaseUrl must be an absolute HTTPS URL outside Development.");

    client.BaseAddress = uri;
    client.Timeout = TimeSpan.FromSeconds(20);
}).ConfigurePrimaryHttpMessageHandler(() => new HttpClientHandler
{
    AllowAutoRedirect = false,
    UseCookies = false
});
builder.Services.AddScoped<IFarmManagementService, FarmApiService>();
builder.Services.AddScoped<ICropManagementService, CropApiService>();
builder.Services.AddScoped<IMonitoringService, MonitoringApiService>();
builder.Services.AddScoped<IInventoryService, InventoryApiService>();
builder.Services.AddScoped<IHarvestService, HarvestApiService>();
builder.Services.AddScoped<IFarmReportService, FarmReportApiService>();
builder.Services.AddScoped<IFarmTaskService, FarmTaskApiService>();
builder.Services.AddScoped<IFarmAccessService, FarmAccessApiService>();
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
