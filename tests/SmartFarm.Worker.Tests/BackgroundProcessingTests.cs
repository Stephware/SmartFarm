using Hangfire;
using Hangfire.SqlServer;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using SmartFarm.Worker;
using Xunit;

namespace SmartFarm.Worker.Tests;

public sealed class BackgroundProcessingTests
{
    [Fact]
    public async Task DisabledWorkerStartsWithoutStorageOrCredentials()
    {
        var builder = Host.CreateApplicationBuilder(new HostApplicationBuilderSettings
        {
            DisableDefaults = true
        });
        builder.Configuration.AddInMemoryCollection(new Dictionary<string, string?>
        {
            ["ConnectionStrings:Hangfire"] = "invalid and unused"
        });
        builder.Services.AddSmartFarmBackgroundProcessing(builder.Configuration);

        using var host = builder.Build();
        Assert.Null(host.Services.GetService<JobStorage>());
        Assert.Null(host.Services.GetService<IBackgroundJobClient>());
        await host.StartAsync();
        await host.StopAsync();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("Server=127.0.0.1,1;Database=SmartFarmDB")]
    [InlineData("Server=127.0.0.1,1;Database=sMaRtFaRm")]
    [InlineData("Server=127.0.0.1,1;Database=master")]
    [InlineData("Server=127.0.0.1,1")]
    [InlineData("Database=SmartFarmJobsDB")]
    [InlineData("Server=127.0.0.1,1;Database=SmartFarmJobsDB;AttachDbFilename=test.mdf")]
    public void EnabledWorkerRejectsMissingOrUnsafeDatabase(string? connectionString)
    {
        var configuration = Configuration(connectionString);
        var exception = Assert.Throws<InvalidOperationException>(() =>
            new ServiceCollection().AddSmartFarmBackgroundProcessing(configuration));

        Assert.Contains("separate, pre-provisioned jobs database", exception.Message);
    }

    [Fact]
    public void EnabledWorkerRejectsApplicationCatalog()
    {
        var configuration = Configuration("Server=127.0.0.1,1;Database=OtherFarmDB");
        configuration["ConnectionStrings:SmartFarm"] = "Server=127.0.0.1,1;Database=OtherFarmDB";

        Assert.Throws<InvalidOperationException>(() =>
            new ServiceCollection().AddSmartFarmBackgroundProcessing(configuration));
    }

    [Fact]
    public void InvalidConnectionDoesNotExposeCredentials()
    {
        var configuration = Configuration("Server=localhost;Password=test-sentinel;UnsupportedKey=invalid");
        var exception = Assert.Throws<InvalidOperationException>(() =>
            new ServiceCollection().AddSmartFarmBackgroundProcessing(configuration));

        Assert.DoesNotContain("test-sentinel", exception.ToString());
        Assert.DoesNotContain("UnsupportedKey", exception.ToString());
    }

    [Theory]
    [InlineData(0)]
    [InlineData(65)]
    public void EnabledWorkerRejectsInvalidWorkerCount(int workerCount)
    {
        var configuration = Configuration("Server=127.0.0.1,1;Database=SmartFarmJobsDB");
        configuration["Hangfire:WorkerCount"] = workerCount.ToString();

        Assert.Throws<InvalidOperationException>(() =>
            new ServiceCollection().AddSmartFarmBackgroundProcessing(configuration));
    }

    [Fact]
    public void EnabledWorkerRegistersProcessingAndSchedulingWithoutConnecting()
    {
        var services = new ServiceCollection();
        services.AddLogging();
        services.AddSmartFarmBackgroundProcessing(Configuration(
            "Server=127.0.0.1,1;Database=SmartFarmJobsDB;Integrated Security=true;Connect Timeout=1"));
        using var provider = services.BuildServiceProvider();

        Assert.IsType<SqlServerStorage>(provider.GetRequiredService<JobStorage>());
        Assert.NotNull(provider.GetRequiredService<IBackgroundJobClient>());
        Assert.NotNull(provider.GetRequiredService<IRecurringJobManager>());
        Assert.NotEmpty(provider.GetServices<IHostedService>());
    }

    [Fact]
    public void StorageNeverInstallsOrUpgradesSchema()
    {
        var options = BackgroundProcessingRegistration.CreateStorageOptions();

        Assert.False(options.PrepareSchemaIfNecessary);
        Assert.False(options.EnableHeavyMigrations);
        Assert.False(options.TryAutoDetectSchemaDependentOptions);
        Assert.IsType<Microsoft.Data.SqlClient.SqlClientFactory>(options.SqlClientFactory);
    }

    private static IConfigurationRoot Configuration(string? connectionString) =>
        new ConfigurationBuilder().AddInMemoryCollection(new Dictionary<string, string?>
        {
            ["Hangfire:Enabled"] = "true",
            ["ConnectionStrings:Hangfire"] = connectionString
        }).Build();
}
