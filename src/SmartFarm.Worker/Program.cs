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
