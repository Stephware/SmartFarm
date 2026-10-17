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
