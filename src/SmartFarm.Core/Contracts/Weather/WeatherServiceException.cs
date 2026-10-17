namespace SmartFarm.Core.Contracts.Weather;

public enum WeatherFailure
{
    NotConfigured,
    Unavailable,
    Timeout,
    InvalidResponse
}

public sealed class WeatherServiceException(WeatherFailure failure, string message) : Exception(message)
{
    public WeatherFailure Failure { get; } = failure;
}
