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
    public async Task TwoSeparateHostsCanSaveAndReadTheSamePrivatePhoto()
    {
        var sharedPath = Path.Combine(Path.GetTempPath(), "anigo-media-" + Guid.NewGuid().ToString("N"));
        var config = new ConfigurationBuilder().AddInMemoryCollection(
            new Dictionary<string, string?> { ["ObservationPhotos:StoragePath"] = sharedPath }).Build();
        try
        {
            var webServices = new ServiceCollection();
            webServices.AddSharedObservationPhotoStore(config, false, LocalPath);
            var apiServices = new ServiceCollection();
            apiServices.AddSharedObservationPhotoStore(config, false, LocalPath);
            using var webHost = webServices.BuildServiceProvider();
            using var apiHost = apiServices.BuildServiceProvider();
            var web = webHost.GetRequiredService<IObservationPhotoStore>();
            var api = apiHost.GetRequiredService<IObservationPhotoStore>();

            var bytes = new byte[] { 1, 2, 3, 4 };
            var key = await web.SaveAsync(bytes, ".jpg");
            await using (var stream = await api.OpenAsync(key))
            {
                using var output = new MemoryStream();
                await stream.CopyToAsync(output);
                Assert.Equal(bytes, output.ToArray());
            }
            await api.DeleteAsync(key);
        }
        finally
        {
            if (Directory.Exists(sharedPath)) Directory.Delete(sharedPath, true);
        }
    }
}
