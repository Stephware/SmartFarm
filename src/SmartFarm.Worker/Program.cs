using Microsoft.Extensions.Hosting;
using SmartFarm.Worker;

var builder = Host.CreateApplicationBuilder(args);
builder.Services.AddSmartFarmBackgroundProcessing(builder.Configuration);

await builder.Build().RunAsync();
