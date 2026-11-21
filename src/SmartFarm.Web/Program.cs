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
using SmartFarm.Web.Authentication;
using SmartFarm.Infrastructure.Services.Admin;
using SmartFarm.Web.Services.Admin;
using Microsoft.AspNetCore.Identity;

var builder = WebApplication.CreateBuilder(args);

// Production containers listen on loopback; only the local Caddy proxy is trusted.
builder.Services.Configure<Microsoft.AspNetCore.Builder.ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = Microsoft.AspNetCore.HttpOverrides.ForwardedHeaders.XForwardedFor
        | Microsoft.AspNetCore.HttpOverrides.ForwardedHeaders.XForwardedProto;
    options.KnownIPNetworks.Clear();
    options.KnownProxies.Clear();
    options.KnownProxies.Add(System.Net.IPAddress.Loopback);
    options.KnownProxies.Add(System.Net.IPAddress.IPv6Loopback);
});

builder.Services.AddLocalization(options => options.ResourcesPath = "Resources");
builder.Services.AddControllersWithViews(options => options.Filters.AddService<SmartFarm.Web.Filters.ApiUnavailableFilter>())
    .AddViewLocalization().AddDataAnnotationsLocalization(options =>
        options.DataAnnotationLocalizerProvider = (_, factory) => factory.Create(typeof(SmartFarm.Web.SharedResource)));
builder.Services.Configure<Microsoft.AspNetCore.Builder.RequestLocalizationOptions>(options => {
    options.SetDefaultCulture("en").AddSupportedCultures("en", "fil").AddSupportedUICultures("en", "fil");
    // Explicit browser choice wins; URL parameters must not silently change saved language.
    options.RequestCultureProviders = [new Microsoft.AspNetCore.Localization.CookieRequestCultureProvider()];
});
builder.Services.PostConfigure<Microsoft.AspNetCore.Mvc.MvcOptions>(options => {
    options.ModelMetadataDetailsProviders.Add(new SmartFarm.Web.Presentation.LocalizedValidationMetadata());
    options.ModelBindingMessageProvider.SetValueMustBeANumberAccessor(_ => SmartFarm.Web.Presentation.UiMessages.Text("Enter a valid number."));
    options.ModelBindingMessageProvider.SetAttemptedValueIsInvalidAccessor((_, _) => SmartFarm.Web.Presentation.UiMessages.Text("Check the entered value."));
    options.ModelBindingMessageProvider.SetNonPropertyAttemptedValueIsInvalidAccessor(_ => SmartFarm.Web.Presentation.UiMessages.Text("Check the entered value."));
    options.ModelBindingMessageProvider.SetValueMustNotBeNullAccessor(_ => SmartFarm.Web.Presentation.UiMessages.Text("This field is required."));
});
builder.Services.AddScoped<SmartFarm.Web.Filters.ApiUnavailableFilter>();
builder.Services.AddEnrollmentRateLimiting();
builder.Services.AddAccountRecovery(builder.Configuration);
SmartFarm.Infrastructure.Integrations.Gemini.GeminiRegistration.AddWeatherGuidance(builder.Services, builder.Configuration);
builder.Services.AddSmartFarmPersistence(
    builder.Configuration.GetConnectionString("SmartFarm"));
builder.Services.AddSmartFarmWebIdentity(builder.Environment);
builder.Services.AddSuperAdminSecurity();
builder.Services.AddScoped<AdminAuditWriter>();
builder.Services.AddHttpClient<AdminDiagnosticsService>(client =>
{
    // Diagnostic probes are bounded; a down API must not hang the admin portal.
    client.Timeout = TimeSpan.FromSeconds(5);
}).ConfigurePrimaryHttpMessageHandler(() => new HttpClientHandler
{
    AllowAutoRedirect = false
});
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

app.UseForwardedHeaders();

// Explicit one-time local operator action. Never seed admins from public registration.
if (args.Length > 0 && args[0] == "--provision-super-admin")
{
    await SuperAdminProvisioner.RunAsync(app.Services, args);
    return;
}

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
app.UseRequestLocalization();
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
// Admin controllers use explicit attribute routes; do not let area routes
// generate /admin/* URLs for ordinary farm controllers.
app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();

public partial class Program { }
