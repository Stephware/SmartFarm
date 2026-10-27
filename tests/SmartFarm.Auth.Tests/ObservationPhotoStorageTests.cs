using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Infrastructure.Services.Monitoring;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class ObservationPhotoStorageTests
{
    private static readonly string LocalPath = Path.Combine(
        Path.GetTempPath(), "anigo-private-photos");

    [Fact]
    public void DevelopmentUsesExplicitOrExistingLocalPath()
    {
        Assert.Equal(Path.GetFullPath(LocalPath),
            ObservationPhotoStorageRegistration.ResolvePath(null, true, LocalPath));
        var sharedPath = Path.Combine(Path.GetTempPath(), "shared-private-media");
        Assert.Equal(Path.GetFullPath(sharedPath),
            ObservationPhotoStorageRegistration.ResolvePath(sharedPath, true, LocalPath));
    }

    [Fact]
    public void ProductionRequiresAnAbsolutePrivateMountedDirectory()
    {
        Assert.Throws<InvalidOperationException>(() =>
            ObservationPhotoStorageRegistration.ResolvePath(null, false, LocalPath));
        Assert.Throws<InvalidOperationException>(() =>
            ObservationPhotoStorageRegistration.ResolvePath("relative/photos", false, LocalPath));
        Assert.Throws<InvalidOperationException>(() =>
            ObservationPhotoStorageRegistration.ResolvePath(Path.Combine(
                Path.GetTempPath(), "wwwroot", "photos"), false, LocalPath));
        Assert.Throws<InvalidOperationException>(() =>
            ObservationPhotoStorageRegistration.ResolvePath(Path.GetPathRoot(LocalPath), false, LocalPath));
    }

    [Fact]
    public void BothHostsUseTheSameConfiguredStoreRegistration()
    {
        var sharedPath = Path.Combine(Path.GetTempPath(), "anigo-shared-photo-store");
        var config = new ConfigurationBuilder().AddInMemoryCollection(
            new Dictionary<string, string?> { ["ObservationPhotos:StoragePath"] = sharedPath }).Build();

        foreach (var development in new[] { true, false })
        {
            var services = new ServiceCollection();
            services.AddSharedObservationPhotoStore(config, development, LocalPath);
            using var scope = services.BuildServiceProvider();
            Assert.IsType<LocalObservationPhotoStore>(
                scope.GetRequiredService<IObservationPhotoStore>());
        }
    }
}
