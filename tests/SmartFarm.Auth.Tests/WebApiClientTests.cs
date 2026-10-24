using System.Net;
using System.Net.Http.Headers;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Web.Clients;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class WebApiClientTests
{
    [Fact]
    public void MvcUsesSharedInfrastructureWithoutRequiringApiHost()
    {
        using var app = new WebApplicationFactory<Program>();
        using var scope = app.Services.CreateScope();
        var services = scope.ServiceProvider;

        Assert.IsType<SmartFarm.Infrastructure.Services.Farms.FarmManagementService>(
            services.GetRequiredService<IFarmManagementService>());
        Assert.IsType<SmartFarm.Infrastructure.Services.Crops.CropManagementService>(
            services.GetRequiredService<ICropManagementService>());
        Assert.IsType<SmartFarm.Infrastructure.Services.Inventory.InventoryService>(
            services.GetRequiredService<IInventoryService>());
        Assert.IsType<SmartFarm.Infrastructure.Services.Monitoring.MonitoringService>(
            services.GetRequiredService<IMonitoringService>());
        Assert.IsType<SmartFarm.Infrastructure.Services.Harvests.HarvestService>(
            services.GetRequiredService<IHarvestService>());
        Assert.IsType<SmartFarm.Infrastructure.Services.Reports.FarmReportService>(
            services.GetRequiredService<SmartFarm.Core.Interfaces.Reports.IFarmReportService>());
        Assert.IsType<SmartFarm.Infrastructure.Services.Tasks.FarmTaskService>(
            services.GetRequiredService<SmartFarm.Core.Interfaces.Tasks.IFarmTaskService>());
        Assert.IsType<SmartFarm.Infrastructure.Security.FarmAccessService>(
            services.GetRequiredService<IFarmAccessService>());
        Assert.IsType<SmartFarm.Infrastructure.Services.Monitoring.LocalObservationPhotoStore>(
            services.GetRequiredService<IObservationPhotoStore>());
        Assert.Null(services.GetService<SmartFarmApiClient>());
    }

    [Fact]
    public async Task ApiCallsCarryShortLivedBearerTokenForCurrentUser()
    {
        using var app = new WebApplicationFactory<Program>();
        var userId = Guid.NewGuid();
        var identity = new ClaimsIdentity(
            [new Claim(ClaimTypes.NameIdentifier, userId.ToString())],
            IdentityConstants.ApplicationScheme);
        var context = new DefaultHttpContext { User = new ClaimsPrincipal(identity) };
        var accessor = new HttpContextAccessor { HttpContext = context };
        var handler = new RecordingHandler();
        using var client = new HttpClient(handler) { BaseAddress = new Uri("https://api.example.test/") };
        var options = app.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>();
        var gateway = new SmartFarmApiClient(client, accessor, options);

        var farms = await gateway.ReadAsync<List<FarmSummary>>("api/farms", userId, default);

        Assert.Empty(farms);
        Assert.Equal(1, handler.RequestCount);
        Assert.Equal("https://api.example.test/api/farms", handler.LastUrl);
        Assert.Equal("Bearer", handler.LastAuthorization?.Scheme);
        Assert.NotNull(handler.LastAuthorization?.Parameter);
        var ticket = options.Get(IdentityConstants.BearerScheme)
            .BearerTokenProtector.Unprotect(handler.LastAuthorization!.Parameter!);
        Assert.NotNull(ticket);
        Assert.Equal(userId.ToString(),
            ticket.Principal.FindFirst(ClaimTypes.NameIdentifier)?.Value);
        Assert.True(ticket.Properties.ExpiresUtc > DateTimeOffset.UtcNow);
        Assert.True(ticket.Properties.ExpiresUtc <= DateTimeOffset.UtcNow.AddMinutes(3));
    }

    [Fact]
    public async Task ApiCallsCannotImpersonateAnotherUser()
    {
        using var app = new WebApplicationFactory<Program>();
        var userId = Guid.NewGuid();
        var accessor = new HttpContextAccessor
        {
            HttpContext = new DefaultHttpContext
            {
                User = new ClaimsPrincipal(new ClaimsIdentity(
                    [new Claim(ClaimTypes.NameIdentifier, userId.ToString())],
                    IdentityConstants.ApplicationScheme))
            }
        };
        var handler = new RecordingHandler();
        using var client = new HttpClient(handler) { BaseAddress = new Uri("https://api.example.test/") };
        var gateway = new SmartFarmApiClient(client, accessor,
            app.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>());

        var error = await Assert.ThrowsAsync<FarmOperationException>(() =>
            gateway.ReadAsync<List<FarmSummary>>("api/farms", Guid.NewGuid(), default));
        Assert.Equal(FarmError.NotFound, error.Error);
        Assert.Equal(0, handler.RequestCount);
    }

    [Fact]
    public async Task MissingFarmReturnsNullWithoutShowingPrivateData()
    {
        using var app = new WebApplicationFactory<Program>();
        var userId = Guid.NewGuid();
        using var client = new HttpClient(new RecordingHandler(_ =>
            new HttpResponseMessage(HttpStatusCode.NotFound)))
        {
            BaseAddress = new Uri("https://api.example.test/")
        };
        var gateway = new SmartFarmApiClient(client, UserContext(userId),
            app.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>());
        var service = new FarmApiService(gateway);

        Assert.Null(await service.GetAsync(Guid.NewGuid(), userId));
    }

    [Fact]
    public async Task ConflictsFromApiRemainConflictsInMvc()
    {
        using var app = new WebApplicationFactory<Program>();
        var userId = Guid.NewGuid();
        using var client = new HttpClient(new RecordingHandler(_ =>
            new HttpResponseMessage(HttpStatusCode.Conflict)
            {
                Content = new StringContent(
                    "{\"message\":\"The record changed.\"}",
                    System.Text.Encoding.UTF8, "application/json")
            }))
        {
            BaseAddress = new Uri("https://api.example.test/")
        };
        var gateway = new SmartFarmApiClient(client, UserContext(userId),
            app.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>());

        var error = await Assert.ThrowsAsync<FarmOperationException>(() =>
            gateway.WriteAsync(HttpMethod.Put, "api/farms/" + Guid.NewGuid(),
                userId, new { Name = "A farm" }, default));
        Assert.Equal(FarmError.Conflict, error.Error);
        Assert.Equal("The record changed.", error.Message);
    }

    [Fact]
    public async Task ServerErrorsPreserveTheHttpStatusForDiagnosis()
    {
        using var app = new WebApplicationFactory<Program>();
        var actor = Guid.NewGuid();
        using var client = new HttpClient(new RecordingHandler(_ =>
            new HttpResponseMessage(HttpStatusCode.InternalServerError)))
        {
            BaseAddress = new Uri("https://api.example.test/")
        };
        var api = new SmartFarmApiClient(client, UserContext(actor),
            app.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>());

        var error = await Assert.ThrowsAsync<HttpRequestException>(() =>
            api.ReadAsync<List<FarmSummary>>("api/farms", actor, default));
        Assert.Equal(HttpStatusCode.InternalServerError, error.StatusCode);
        Assert.Contains("HTTP 500", error.Message);
    }

    private static HttpContextAccessor UserContext(Guid userId) => new()
    {
        HttpContext = new DefaultHttpContext
        {
            User = new ClaimsPrincipal(new ClaimsIdentity(
                [new Claim(ClaimTypes.NameIdentifier, userId.ToString())],
                IdentityConstants.ApplicationScheme))
        }
    };

    private sealed class RecordingHandler : HttpMessageHandler
    {
        private readonly Func<HttpRequestMessage, HttpResponseMessage>? _reply;

        public RecordingHandler(Func<HttpRequestMessage, HttpResponseMessage>? reply = null) =>
            _reply = reply;

        public int RequestCount { get; private set; }
        public string? LastUrl { get; private set; }
        public AuthenticationHeaderValue? LastAuthorization { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request, CancellationToken cancellationToken)
        {
            RequestCount++;
            LastUrl = request.RequestUri?.ToString();
            LastAuthorization = request.Headers.Authorization;
            return Task.FromResult(_reply?.Invoke(request) ?? new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent("[]", System.Text.Encoding.UTF8, "application/json")
            });
        }
    }
}
