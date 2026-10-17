using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using SmartFarm.Infrastructure;
using SmartFarm.Infrastructure.Integrations.Weather;
using SmartFarm.Web.HealthChecks;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews();
builder.Services.AddSmartFarmPersistence(
    builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmWeather(builder.Configuration);

if (builder.Environment.IsDevelopment())
{
    builder.Services.AddHealthChecks()
        .AddCheck<DatabaseConnectivityHealthCheck>("database", timeout: TimeSpan.FromSeconds(5));
}

var app = builder.Build();

app.UseStaticFiles();

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
