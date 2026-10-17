using Microsoft.Extensions.Hosting;
using SmartFarm.Infrastructure.Integrations.Weather;
using SmartFarm.Worker;

var builder = Host.CreateApplicationBuilder(args);
builder.Services.AddSmartFarmWeather(builder.Configuration);
builder.Services.AddSmartFarmBackgroundProcessing(builder.Configuration);

await builder.Build().RunAsync();
