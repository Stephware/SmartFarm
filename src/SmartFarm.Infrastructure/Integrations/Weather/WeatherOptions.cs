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
