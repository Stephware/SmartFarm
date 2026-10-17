# Weather integration

`AddSmartFarmWeather(configuration)` registers the provider-independent `IWeatherService` as a typed HTTP client. No provider has been selected. Calls return `WeatherFailure.NotConfigured` until a confirmed `IWeatherProviderAdapter` and HTTPS base address are configured. Host startup needs neither a provider nor an API key.

After confirming the provider, implement and register one adapter to construct its request and map its response to `WeatherForecastResult`. The adapter owns the provider's endpoint, units, and response contract. No live weather calls or automation run during initialization.

Configure these values through host user secrets or environment variables:

- `Weather__BaseAddress`: confirmed HTTPS origin/base path, without credentials or query parameters.
- `Weather__TimeoutSeconds`: 1–120; default 15, covering request and response parsing.
- `Weather__ApiKeyHeaderName` and `Weather__ApiKey`: optional pair, only if the confirmed provider uses header authentication. Keep the key in user secrets or a deployment secret store. Other authentication methods require the confirmed adapter/configuration to be implemented first.

The client validates configured options at startup, blocks requests to another origin, disables redirects and HTTP request logging, and sanitizes transport/provider errors. Caller cancellation propagates; internal timeout, HTTP errors, and malformed responses use `WeatherServiceException`. Adapters must pass cancellation into response reading and use `JsonException` or `FormatException` for malformed payloads.

Run fake-handler tests without credentials or network access:

```sh
dotnet test tests/SmartFarm.Weather.Tests/SmartFarm.Weather.Tests.csproj
```
