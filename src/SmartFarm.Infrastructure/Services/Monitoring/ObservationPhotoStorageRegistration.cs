using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Interfaces.Monitoring;

namespace SmartFarm.Infrastructure.Services.Monitoring;

public static class ObservationPhotoStorageRegistration
{
    // Both hosts must use the same mounted private directory in deployment.
    // Development keeps the existing shared Web/App_Data location.
    public static IServiceCollection AddSharedObservationPhotoStore(
        this IServiceCollection services, IConfiguration configuration,
        bool isDevelopment, string developmentDefault)
    {
        var root = ResolvePath(configuration["ObservationPhotos:StoragePath"],
            isDevelopment, developmentDefault);
        services.AddSingleton<IObservationPhotoStore>(new LocalObservationPhotoStore(root));
        return services;
    }

    public static string ResolvePath(string? configured, bool isDevelopment, string developmentDefault)
    {
        if (string.IsNullOrWhiteSpace(configured) && !isDevelopment)
            throw new InvalidOperationException(
                "Set ObservationPhotos:StoragePath to the same absolute, mounted private directory " +
                "on both MVC and API hosts before deployment.");

        var value = string.IsNullOrWhiteSpace(configured) ? developmentDefault : configured.Trim();
        if (!Path.IsPathFullyQualified(value))
            throw new InvalidOperationException("ObservationPhotos:StoragePath must be an absolute path.");

        var fullPath = Path.GetFullPath(value);
        if (string.Equals(fullPath, Path.GetPathRoot(fullPath), StringComparison.OrdinalIgnoreCase) ||
            fullPath.Split(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar)
                .Any(part => part.Equals("wwwroot", StringComparison.OrdinalIgnoreCase)))
            throw new InvalidOperationException(
                "Photo storage must be a private directory outside wwwroot, not a drive root.");

        return fullPath;
    }
}
