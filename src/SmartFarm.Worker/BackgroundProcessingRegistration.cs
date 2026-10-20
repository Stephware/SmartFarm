using Hangfire;
using Hangfire.SqlServer;
using Microsoft.Data.SqlClient;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Worker.Jobs;
using SmartFarm.Worker.Scheduling;

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
        services.AddScoped<WorkerHeartbeatJob>();
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
