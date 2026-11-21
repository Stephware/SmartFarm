# SmartFarm automation source snapshot

> Source: Local SmartFarm repository at 850bfc7dab94675d5a70c13a95a9d00c481c1784
> Collected: 2026-10-27
> Published: Unknown

Read-only capture of repository file contents; not proof of runtime behavior.


## Repository file: src/SmartFarm.Worker/BackgroundProcessingRegistration.cs

using Hangfire;
using Hangfire.SqlServer;
using Microsoft.Data.SqlClient;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Scheduling;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker;

public static class BackgroundProcessingRegistration
{
    public static IServiceCollection AddSmartFarmBackgroundProcessing(
        this IServiceCollection services, IConfiguration configuration)
    {
        var options = configuration.GetSection("Hangfire").Get<HangfireOptions>() ?? new();
        if (!options.Enabled)
        {
            return services;
        }

        if (options.WorkerCount is < 1 or > 64)
        {
            throw new InvalidOperationException("Hangfire:WorkerCount must be between 1 and 64.");
        }

        var connectionString = configuration.GetConnectionString("Hangfire");
        ValidateConnectionString(connectionString, configuration.GetConnectionString("SmartFarm"));

        services.AddHangfire(configuration => configuration
            .SetDataCompatibilityLevel(CompatibilityLevel.Version_180)
            .UseSimpleAssemblyNameTypeSerializer()
            .UseRecommendedSerializerSettings()
            .UseSqlServerStorage(connectionString!, CreateStorageOptions(options.InitializeSchema)));
        services.AddHangfireServer(server =>
        {
            server.WorkerCount = options.WorkerCount;
            server.Queues = ["default"];
        });
        services.AddScoped<IWorkerHealthService, WorkerHealthService>();
        services.AddScoped<WorkerHeartbeatJob>();
        services.AddScoped<TaskReminderJob>();
        services.AddScoped<WeatherCheckJob>();
        services.AddHostedService<WorkerJobScheduler>();

        return services;
    }

    internal static SqlServerStorageOptions CreateStorageOptions(bool initializeSchema = false) => new()
    {
        SqlClientFactory = SqlClientFactory.Instance,
        PrepareSchemaIfNecessary = initializeSchema,
        EnableHeavyMigrations = false,
        TryAutoDetectSchemaDependentOptions = false,
        CommandBatchMaxTimeout = TimeSpan.FromMinutes(5),
        SlidingInvisibilityTimeout = TimeSpan.FromMinutes(5),
        QueuePollInterval = TimeSpan.FromSeconds(15),
        UseRecommendedIsolationLevel = true
    };

    private static void ValidateConnectionString(string? connectionString, string? applicationConnectionString)
    {
        const string message = "Set ConnectionStrings:Hangfire to a separate, pre-provisioned jobs database " +
            "(recommended: SmartFarmJobsDB). Install its Hangfire schema separately, or explicitly set Hangfire:InitializeSchema=true for initial setup.";
        if (string.IsNullOrWhiteSpace(connectionString))
        {
            throw new InvalidOperationException(message);
        }

        SqlConnectionStringBuilder jobs;
        SqlConnectionStringBuilder? application;
        try
        {
            jobs = new(connectionString);
            application = string.IsNullOrWhiteSpace(applicationConnectionString)
                ? null : new(applicationConnectionString);
        }
        catch (ArgumentException)
        {
            throw new InvalidOperationException("Invalid SQL Server connection configuration. " + message);
        }

        var catalog = jobs.InitialCatalog.Trim();
        string[] reservedCatalogs = ["master", "model", "msdb", "tempdb", "SmartFarm", "SmartFarmDB"];
        if (string.IsNullOrWhiteSpace(jobs.DataSource) || string.IsNullOrWhiteSpace(catalog)
            || !string.IsNullOrWhiteSpace(jobs.AttachDBFilename)
            || reservedCatalogs.Contains(catalog, StringComparer.OrdinalIgnoreCase)
            || string.Equals(catalog, application?.InitialCatalog.Trim(), StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException(message);
        }
    }
}


## Repository file: src/SmartFarm.Worker/HangfireOptions.cs

namespace SmartFarm.Worker;

public sealed class HangfireOptions
{
    public bool Enabled { get; set; }
    public int WorkerCount { get; set; } = 1;
    public bool InitializeSchema { get; set; } = false;
}


## Repository file: src/SmartFarm.Worker/Jobs/TaskReminderJob.cs

using Hangfire;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

public sealed class TaskReminderJob(IFarmAutomationService service)
{
    [AutomaticRetry(Attempts = 3)]
    public Task ExecuteAsync() => service.GenerateTasksAndRemindersAsync();
}


## Repository file: src/SmartFarm.Worker/Jobs/WeatherCheckJob.cs

using Hangfire;
using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

public sealed class WeatherCheckJob(IFarmAutomationService service)
{
    [AutomaticRetry(Attempts = 3)]
    [DisableConcurrentExecution(timeoutInSeconds: 600)]
    public Task ExecuteAsync() => service.CheckWeatherAsync();
}


## Repository file: src/SmartFarm.Worker/Jobs/WorkerHeartbeatJob.cs

using SmartFarm.Worker.Services;

namespace SmartFarm.Worker.Jobs;

// Keep jobs small: the service contains the actual operation.
public sealed class WorkerHeartbeatJob
{
    private readonly IWorkerHealthService _service;

    public WorkerHeartbeatJob(IWorkerHealthService service)
    {
        _service = service;
    }

    public Task ExecuteAsync() => _service.CheckAsync();
}


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


## Repository file: src/SmartFarm.Worker/Scheduling/WorkerJobScheduler.cs

using Hangfire;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using SmartFarm.Worker.Jobs;

namespace SmartFarm.Worker.Scheduling;

public sealed class WorkerJobScheduler : IHostedService
{
    public const string HeartbeatJobId = "smartfarm-worker-check";
    public const string TaskJobId = "smartfarm-tasks-and-reminders";
    public const string WeatherJobId = "smartfarm-weather-check";

    private readonly IRecurringJobManager _recurringJobs;
    private readonly ILogger<WorkerJobScheduler> _logger;

    public WorkerJobScheduler(
        IRecurringJobManager recurringJobs,
        ILogger<WorkerJobScheduler> logger)
    {
        _recurringJobs = recurringJobs;
        _logger = logger;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();

        _recurringJobs.AddOrUpdate<WorkerHeartbeatJob>(
            HeartbeatJobId,
            job => job.ExecuteAsync(),
            Cron.Hourly(),
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _recurringJobs.AddOrUpdate<TaskReminderJob>(
            TaskJobId,
            job => job.ExecuteAsync(),
            Cron.Hourly(),
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _recurringJobs.AddOrUpdate<WeatherCheckJob>(
            WeatherJobId,
            job => job.ExecuteAsync(),
            "0 */6 * * *",
            new RecurringJobOptions { TimeZone = TimeZoneInfo.Utc });

        _logger.LogInformation("Scheduled worker, task, and weather checks.");
        return Task.CompletedTask;
    }

    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;
}


## Repository file: src/SmartFarm.Worker/Services/FarmAutomationService.cs

using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Core.Services.Tasks;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Worker.Services;

public sealed class FarmAutomationService(
    SmartFarmDbContext db,
    IWeatherService weather,
    ILogger<FarmAutomationService> logger) : IFarmAutomationService
{
    private static DateOnly LocalDate(Farm farm, DateTime utcNow) =>
        DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
            utcNow, TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId)));

    public async Task GenerateTasksAndRemindersAsync(CancellationToken ct = default)
    {
        var utcNow = DateTime.UtcNow;
        var farms = await db.Farms.AsNoTracking()
            .Where(f => f.ArchivedAtUtc == null).ToListAsync(ct);

        foreach (var farm in farms)
        {
            DateOnly today;
            try { today = LocalDate(farm, utcNow); }
            catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
            {
                logger.LogWarning("Skipping tasks for farm {FarmId}: invalid timezone.", farm.Id);
                continue;
            }

            var horizon = today.AddDays(7);
            var cycles = await db.CropCycles.AsNoTracking()
                .Where(c => c.Status == 0 || c.Status == 1)
                .Where(c => db.Plots.Any(p => p.Id == c.PlotId &&
                    p.FarmId == farm.Id && p.ArchivedAtUtc == null))
                .ToListAsync(ct);
            var keys = (await db.FarmTasks.AsNoTracking()
                    .Where(t => t.FarmId == farm.Id && t.DedupeKey != null)
                    .Select(t => t.DedupeKey!).ToListAsync(ct))
                .ToHashSet(StringComparer.Ordinal);

            foreach (var crop in cycles)
            {
                foreach (var (type, due) in TaskRules.ScheduledTasks(crop, today, horizon))
                {
                    var key = TaskRules.ScheduleKey(crop.Id, type, due);
                    if (!keys.Add(key)) continue;

                    db.FarmTasks.Add(new FarmTask
                    {
                        FarmId = farm.Id,
                        CropCycleId = crop.Id,
                        AssignedToUserId = farm.OwnerUserId,
                        TaskType = type,
                        DueDate = due,
                        Description = type switch
                        {
                            1 => "Planned planting",
                            2 => "Scheduled watering",
                            _ => "Expected harvest"
                        },
                        DedupeKey = key,
                        CreatedAtUtc = utcNow
                    });
                }
            }

            await db.SaveChangesAsync(ct);

            // One in-app reminder for each pending task that is due or overdue.
            var dueTasks = await db.FarmTasks.AsNoTracking()
                .Where(t => t.FarmId == farm.Id && t.Status == 0 && t.DueDate <= today)
                .ToListAsync(ct);
            var activeCropStatuses = cycles.ToDictionary(c => c.Id, c => c.Status);

            foreach (var task in dueTasks)
            {
                byte? cropStatus = task.CropCycleId is { } cycleId &&
                    activeCropStatuses.TryGetValue(cycleId, out var state) ? state : null;
                if (!TaskRules.IsReminderRelevant(task, cropStatus)) continue;

                var recipient = task.AssignedToUserId ?? farm.OwnerUserId;
                var active = await db.Users.AsNoTracking().AnyAsync(
                    u => u.Id == recipient && u.DisabledAtUtc == null, ct);
                var stillMember = recipient == farm.OwnerUserId || await db.FarmMembers.AsNoTracking()
                    .AnyAsync(m => m.FarmId == farm.Id && m.UserId == recipient &&
                        m.RemovedAtUtc == null, ct);
                if (!active || !stillMember)
                {
                    // Removed assignees must not receive farm information.
                    recipient = farm.OwnerUserId;
                    if (!await db.Users.AsNoTracking().AnyAsync(
                        u => u.Id == recipient && u.DisabledAtUtc == null, ct)) continue;
                }

                var key = TaskRules.ReminderKey(task.Id, task.DueDate);
                if (await db.Notifications.AnyAsync(n => n.UserId == recipient &&
                    n.DeduplicationKey == key, ct)) continue;

                db.Notifications.Add(new Notification
                {
                    UserId = recipient,
                    FarmTaskId = task.Id,
                    Type = 1,
                    DeduplicationKey = key,
                    Title = "Farm task due",
                    Message = $"A farm task is due on {task.DueDate:yyyy-MM-dd}.",
                    CreatedAtUtc = utcNow
                });
            }

            await db.SaveChangesAsync(ct);
        }
    }

    public async Task CheckWeatherAsync(CancellationToken ct = default)
    {
        var utcNow = DateTime.UtcNow;
        var farms = await db.Farms.AsNoTracking()
            .Where(f => f.ArchivedAtUtc == null && f.Latitude != null && f.Longitude != null)
            .ToListAsync(ct);

        foreach (var farm in farms)
        {
            DateOnly today;
            try { today = LocalDate(farm, utcNow); }
            catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
            {
                logger.LogWarning("Skipping weather for farm {FarmId}: invalid time zone.", farm.Id);
                continue;
            }

            WeatherForecastResult forecast;
            try
            {
                forecast = await weather.GetForecastAsync(
                    new WeatherLocation((double)farm.Latitude!.Value, (double)farm.Longitude!.Value), ct);
            }
            catch (WeatherServiceException error) when (error.Failure == WeatherFailure.NotConfigured)
            {
                logger.LogInformation("Weather checks are disabled until a provider is configured.");
                return;
            }
            catch (WeatherServiceException error)
            {
                logger.LogWarning("Weather unavailable for farm {FarmId}: {Failure}",
                    farm.Id, error.Failure);
                continue;
            }

            var validDays = forecast.Days
                .Where(d => d.Date >= today && d.Date <= today.AddDays(2))
                .Where(WeatherRules.IsPlausible)
                .GroupBy(d => d.Date).Select(g => g.First())
                .OrderBy(d => d.Date).ToList();
            if (validDays.Count == 0)
            {
                logger.LogWarning("No usable forecast days for farm {FarmId}.", farm.Id);
                continue;
            }

            var memberIds = await db.FarmMembers.AsNoTracking()
                .Where(m => m.FarmId == farm.Id && m.RemovedAtUtc == null)
                .Select(m => m.UserId).ToListAsync(ct);
            memberIds.Add(farm.OwnerUserId);
            var recipients = await db.Users.AsNoTracking()
                .Where(u => memberIds.Contains(u.Id) && u.DisabledAtUtc == null)
                .Select(u => u.Id).ToListAsync(ct);

            foreach (var day in validDays)
            {
                // Save the forecast even when no warning is generated.
                var snapshot = new WeatherForecast
                {
                    FarmId = farm.Id,
                    ForecastDate = day.Date,
                    RetrievedAtUtc = utcNow,
                    MinTemperatureC = day.MinimumTemperatureC,
                    MaxTemperatureC = day.MaximumTemperatureC,
                    RainfallMm = day.RainfallMm,
                    MaxPrecipitationProbability = day.MaxPrecipitationProbability,
                    MaxWindKph = day.MaxWindKph,
                    Provider = forecast.Provider.Length > 32 ? forecast.Provider[..32] : forecast.Provider
                };
                db.WeatherForecasts.Add(snapshot);

                var warnings = WeatherRules.Evaluate(day);
                var current = await db.WeatherAdvisories
                    .Where(a => a.FarmId == farm.Id && a.AdvisoryDate == day.Date)
                    .ToListAsync(ct);

                // Automatically resolve a warning when a newer forecast no longer predicts it.
                foreach (var previous in current.Where(a => a.Status == 0 &&
                    WeatherRules.HasSignal(day, a.AdvisoryType) &&
                    warnings.All(w => w.Type != a.AdvisoryType)))
                {
                    previous.Status = 1;
                    previous.ResolvedAtUtc = utcNow;
                }

                var alertRows = new List<WeatherAdvisory>();
                foreach (var warning in warnings)
                {
                    var existing = current.FirstOrDefault(a => a.AdvisoryType == warning.Type);
                    if (existing is null)
                    {
                        existing = new WeatherAdvisory
                        {
                            FarmId = farm.Id, AdvisoryDate = day.Date,
                            AdvisoryType = warning.Type, CreatedAtUtc = utcNow,
                            WeatherForecastId = snapshot.Id
                        };
                        db.WeatherAdvisories.Add(existing);
                    }
                    existing.Severity = warning.Severity;
                    existing.Message = warning.Message;
                    existing.WeatherForecastId = snapshot.Id;
                    existing.Status = 0;
                    existing.ResolvedAtUtc = null;
                    alertRows.Add(existing);
                }

                // Persist the forecast and advisories before creating notification references.
                await db.SaveChangesAsync(ct);
                foreach (var advisory in alertRows)
                {
                    var key = $"weather:{advisory.Id:N}:severity:{advisory.Severity}";
                    foreach (var userId in recipients.Distinct())
                    {
                        if (await db.Notifications.AnyAsync(n => n.UserId == userId &&
                            n.DeduplicationKey == key, ct)) continue;
                        db.Notifications.Add(new Notification
                        {
                            UserId = userId,
                            WeatherAdvisoryId = advisory.Id,
                            Type = 2,
                            DeduplicationKey = key,
                            Title = "Farm weather advisory",
                            Message = advisory.Message,
                            CreatedAtUtc = utcNow
                        });
                    }
                }
                await db.SaveChangesAsync(ct);
            }
        }
    }
}


## Repository file: src/SmartFarm.Worker/Services/IFarmAutomationService.cs

namespace SmartFarm.Worker.Services;

public interface IFarmAutomationService
{
    Task GenerateTasksAndRemindersAsync(CancellationToken ct = default);
    Task CheckWeatherAsync(CancellationToken ct = default);
}


## Repository file: src/SmartFarm.Worker/Services/IWorkerHealthService.cs

namespace SmartFarm.Worker.Services;

public interface IWorkerHealthService
{
    Task CheckAsync();
}


## Repository file: src/SmartFarm.Worker/Services/WorkerHealthService.cs

using Hangfire;
using Microsoft.Extensions.Logging;

namespace SmartFarm.Worker.Services;

// Check that Hangfire can read its storage and has a running server.
public sealed class WorkerHealthService : IWorkerHealthService
{
    private readonly JobStorage _storage;
    private readonly ILogger<WorkerHealthService> _logger;

    public WorkerHealthService(JobStorage storage, ILogger<WorkerHealthService> logger)
    {
        _storage = storage;
        _logger = logger;
    }

    public Task CheckAsync()
    {
        var servers = _storage.GetMonitoringApi().Servers();
        if (servers.Count == 0)
            throw new InvalidOperationException("Hangfire has no active worker servers.");

        _logger.LogInformation("SmartFarm worker check passed. Active servers: {ServerCount}.",
            servers.Count);

        return Task.CompletedTask;
    }
}


## Repository file: src/SmartFarm.Worker/appsettings.json

{
  "Hangfire": {
    "Enabled": false,
    "WorkerCount": 1
  },
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.Hosting.Lifetime": "Information",
      "Hangfire": "Information"
    }
  },
  "Weather": {
    "Provider": "OpenMeteo",
    "BaseAddress": "https://api.open-meteo.com/",
    "TimeoutSeconds": 15
  }
}


## Repository file: src/SmartFarm.Infrastructure/Integrations/Weather/IWeatherProviderAdapter.cs

using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Infrastructure.Integrations.Weather;

// Implement only after the provider's endpoint, authentication and response contract are confirmed.
public interface IWeatherProviderAdapter
{
    HttpRequestMessage CreateForecastRequest(WeatherLocation location);

    Task<WeatherForecastResult> ReadForecastAsync(
        HttpResponseMessage response,
        CancellationToken cancellationToken);
}


## Repository file: src/SmartFarm.Infrastructure/Integrations/Weather/OpenMeteoAdapter.cs

using System.Globalization;
using System.Net.Http;
using System.Text.Json;
using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Infrastructure.Integrations.Weather;

// Optional no-key provider. Configure Weather:Provider and Weather:BaseAddress to enable.
public sealed class OpenMeteoAdapter : IWeatherProviderAdapter
{
    public HttpRequestMessage CreateForecastRequest(WeatherLocation location)
    {
        var lat = location.Latitude.ToString(CultureInfo.InvariantCulture);
        var lon = location.Longitude.ToString(CultureInfo.InvariantCulture);
        var path = $"v1/forecast?latitude={lat}&longitude={lon}" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum," +
            "precipitation_probability_max,wind_speed_10m_max" +
            "&timezone=auto&forecast_days=4";
        return new HttpRequestMessage(HttpMethod.Get, path);
    }

    public async Task<WeatherForecastResult> ReadForecastAsync(
        HttpResponseMessage response, CancellationToken cancellationToken)
    {
        await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
        using var json = await JsonDocument.ParseAsync(stream, cancellationToken: cancellationToken);
        if (json.RootElement.ValueKind != JsonValueKind.Object ||
            !json.RootElement.TryGetProperty("daily", out var daily) ||
            daily.ValueKind != JsonValueKind.Object ||
            !daily.TryGetProperty("time", out var dates) ||
            !daily.TryGetProperty("temperature_2m_min", out var lows) ||
            !daily.TryGetProperty("temperature_2m_max", out var highs) ||
            !daily.TryGetProperty("precipitation_sum", out var rain))
            throw new JsonException("Missing required daily weather data.");
        if (dates.ValueKind != JsonValueKind.Array ||
            lows.ValueKind != JsonValueKind.Array ||
            highs.ValueKind != JsonValueKind.Array ||
            rain.ValueKind != JsonValueKind.Array)
            throw new JsonException("Daily weather fields must be arrays.");
        var probability = daily.TryGetProperty("precipitation_probability_max", out var rainChance)
            ? rainChance : (JsonElement?)null;
        var wind = daily.TryGetProperty("wind_speed_10m_max", out var windSpeed)
            ? windSpeed : (JsonElement?)null;

        if (probability is { } chanceArray &&
                (chanceArray.ValueKind != JsonValueKind.Array ||
                 chanceArray.GetArrayLength() != dates.GetArrayLength()) ||
            wind is { } windArray &&
                (windArray.ValueKind != JsonValueKind.Array ||
                 windArray.GetArrayLength() != dates.GetArrayLength()))
            throw new JsonException("Inconsistent optional daily weather data.");

        if (dates.GetArrayLength() != lows.GetArrayLength() ||
            dates.GetArrayLength() != highs.GetArrayLength() ||
            dates.GetArrayLength() != rain.GetArrayLength())
            throw new JsonException("Inconsistent daily weather data.");

        var days = new List<DailyWeatherForecast>();
        for (var i = 0; i < dates.GetArrayLength(); i++)
        {
            if (dates[i].ValueKind != JsonValueKind.String)
                throw new JsonException("Invalid daily date.");
            var date = DateOnly.ParseExact(dates[i].GetString()!, "yyyy-MM-dd", CultureInfo.InvariantCulture);
            days.Add(new DailyWeatherForecast(
                date, Number(lows[i]), Number(highs[i]), Number(rain[i]),
                probability is { } chance ? Number(chance[i]) : null,
                wind is { } speed ? Number(speed[i]) : null));
        }
        return new WeatherForecastResult("Open-Meteo", DateTimeOffset.UtcNow, days);
    }

    private static decimal? Number(JsonElement value) => value.ValueKind switch
    {
        JsonValueKind.Null => null,
        JsonValueKind.Number => value.GetDecimal(),
        _ => throw new JsonException("Invalid numeric weather value.")
    };
}


## Repository file: src/SmartFarm.Infrastructure/Integrations/Weather/WeatherHttpClient.cs

using System.Text.Json;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Interfaces.Weather;

namespace SmartFarm.Infrastructure.Integrations.Weather;

public sealed class WeatherHttpClient(
    HttpClient httpClient,
    IOptions<WeatherOptions> options,
    IWeatherProviderAdapter? adapter = null) : IWeatherService
{
    public async Task<WeatherForecastResult> GetForecastAsync(
        WeatherLocation location,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(location);
        if (!double.IsFinite(location.Latitude) || location.Latitude is < -90 or > 90 ||
            !double.IsFinite(location.Longitude) || location.Longitude is < -180 or > 180)
            throw new ArgumentOutOfRangeException(nameof(location), "Latitude or longitude is out of range.");

        cancellationToken.ThrowIfCancellationRequested();
        var settings = options.Value;
        if (adapter is null || settings.BaseAddress is null)
            throw new WeatherServiceException(WeatherFailure.NotConfigured,
                "Weather integration requires a confirmed provider adapter and HTTPS base address.");

        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeout.CancelAfter(TimeSpan.FromSeconds(settings.TimeoutSeconds));

        using var request = adapter.CreateForecastRequest(location);
        // A provider adapter may choose the path, but credentials stay on the configured origin.
        if (request.RequestUri is null || !Uri.TryCreate(settings.BaseAddress, request.RequestUri, out var target) ||
            target.GetLeftPart(UriPartial.Authority) != settings.BaseAddress.GetLeftPart(UriPartial.Authority) ||
            !string.IsNullOrEmpty(target.UserInfo) || !string.IsNullOrEmpty(target.Fragment))
            throw new WeatherServiceException(WeatherFailure.NotConfigured,
                "Weather provider request must use the configured HTTPS origin.");

        request.RequestUri = target;
        if (settings.ApiKeyHeaderName is not null)
        {
            request.Headers.Remove(settings.ApiKeyHeaderName);
            request.Headers.Add(settings.ApiKeyHeaderName, settings.ApiKey);
        }

        try
        {
            using var response = await httpClient.SendAsync(
                request, HttpCompletionOption.ResponseHeadersRead, timeout.Token);
            if (!response.IsSuccessStatusCode)
                throw new WeatherServiceException(WeatherFailure.Unavailable,
                    $"Weather provider returned HTTP {(int)response.StatusCode}.");

            var forecast = await adapter.ReadForecastAsync(response, timeout.Token);
            return forecast ?? throw new WeatherServiceException(WeatherFailure.InvalidResponse,
                "Weather provider returned an invalid forecast.");
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
        {
            throw new WeatherServiceException(WeatherFailure.Timeout, "Weather provider request timed out.");
        }
        catch (Exception exception) when (exception is HttpRequestException or IOException)
        {
            // Do not expose provider URLs, response bodies or credential-bearing inner exceptions.
            throw new WeatherServiceException(WeatherFailure.Unavailable, "Weather provider request failed.");
        }
        catch (Exception exception) when (exception is JsonException or FormatException)
        {
            throw new WeatherServiceException(WeatherFailure.InvalidResponse,
                "Weather provider returned an invalid forecast.");
        }
    }
}


## Repository file: src/SmartFarm.Infrastructure/Integrations/Weather/WeatherOptions.cs

namespace SmartFarm.Infrastructure.Integrations.Weather;

public sealed class WeatherOptions
{
    public const string SectionName = "Weather";

    public Uri? BaseAddress { get; set; }
    public int TimeoutSeconds { get; set; } = 15;
    public string? ApiKeyHeaderName { get; set; }
    public string? ApiKey { get; set; }

    internal bool HasValidBaseAddress() => BaseAddress is null ||
        (BaseAddress.IsAbsoluteUri && BaseAddress.Scheme == Uri.UriSchemeHttps &&
         string.IsNullOrEmpty(BaseAddress.UserInfo) && string.IsNullOrEmpty(BaseAddress.Query) &&
         string.IsNullOrEmpty(BaseAddress.Fragment));

    internal bool HasValidApiKeyHeader()
    {
        if (ApiKeyHeaderName is null && ApiKey is null)
            return true;

        if (string.IsNullOrWhiteSpace(ApiKeyHeaderName) || string.IsNullOrWhiteSpace(ApiKey) ||
            ApiKey.Contains('\r') || ApiKey.Contains('\n'))
            return false;

        using var request = new HttpRequestMessage();
        try
        {
            request.Headers.Add(ApiKeyHeaderName, ApiKey);
            // Never allow configured credentials to alter request routing.
            return !ApiKeyHeaderName.Equals("Host", StringComparison.OrdinalIgnoreCase);
        }
        catch (FormatException)
        {
            return false;
        }
        catch (InvalidOperationException)
        {
            return false;
        }
    }
}


## Repository file: src/SmartFarm.Infrastructure/Integrations/Weather/WeatherServiceCollectionExtensions.cs

using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Interfaces.Weather;

namespace SmartFarm.Infrastructure.Integrations.Weather;

public static class WeatherServiceCollectionExtensions
{
    public static IHttpClientBuilder AddSmartFarmWeather(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        if (string.Equals(configuration["Weather:Provider"], "OpenMeteo", StringComparison.OrdinalIgnoreCase))
        {
            const string expected = "https://api.open-meteo.com/";
            if (!string.Equals(configuration["Weather:BaseAddress"]?.TrimEnd('/') + "/", expected,
                    StringComparison.OrdinalIgnoreCase))
                throw new InvalidOperationException("OpenMeteo requires Weather:BaseAddress=https://api.open-meteo.com/");
            services.AddSingleton<IWeatherProviderAdapter, OpenMeteoAdapter>();
        }
        else if (!string.IsNullOrWhiteSpace(configuration["Weather:Provider"]))
        {
            throw new InvalidOperationException("Unsupported weather provider.");
        }

        services.AddOptions<WeatherOptions>()
            .Bind(configuration.GetSection(WeatherOptions.SectionName))
            .Validate(options => options.TimeoutSeconds is >= 1 and <= 120,
                "Weather:TimeoutSeconds must be between 1 and 120.")
            .Validate(options => options.HasValidBaseAddress(),
                "Weather:BaseAddress must be an absolute HTTPS URL without credentials, query or fragment.")
            .Validate(options => options.HasValidApiKeyHeader(),
                "Weather API-key header configuration is invalid; configure both name and key or neither.")
            .ValidateOnStart();

        return services.AddHttpClient<IWeatherService, WeatherHttpClient>((provider, client) =>
            {
                client.BaseAddress = provider.GetRequiredService<IOptions<WeatherOptions>>().Value.BaseAddress;
                // The typed client applies one timeout to sending and parsing the response.
                client.Timeout = Timeout.InfiniteTimeSpan;
            })
            .ConfigurePrimaryHttpMessageHandler(() => new HttpClientHandler { AllowAutoRedirect = false })
            .RedactLoggedHeaders(_ => true)
            .RemoveAllLoggers();
    }
}


## Repository file: src/SmartFarm.Infrastructure/Services/Weather/FarmWeatherService.cs

using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Weather;

// Shared read model for MVC and API. Forecast retrieval remains a Worker concern.
public sealed class FarmWeatherService(IFarmAccessService access, SmartFarmDbContext db)
    : IFarmWeatherService
{
    public async Task<WeatherOverview> GetAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct))
            throw new FarmOperationException(FarmError.NotFound, "Resource not found.");

        var farm = await db.Farms.AsNoTracking().SingleOrDefaultAsync(
            f => f.Id == farmId && f.ArchivedAtUtc == null, ct);
        if (farm is null)
            throw new FarmOperationException(FarmError.NotFound, "Resource not found.");

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        try
        {
            today = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
                DateTime.UtcNow, TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId)));
        }
        catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
        {
            // Legacy rows with invalid zones can still view their weather data.
        }

        var forecasts = await db.WeatherForecasts.AsNoTracking()
            .Where(f => f.FarmId == farmId && f.ForecastDate >= today &&
                f.ForecastDate <= today.AddDays(4))
            .OrderByDescending(f => f.RetrievedAtUtc)
            .Take(100).ToListAsync(ct);
        var days = forecasts.GroupBy(f => f.ForecastDate)
            .Select(g => g.OrderByDescending(f => f.RetrievedAtUtc).First())
            .OrderBy(f => f.ForecastDate)
            .Select(f => new WeatherDayDetails(f.ForecastDate, f.MinTemperatureC,
                f.MaxTemperatureC, f.RainfallMm, f.RetrievedAtUtc, f.Provider,
                f.MaxPrecipitationProbability, f.MaxWindKph))
            .ToList();

        var alerts = await db.WeatherAdvisories.AsNoTracking()
            .Where(a => a.FarmId == farmId && a.AdvisoryDate >= today &&
                a.AdvisoryDate <= today.AddDays(4))
            .OrderBy(a => a.AdvisoryDate).ThenByDescending(a => a.Severity)
            .Select(a => new WeatherAdvisoryDetails(
                a.Id, a.AdvisoryDate, a.AdvisoryType, a.Severity,
                a.Status, a.Message, a.CreatedAtUtc))
            .ToListAsync(ct);

        return new WeatherOverview(farmId, farm.Latitude.HasValue && farm.Longitude.HasValue,
            farm.TimeZoneId, days, alerts);
    }
}


## Repository file: src/SmartFarm.Infrastructure/Services/Tasks/FarmTaskService.cs

using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Services.Tasks;

public sealed class FarmTaskService(
    IFarmTaskRepository tasks,
    IFarmAccessService access,
    IUnitOfWork unit) : IFarmTaskService
{
    private static FarmOperationException NotFound() => new(FarmError.NotFound, "Resource not found.");
    private static FarmOperationException Invalid(string message) => new(FarmError.InvalidInput, message);
    private static FarmOperationException Conflict() => new(FarmError.Conflict, "This task has changed. Reload and retry.");

    private static FarmTaskDetails Map(FarmTask task) => new(
        task.Id, task.FarmId, task.CropCycleId, task.AssignedToUserId,
        task.TaskType, task.DueDate, task.Status, task.Description,
        task.CreatedAtUtc, task.ClosedAtUtc, Convert.ToBase64String(task.RowVersion));

    private static NotificationDetails Map(Notification n) => new(
        n.Id, n.Type, n.Title, n.Message, n.FarmTaskId, n.WeatherAdvisoryId,
        n.CreatedAtUtc, n.ReadAtUtc);

    private async Task Require(Guid farmId, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, action, ct))
            throw NotFound();
    }

    private async Task Save(CancellationToken ct)
    {
        try { await unit.SaveChangesAsync(ct); }
        catch (DbUpdateConcurrencyException) { throw Conflict(); }
        catch (DbUpdateException e) when (e.InnerException is SqlException { Number: 547 })
        {
            throw Invalid("The task uses an invalid farm, crop, or user.");
        }
    }

    public async Task<IReadOnlyList<FarmTaskDetails>> ListAsync(Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        return (await tasks.ListAsync(farmId, ct)).Select(Map).ToList();
    }

    public async Task<FarmTaskDetails> CreateAsync(Guid farmId, Guid actor, FarmTaskInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (TaskRules.Validate(input) is { } error) throw Invalid(error);
        if (input.CropCycleId is { } cycle && !await tasks.CropBelongsToFarmAsync(farmId, cycle, ct))
            throw NotFound();
        if (input.AssignedToUserId is { } assignee && !await tasks.IsAssignableAsync(farmId, assignee, ct))
            throw Invalid("Assign the task to an active farm member.");

        var task = new FarmTask
        {
            FarmId = farmId,
            CropCycleId = input.CropCycleId,
            AssignedToUserId = input.AssignedToUserId,
            TaskType = input.TaskType,
            DueDate = input.DueDate,
            Description = input.Description?.Trim(),
            CreatedAtUtc = DateTime.UtcNow
        };
        tasks.Add(task);
        await Save(ct);
        return Map(task);
    }

    private static byte[] ParseVersion(string? version)
    {
        try
        {
            var parsed = Convert.FromBase64String(version ?? "");
            if (parsed.Length == 8) return parsed;
        }
        catch (FormatException) { }
        throw Invalid("A current row version is required.");
    }

    public async Task<FarmTaskDetails> UpdateAsync(Guid farmId, Guid taskId, Guid actor,
        FarmTaskUpdateInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (TaskRules.ValidateUpdate(input) is { } error) throw Invalid(error);
        var task = await tasks.FindAsync(farmId, taskId, ct) ?? throw NotFound();
        if (task.Status != 0) throw Conflict();
        if (task.CropCycleId is { } cycle &&
            !await tasks.CropBelongsToFarmAsync(farmId, cycle, ct))
            throw Conflict();
        if (input.AssignedToUserId is { } assignee &&
            !await tasks.IsAssignableAsync(farmId, assignee, ct))
            throw Invalid("Assign the task to an active farm member.");

        tasks.SetOriginalVersion(task, ParseVersion(input.RowVersion));
        if (task.DueDate != input.DueDate || task.AssignedToUserId != input.AssignedToUserId)
            await tasks.DismissRemindersAsync(task.Id, DateTime.UtcNow, ct);
        task.DueDate = input.DueDate;
        task.AssignedToUserId = input.AssignedToUserId;
        task.Description = input.Description?.Trim();
        await Save(ct);
        return Map(task);
    }

    public async Task<FarmTaskDetails> CloseAsync(Guid farmId, Guid taskId, Guid actor,
        FarmTaskCloseInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var task = await tasks.FindAsync(farmId, taskId, ct) ?? throw NotFound();
        var level = await access.GetAccessLevelAsync(farmId, actor, ct);
        if (level == FarmAccessLevel.FieldWorker && task.AssignedToUserId != actor)
            throw NotFound();
        if (task.Status != 0) throw Conflict();
        if (!TaskRules.IsValidCloseStatus(input.Status))
            throw Invalid("Choose Completed, Skipped, or Cancelled.");

        tasks.SetOriginalVersion(task, ParseVersion(input.RowVersion));
        TaskRules.Close(task, input.Status, actor, DateTime.UtcNow);
        await tasks.DismissRemindersAsync(task.Id, task.ClosedAtUtc!.Value, ct);
        await Save(ct);
        return Map(task);
    }

    public async Task<IReadOnlyList<NotificationDetails>> NotificationsAsync(Guid actor, CancellationToken ct = default)
    {
        if (actor == Guid.Empty) throw NotFound();
        return (await tasks.ListNotificationsAsync(actor, ct)).Select(Map).ToList();
    }

    public async Task MarkNotificationReadAsync(Guid actor, Guid notificationId, CancellationToken ct = default)
    {
        if (actor == Guid.Empty) throw NotFound();
        var notification = await tasks.FindNotificationAsync(actor, notificationId, ct) ?? throw NotFound();
        notification.ReadAtUtc ??= DateTime.UtcNow;
        await Save(ct);
    }
}


## Repository file: src/SmartFarm.Core/Services/Tasks/TaskRules.cs

using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Services.Tasks;

public static class TaskRules
{
    public static bool IsValidType(byte type) => type is >= 1 and <= 6;
    public static bool IsValidCloseStatus(byte status) => status is >= 1 and <= 3;

    public static string? Validate(FarmTaskInput input)
    {
        if (!IsValidType(input.TaskType) || input.DueDate == default)
            return "Choose a task type and due date.";
        if (input.Description?.Trim().Length > 500)
            return "Task description cannot exceed 500 characters.";
        if (input.CropCycleId == Guid.Empty || input.AssignedToUserId == Guid.Empty)
            return "Choose a valid crop and assignee.";
        return null;
    }

    public static byte? ForActivity(byte activityType) => activityType switch
    {
        1 => (byte)2, // Watering
        2 => (byte)3, // Fertilizing
        _ => null
    };

    public static string? ValidateUpdate(FarmTaskUpdateInput input)
    {
        if (input.DueDate == default || input.Description?.Trim().Length > 500 ||
            input.AssignedToUserId == Guid.Empty)
            return "Choose a valid due date, assignee, and description up to 500 characters.";
        return null;
    }

    // Recording actual work may fulfill a generated farm-calendar task.
    // Manual assignments remain limited to their assignee or farm managers.
    public static bool CanFulfillFromRecord(FarmTask task, Guid actor, bool mayManage) =>
        mayManage || task.AssignedToUserId == actor || task.DedupeKey is not null;

    public static bool Close(FarmTask task, byte status, Guid actor, DateTime utcNow)
    {
        if (task.Status != 0 || !IsValidCloseStatus(status)) return false;
        task.Status = status;
        task.ClosedAtUtc = utcNow;
        task.ClosedByUserId = actor;
        return true;
    }

    // Farm dates use their configured time zone, not the worker's UTC calendar day.
    public static DateOnly FarmDate(string timeZoneId, DateTime utcNow) =>
        DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
            utcNow, TimeZoneInfo.FindSystemTimeZoneById(timeZoneId)));

    // Create upcoming planting, watering and harvest dates from the crop calendar.
    public static IEnumerable<(byte Type, DateOnly Due)> ScheduledTasks(
        CropCycle crop, DateOnly today, DateOnly lastDay)
    {
        if (crop.Status == 0 && crop.PlannedPlantingDate <= lastDay)
            yield return (1, crop.PlannedPlantingDate);

        if (crop.Status != 1) yield break;

        if (crop.PlantedDate is { } planted && crop.WateringIntervalDays is > 0)
        {
            var interval = crop.WateringIntervalDays.Value;
            var days = today.DayNumber - planted.DayNumber;
            var next = Math.Max(1, (days + interval - 1) / interval);
            for (var due = planted.AddDays(next * interval); due <= lastDay; due = due.AddDays(interval))
            {
                if (due >= today) yield return (2, due);
            }
        }

        if (crop.ExpectedHarvestDate is { } harvest && harvest <= lastDay)
            yield return (5, harvest);
    }

    public static string ReminderKey(Guid taskId, DateOnly due) =>
        $"task:{taskId:N}:due:{due:yyyyMMdd}";

    public static bool IsReminderRelevant(FarmTask task, byte? cropStatus) =>
        task.Status == 0 && (task.CropCycleId is null ||
            (cropStatus is 0 or 1 && !(task.TaskType == 1 && cropStatus == 1)));

    // Deterministic keys keep recurring jobs safe to rerun.
    public static string ScheduleKey(Guid cropId, byte taskType, DateOnly due) =>
        $"cycle:{cropId:N}:type:{taskType}:date:{due:yyyyMMdd}";
}


## Repository file: src/SmartFarm.Core/Services/Tasks/WeatherRules.cs

using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Core.Services.Tasks;

public sealed record WeatherWarning(byte Type, byte Severity, string Message);

public static class WeatherRules
{
    // Reject implausible provider values before writing decimal SQL columns.
    public static bool IsPlausible(DailyWeatherForecast day) =>
        (day.MinimumTemperatureC is null or >= -100m and <= 100m) &&
        (day.MaximumTemperatureC is null or >= -100m and <= 100m) &&
        (day.MinimumTemperatureC is null || day.MaximumTemperatureC is null ||
            day.MinimumTemperatureC <= day.MaximumTemperatureC) &&
        (day.RainfallMm is null or >= 0m and <= 10000m) &&
        (day.MaxPrecipitationProbability is null or >= 0m and <= 100m) &&
        (day.MaxWindKph is null or >= 0m and <= 300m);

    // Missing provider readings are not evidence that an existing warning has cleared.
    public static bool HasSignal(DailyWeatherForecast day, byte warningType) => warningType switch
    {
        1 => day.RainfallMm.HasValue,
        2 => day.MaxWindKph.HasValue,
        3 => day.MaximumTemperatureC.HasValue,
        _ => false
    };

    public static IReadOnlyList<WeatherWarning> Evaluate(DailyWeatherForecast day)
    {
        var results = new List<WeatherWarning>();
        if (day.RainfallMm is >= 20m)
            results.Add(new WeatherWarning(1, day.RainfallMm >= 50m ? (byte)3 : (byte)2,
                $"Heavy rain is forecast ({day.RainfallMm:0.#} mm). Check drainage and outdoor farm work."));
        if (day.MaxWindKph is >= 50m)
            results.Add(new WeatherWarning(2, day.MaxWindKph >= 80m ? (byte)3 : (byte)2,
                $"Strong wind is forecast ({day.MaxWindKph:0.#} km/h). Secure equipment and assess outdoor work."));
        if (day.MaximumTemperatureC is >= 35m)
            results.Add(new WeatherWarning(3, day.MaximumTemperatureC >= 40m ? (byte)3 : (byte)2,
                $"High temperature is forecast ({day.MaximumTemperatureC:0.#}°C). Monitor crop heat stress."));
        return results;
    }
}
