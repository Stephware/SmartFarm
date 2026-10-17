using System.Net;
using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Integrations.Weather;
using Xunit;

namespace SmartFarm.Weather.Tests;

public sealed class WeatherServiceTests
{
    private static readonly WeatherLocation Location = new(14.6, 121.0);
    private static readonly WeatherForecastResult Forecast = new("Test provider", DateTimeOffset.UnixEpoch,
        [new(new DateOnly(2026, 10, 17), 23, 30, 4)]);

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task UnconfiguredProviderStartsButFailsWithoutSendingARequest(bool configured)
    {
        var handler = new FakeHandler((_, _) => throw new InvalidOperationException("Must not send."));
        using var services = CreateServices(handler, adapter: null, configured: configured);
        services.GetRequiredService<IStartupValidator>().Validate();

        var error = await Assert.ThrowsAsync<WeatherServiceException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));

        Assert.Equal(WeatherFailure.NotConfigured, error.Failure);
        Assert.Equal(0, handler.RequestCount);
    }

    [Fact]
    public async Task ConfirmedAdapterWithoutBaseAddressDoesNotSendRequest()
    {
        var handler = OkHandler();
        using var services = CreateServices(handler, new TestAdapter(), configured: false);

        var error = await Assert.ThrowsAsync<WeatherServiceException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));

        Assert.Equal(WeatherFailure.NotConfigured, error.Failure);
        Assert.Equal(0, handler.RequestCount);
    }

    [Fact]
    public async Task AdapterCanUseProviderWithoutApiKey()
    {
        var handler = new FakeHandler((request, _) =>
        {
            Assert.Empty(request.Headers);
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK));
        });
        using var services = CreateServices(handler, new TestAdapter(), configured: false,
            settings: new() { ["Weather:BaseAddress"] = "https://weather.test/api/" });

        Assert.Same(Forecast, await services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));
    }

    [Fact]
    public async Task ConfiguredTypedClientSendsKeyAndReturnsProviderIndependentForecast()
    {
        var handler = new FakeHandler((request, _) =>
        {
            Assert.Equal(new Uri("https://weather.test/api/forecast"), request.RequestUri);
            Assert.Equal("test-only-value", Assert.Single(request.Headers.GetValues("X-Test-Key")));
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK));
        });
        using var services = CreateServices(handler, new TestAdapter());

        var result = await services.GetRequiredService<IWeatherService>().GetForecastAsync(Location);

        Assert.Same(Forecast, result);
        Assert.Equal(1, handler.RequestCount);
    }

    [Theory]
    [InlineData(HttpStatusCode.Unauthorized)]
    [InlineData(HttpStatusCode.TooManyRequests)]
    [InlineData(HttpStatusCode.InternalServerError)]
    [InlineData(HttpStatusCode.Redirect)]
    public async Task HttpFailureIsSanitized(HttpStatusCode status)
    {
        using var services = CreateServices(new FakeHandler((_, _) => Task.FromResult(
            new HttpResponseMessage(status) { Content = new StringContent("private provider details") })),
            new TestAdapter());

        var error = await Assert.ThrowsAsync<WeatherServiceException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));

        Assert.Equal(WeatherFailure.Unavailable, error.Failure);
        Assert.DoesNotContain("private", error.Message);
        Assert.Null(error.InnerException);
    }

    [Fact]
    public async Task TransportFailureDoesNotExposeCredentials()
    {
        using var services = CreateServices(new FakeHandler((_, _) =>
            throw new HttpRequestException("https://weather.test/?key=test-only-value")), new TestAdapter());

        var error = await Assert.ThrowsAsync<WeatherServiceException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));

        Assert.Equal(WeatherFailure.Unavailable, error.Failure);
        Assert.DoesNotContain("test-only-value", error.ToString());
    }

    [Fact]
    public async Task InvalidProviderResponseIsSanitized()
    {
        var adapter = new TestAdapter { Read = (_, _) => throw new JsonException("private response") };
        using var services = CreateServices(OkHandler(), adapter);

        var error = await Assert.ThrowsAsync<WeatherServiceException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));

        Assert.Equal(WeatherFailure.InvalidResponse, error.Failure);
        Assert.DoesNotContain("private response", error.ToString());
    }

    [Fact]
    public async Task CancellationDuringSendPropagatesToCaller()
    {
        using var cancellation = new CancellationTokenSource();
        var handler = new FakeHandler(async (_, token) =>
        {
            cancellation.Cancel();
            await Task.Delay(Timeout.InfiniteTimeSpan, token);
            return new HttpResponseMessage(HttpStatusCode.OK);
        });
        using var services = CreateServices(handler, new TestAdapter());

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location, cancellation.Token));
    }

    [Fact]
    public async Task CancellationWhileReadingBodyPropagatesToCaller()
    {
        using var cancellation = new CancellationTokenSource();
        var adapter = new TestAdapter
        {
            Read = async (_, token) =>
            {
                cancellation.Cancel();
                await Task.Delay(Timeout.InfiniteTimeSpan, token);
                return Forecast;
            }
        };
        using var services = CreateServices(OkHandler(), adapter);

        await Assert.ThrowsAnyAsync<OperationCanceledException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location, cancellation.Token));
    }

    [Theory]
    [InlineData(false)]
    [InlineData(true)]
    public async Task TimeoutCoversSendingAndReading(bool reading)
    {
        var handler = reading ? OkHandler() : new FakeHandler(async (_, token) =>
        {
            await Task.Delay(Timeout.InfiniteTimeSpan, token);
            return new HttpResponseMessage(HttpStatusCode.OK);
        });
        var adapter = new TestAdapter();
        if (reading)
            adapter.Read = async (_, token) =>
            {
                await Task.Delay(Timeout.InfiniteTimeSpan, token);
                return Forecast;
            };
        using var services = CreateServices(handler, adapter,
            settings: new() { ["Weather:TimeoutSeconds"] = "1" });

        var error = await Assert.ThrowsAsync<WeatherServiceException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));

        Assert.Equal(WeatherFailure.Timeout, error.Failure);
    }

    [Theory]
    [InlineData("https://other.test/forecast")]
    [InlineData("//other.test/forecast")]
    [InlineData("http://weather.test/forecast")]
    public async Task AdapterCannotSendCredentialsToAnotherOrigin(string target)
    {
        var handler = OkHandler();
        using var services = CreateServices(handler, new TestAdapter { Target = target });

        var error = await Assert.ThrowsAsync<WeatherServiceException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(Location));

        Assert.Equal(WeatherFailure.NotConfigured, error.Failure);
        Assert.Equal(0, handler.RequestCount);
    }

    [Theory]
    [InlineData("Weather:TimeoutSeconds", "0")]
    [InlineData("Weather:TimeoutSeconds", "121")]
    [InlineData("Weather:BaseAddress", "http://weather.test/")]
    [InlineData("Weather:BaseAddress", "https://user:password@weather.test/")]
    [InlineData("Weather:BaseAddress", "https://weather.test/?key=secret")]
    [InlineData("Weather:ApiKeyHeaderName", "Host")]
    [InlineData("Weather:ApiKeyHeaderName", "bad header")]
    [InlineData("Weather:ApiKey", "bad\r\nvalue")]
    [InlineData("Weather:ApiKey", "")]
    public void InvalidExplicitConfigurationFailsStartupWithoutRevealingValues(string name, string value)
    {
        using var services = CreateServices(OkHandler(), new TestAdapter(),
            settings: new() { [name] = value });

        var error = Assert.Throws<OptionsValidationException>(() =>
            services.GetRequiredService<IStartupValidator>().Validate());

        Assert.DoesNotContain("password", error.Message);
        Assert.DoesNotContain("bad\r\nvalue", error.Message);
    }

    [Theory]
    [InlineData(91, 0)]
    [InlineData(0, -181)]
    [InlineData(double.NaN, 0)]
    public async Task InvalidLocationDoesNotSendRequest(double latitude, double longitude)
    {
        var handler = OkHandler();
        using var services = CreateServices(handler, new TestAdapter());

        await Assert.ThrowsAsync<ArgumentOutOfRangeException>(() =>
            services.GetRequiredService<IWeatherService>().GetForecastAsync(new(latitude, longitude)));

        Assert.Equal(0, handler.RequestCount);
    }

    private static ServiceProvider CreateServices(FakeHandler handler, IWeatherProviderAdapter? adapter,
        bool configured = true, Dictionary<string, string?>? settings = null)
    {
        var values = configured ? new Dictionary<string, string?>
        {
            ["Weather:BaseAddress"] = "https://weather.test/api/",
            ["Weather:ApiKeyHeaderName"] = "X-Test-Key",
            ["Weather:ApiKey"] = "test-only-value"
        } : [];
        foreach (var item in settings ?? [])
            values[item.Key] = item.Value;

        var services = new ServiceCollection();
        if (adapter is not null)
            services.AddSingleton(adapter);
        services.AddSmartFarmWeather(new ConfigurationBuilder().AddInMemoryCollection(values).Build())
            .ConfigurePrimaryHttpMessageHandler(() => handler);
        return services.BuildServiceProvider();
    }

    private static FakeHandler OkHandler() => new((_, _) =>
        Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK)));

    private sealed class FakeHandler(
        Func<HttpRequestMessage, CancellationToken, Task<HttpResponseMessage>> send) : HttpMessageHandler
    {
        public int RequestCount { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken token)
        {
            RequestCount++;
            return send(request, token);
        }
    }

    private sealed class TestAdapter : IWeatherProviderAdapter
    {
        public string Target { get; init; } = "forecast";
        public Func<HttpResponseMessage, CancellationToken, Task<WeatherForecastResult>> Read { get; set; } =
            (_, _) => Task.FromResult(Forecast);

        public HttpRequestMessage CreateForecastRequest(WeatherLocation location) => new(HttpMethod.Get, Target);

        public Task<WeatherForecastResult> ReadForecastAsync(HttpResponseMessage response,
            CancellationToken cancellationToken) => Read(response, cancellationToken);
    }
}
