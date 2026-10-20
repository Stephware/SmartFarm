using System.Net;
using System.Security.Claims;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using SmartFarm.Web.Clients;
using Xunit;

namespace SmartFarm.Auth.Tests;

// Check MVC-to-API paths without needing a running SQL Server.
public sealed class BusinessClientRouteTests : IClassFixture<WebApplicationFactory<Program>>
{
    private static readonly Guid FarmId = Guid.NewGuid();
    private static readonly Guid PlotId = Guid.NewGuid();
    private static readonly Guid MemberId = Guid.NewGuid();
    private static readonly Guid CycleId = Guid.NewGuid();
    private static readonly Guid ObservationId = Guid.NewGuid();
    private static readonly Guid ItemId = Guid.NewGuid();
    private static readonly Guid TransactionId = Guid.NewGuid();
    private static readonly Guid HarvestId = Guid.NewGuid();
    private static readonly Guid TaskId = Guid.NewGuid();

    private static string FarmRoute => $"api/farms/{FarmId}";
    private static string PlotRoute => $"{FarmRoute}/plots/{PlotId}";
    private static string CropRoute => $"{FarmRoute}/crop-cycles/{CycleId}";
    private static string MonitoringRoute => $"{CropRoute}/monitoring";
    private static string InventoryRoute => $"{FarmRoute}/inventory";
    private static string ItemRoute => $"{InventoryRoute}/{ItemId}";
    private static string HarvestRoute => $"{CropRoute}/harvests";

    private readonly WebApplicationFactory<Program> _factory;

    public BusinessClientRouteTests(WebApplicationFactory<Program> factory) => _factory = factory;

    private async Task VerifyAsync(HttpMethod method, string path,
        Func<SmartFarmApiClient, Guid, Task> operation)
    {
        var actor = Guid.NewGuid();
        var identity = new ClaimsIdentity(
            [new Claim(ClaimTypes.NameIdentifier, actor.ToString())],
            IdentityConstants.ApplicationScheme);
        var accessor = new HttpContextAccessor
        {
            HttpContext = new DefaultHttpContext { User = new ClaimsPrincipal(identity) }
        };
        var handler = new RouteHandler();
        using var http = new HttpClient(handler) { BaseAddress = new Uri("https://api.example.test/") };
        var options = _factory.Services.GetRequiredService<IOptionsMonitor<BearerTokenOptions>>();
        var gateway = new SmartFarmApiClient(http, accessor, options);

        var error = await Assert.ThrowsAsync<FarmOperationException>(() => operation(gateway, actor));

        Assert.Equal(FarmError.NotFound, error.Error);
        Assert.Equal(1, handler.RequestCount);
        Assert.Equal(method, handler.Method);
        Assert.Equal("/" + path, handler.Path);
        Assert.Equal("Bearer", handler.AuthorizationScheme);
    }

    [Fact]
    public async Task FarmPlotAndMembershipRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, "api/farms",
            (api, actor) => new FarmApiService(api).ListAsync(actor));
        await VerifyAsync(HttpMethod.Post, "api/farms",
            (api, actor) => new FarmApiService(api).CreateAsync(actor, new FarmInput()));
        await VerifyAsync(HttpMethod.Put, FarmRoute,
            (api, actor) => new FarmApiService(api).UpdateAsync(FarmId, actor, new FarmInput()));
        await VerifyAsync(HttpMethod.Delete, FarmRoute,
            (api, actor) => new FarmApiService(api).ArchiveAsync(FarmId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/plots",
            (api, actor) => new FarmApiService(api).ListPlotsAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, FarmRoute + "/plots",
            (api, actor) => new FarmApiService(api).CreatePlotAsync(FarmId, actor, new PlotInput()));
        await VerifyAsync(HttpMethod.Put, PlotRoute,
            (api, actor) => new FarmApiService(api).UpdatePlotAsync(FarmId, PlotId, actor, new PlotInput()));
        await VerifyAsync(HttpMethod.Delete, PlotRoute,
            (api, actor) => new FarmApiService(api).ArchivePlotAsync(FarmId, PlotId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/members",
            (api, actor) => new FarmApiService(api).MembersAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, FarmRoute + "/members/invitations",
            (api, actor) => new FarmApiService(api).CreateInvitationAsync(FarmId, actor, new InviteMemberInput()));
        await VerifyAsync(HttpMethod.Post, "api/farm-invitations/accept",
            (api, actor) => new FarmApiService(api).AcceptInvitationAsync(actor, "token"));
        await VerifyAsync(HttpMethod.Put, FarmRoute + $"/members/{MemberId}",
            (api, actor) => new FarmApiService(api).ChangeMemberRoleAsync(FarmId, MemberId, actor, 1));
        await VerifyAsync(HttpMethod.Delete, FarmRoute + $"/members/{MemberId}",
            (api, actor) => new FarmApiService(api).RemoveMemberAsync(FarmId, MemberId, actor));
    }

    [Fact]
    public async Task CropRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/crop-types",
            (api, actor) => new CropApiService(api).ListCropTypesAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, FarmRoute + "/crop-types",
            (api, actor) => new CropApiService(api).CreateCropTypeAsync(FarmId, actor, new CropTypeInput()));
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/crop-cycles",
            (api, actor) => new CropApiService(api).ListCyclesAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, PlotRoute + "/crop-cycles",
            (api, actor) => new CropApiService(api).PlanAsync(FarmId, PlotId, actor, new CropCyclePlanInput()));
        await VerifyAsync(HttpMethod.Put, CropRoute,
            (api, actor) => new CropApiService(api).UpdatePlanAsync(FarmId, CycleId, actor, new CropCyclePlanInput()));
        await VerifyAsync(HttpMethod.Post, CropRoute + "/transitions",
            (api, actor) => new CropApiService(api).TransitionAsync(FarmId, CycleId, actor, new CropCycleTransitionInput()));
    }

    [Fact]
    public async Task MonitoringRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, MonitoringRoute,
            (api, actor) => new MonitoringApiService(api).GetAsync(FarmId, CycleId, actor));
        await VerifyAsync(HttpMethod.Post, MonitoringRoute + "/activities",
            (api, actor) => new MonitoringApiService(api).RecordActivityAsync(FarmId, CycleId, actor, new RecordActivityInput()));
        await VerifyAsync(HttpMethod.Post, MonitoringRoute + "/observations",
            (api, actor) => new MonitoringApiService(api).RecordObservationAsync(FarmId, CycleId, actor, new RecordObservationInput()));
        using var photo = new MemoryStream(new byte[] { 1, 2, 3 });
        await VerifyAsync(HttpMethod.Post, MonitoringRoute + $"/observations/{ObservationId}/photos",
            (api, actor) => new MonitoringApiService(api).AddPhotoAsync(
                FarmId, CycleId, ObservationId, actor, photo, photo.Length, "image/png"));
        await VerifyAsync(HttpMethod.Get, MonitoringRoute + $"/photos/{ObservationId}",
            (api, actor) => new MonitoringApiService(api).OpenPhotoAsync(FarmId, CycleId, ObservationId, actor));
    }

    [Fact]
    public async Task InventoryRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, InventoryRoute,
            (api, actor) => new InventoryApiService(api).ListAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, InventoryRoute,
            (api, actor) => new InventoryApiService(api).CreateAsync(FarmId, actor, new InventoryItemInput()));
        await VerifyAsync(HttpMethod.Put, ItemRoute,
            (api, actor) => new InventoryApiService(api).UpdateAsync(FarmId, ItemId, actor, new InventoryItemInput()));
        await VerifyAsync(HttpMethod.Delete, ItemRoute,
            (api, actor) => new InventoryApiService(api).ArchiveAsync(FarmId, ItemId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, ItemRoute + "/transactions",
            (api, actor) => new InventoryApiService(api).HistoryAsync(FarmId, ItemId, actor));
        await VerifyAsync(HttpMethod.Post, ItemRoute + "/transactions",
            (api, actor) => new InventoryApiService(api).PostAsync(FarmId, ItemId, actor, new InventoryMovementInput()));
        await VerifyAsync(HttpMethod.Post, ItemRoute + $"/transactions/{TransactionId}/reverse",
            (api, actor) => new InventoryApiService(api).ReverseAsync(FarmId, ItemId, TransactionId, actor, new InventoryReversalInput()));
        await VerifyAsync(HttpMethod.Get, InventoryRoute + "/activities",
            (api, actor) => new InventoryApiService(api).ActivityOptionsAsync(FarmId, actor));
    }

    [Fact]
    public async Task DashboardAndHarvestReportUseTheProtectedApiRoutes()
    {
        await VerifyAsync(HttpMethod.Get, FarmRoute + "/dashboard",
            (api, actor) => new FarmReportApiService(api).DashboardAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Get,
            FarmRoute + "/reports/harvests?from=2026-10-01&to=2026-10-20" +
            $"&plotId={PlotId}&cropTypeId={CycleId}",
            (api, actor) => new FarmReportApiService(api).HarvestsAsync(
                FarmId, actor, new DateOnly(2026, 10, 1),
                new DateOnly(2026, 10, 20), PlotId, CycleId));
    }

    [Fact]
    public async Task TaskEditingUsesTheFarmApiRoute()
    {
        await VerifyAsync(HttpMethod.Put, FarmRoute + $"/tasks/{TaskId}",
            (api, actor) => new FarmTaskApiService(api).UpdateAsync(
                FarmId, TaskId, actor, new FarmTaskUpdateInput(
                    new DateOnly(2026, 10, 20), "Water plants", null, "version")));
    }

    [Fact]
    public async Task HarvestRequestsUseExpectedRoutes()
    {
        await VerifyAsync(HttpMethod.Get, HarvestRoute,
            (api, actor) => new HarvestApiService(api).GetAsync(FarmId, CycleId, actor));
        await VerifyAsync(HttpMethod.Post, HarvestRoute,
            (api, actor) => new HarvestApiService(api).RecordAsync(FarmId, CycleId, actor, new RecordHarvestInput()));
        await VerifyAsync(HttpMethod.Post, HarvestRoute + $"/{HarvestId}/void",
            (api, actor) => new HarvestApiService(api).VoidAsync(FarmId, CycleId, HarvestId, actor, new VoidHarvestInput()));
    }

    private sealed class RouteHandler : HttpMessageHandler
    {
        public int RequestCount { get; private set; }
        public HttpMethod? Method { get; private set; }
        public string? Path { get; private set; }
        public string? AuthorizationScheme { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request, CancellationToken cancellationToken)
        {
            RequestCount++;
            Method = request.Method;
            Path = request.RequestUri?.AbsolutePath;
            AuthorizationScheme = request.Headers.Authorization?.Scheme;
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.NotFound));
        }
    }
}
