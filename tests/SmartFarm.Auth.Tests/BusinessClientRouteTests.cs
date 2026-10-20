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
using SmartFarm.Core.Enums;
using SmartFarm.Web.Clients;
using Xunit;

namespace SmartFarm.Auth.Tests;

// Check MVC-to-API paths without needing a running SQL Server.
public sealed class BusinessClientRouteTests : IClassFixture<WebApplicationFactory<Program>>
{
    private static readonly Guid FarmId = Guid.Parse("11111111-1111-1111-1111-111111111111");
    private static readonly Guid PlotId = Guid.Parse("22222222-2222-2222-2222-222222222222");
    private static readonly Guid CycleId = Guid.Parse("33333333-3333-3333-3333-333333333333");
    private static readonly Guid ItemId = Guid.Parse("44444444-4444-4444-4444-444444444444");
    private static readonly Guid TransactionId = Guid.Parse("55555555-5555-5555-5555-555555555555");
    private static readonly Guid HarvestId = Guid.Parse("66666666-6666-6666-6666-666666666666");
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
        var farm = $"api/farms/{FarmId}";
        await VerifyAsync(HttpMethod.Get, "api/farms",
            (api, actor) => new FarmApiService(api).ListAsync(actor));
        await VerifyAsync(HttpMethod.Post, "api/farms",
            (api, actor) => new FarmApiService(api).CreateAsync(actor, new FarmInput()));
        await VerifyAsync(HttpMethod.Put, farm,
            (api, actor) => new FarmApiService(api).UpdateAsync(FarmId, actor, new FarmInput()));
        await VerifyAsync(HttpMethod.Delete, farm,
            (api, actor) => new FarmApiService(api).ArchiveAsync(FarmId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, farm + "/plots",
            (api, actor) => new FarmApiService(api).ListPlotsAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, farm + "/plots",
            (api, actor) => new FarmApiService(api).CreatePlotAsync(FarmId, actor, new PlotInput()));
        await VerifyAsync(HttpMethod.Put, farm + $"/plots/{PlotId}",
            (api, actor) => new FarmApiService(api).UpdatePlotAsync(FarmId, PlotId, actor, new PlotInput()));
        await VerifyAsync(HttpMethod.Delete, farm + $"/plots/{PlotId}",
            (api, actor) => new FarmApiService(api).ArchivePlotAsync(FarmId, PlotId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, farm + "/members",
            (api, actor) => new FarmApiService(api).MembersAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, farm + "/members/invitations",
            (api, actor) => new FarmApiService(api).CreateInvitationAsync(FarmId, actor, new InviteMemberInput()));
        await VerifyAsync(HttpMethod.Post, "api/farm-invitations/accept",
            (api, actor) => new FarmApiService(api).AcceptInvitationAsync(actor, "token"));
        await VerifyAsync(HttpMethod.Put, farm + $"/members/{PlotId}",
            (api, actor) => new FarmApiService(api).ChangeMemberRoleAsync(FarmId, PlotId, actor, 1));
        await VerifyAsync(HttpMethod.Delete, farm + $"/members/{PlotId}",
            (api, actor) => new FarmApiService(api).RemoveMemberAsync(FarmId, PlotId, actor));
    }

    [Fact]
    public async Task CropRequestsUseExpectedRoutes()
    {
        var farm = $"api/farms/{FarmId}";
        await VerifyAsync(HttpMethod.Get, farm + "/crop-types",
            (api, actor) => new CropApiService(api).ListCropTypesAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, farm + "/crop-types",
            (api, actor) => new CropApiService(api).CreateCropTypeAsync(FarmId, actor, new CropTypeInput()));
        await VerifyAsync(HttpMethod.Get, farm + "/crop-cycles",
            (api, actor) => new CropApiService(api).ListCyclesAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, farm + $"/plots/{PlotId}/crop-cycles",
            (api, actor) => new CropApiService(api).PlanAsync(FarmId, PlotId, actor, new CropCyclePlanInput()));
        await VerifyAsync(HttpMethod.Put, farm + $"/crop-cycles/{CycleId}",
            (api, actor) => new CropApiService(api).UpdatePlanAsync(FarmId, CycleId, actor, new CropCyclePlanInput()));
        await VerifyAsync(HttpMethod.Post, farm + $"/crop-cycles/{CycleId}/transitions",
            (api, actor) => new CropApiService(api).TransitionAsync(FarmId, CycleId, actor, new CropCycleTransitionInput()));
    }

    [Fact]
    public async Task MonitoringRequestsUseExpectedRoutes()
    {
        var path = $"api/farms/{FarmId}/crop-cycles/{CycleId}/monitoring";
        await VerifyAsync(HttpMethod.Get, path,
            (api, actor) => new MonitoringApiService(api).GetAsync(FarmId, CycleId, actor));
        await VerifyAsync(HttpMethod.Post, path + "/activities",
            (api, actor) => new MonitoringApiService(api).RecordActivityAsync(FarmId, CycleId, actor, new RecordActivityInput()));
        await VerifyAsync(HttpMethod.Post, path + "/observations",
            (api, actor) => new MonitoringApiService(api).RecordObservationAsync(FarmId, CycleId, actor, new RecordObservationInput()));
        using var photo = new MemoryStream(new byte[] { 1, 2, 3 });
        await VerifyAsync(HttpMethod.Post, path + $"/observations/{PlotId}/photos",
            (api, actor) => new MonitoringApiService(api).AddPhotoAsync(
                FarmId, CycleId, PlotId, actor, photo, photo.Length, "image/png"));
        await VerifyAsync(HttpMethod.Get, path + $"/photos/{PlotId}",
            (api, actor) => new MonitoringApiService(api).OpenPhotoAsync(FarmId, CycleId, PlotId, actor));
    }

    [Fact]
    public async Task InventoryRequestsUseExpectedRoutes()
    {
        var path = $"api/farms/{FarmId}/inventory";
        await VerifyAsync(HttpMethod.Get, path,
            (api, actor) => new InventoryApiService(api).ListAsync(FarmId, actor));
        await VerifyAsync(HttpMethod.Post, path,
            (api, actor) => new InventoryApiService(api).CreateAsync(FarmId, actor, new InventoryItemInput()));
        await VerifyAsync(HttpMethod.Put, path + $"/{ItemId}",
            (api, actor) => new InventoryApiService(api).UpdateAsync(FarmId, ItemId, actor, new InventoryItemInput()));
        await VerifyAsync(HttpMethod.Delete, path + $"/{ItemId}",
            (api, actor) => new InventoryApiService(api).ArchiveAsync(FarmId, ItemId, actor, "version"));
        await VerifyAsync(HttpMethod.Get, path + $"/{ItemId}/transactions",
            (api, actor) => new InventoryApiService(api).HistoryAsync(FarmId, ItemId, actor));
        await VerifyAsync(HttpMethod.Post, path + $"/{ItemId}/transactions",
            (api, actor) => new InventoryApiService(api).PostAsync(FarmId, ItemId, actor, new InventoryMovementInput()));
        await VerifyAsync(HttpMethod.Post, path + $"/{ItemId}/transactions/{TransactionId}/reverse",
            (api, actor) => new InventoryApiService(api).ReverseAsync(FarmId, ItemId, TransactionId, actor, new InventoryReversalInput()));
        await VerifyAsync(HttpMethod.Get, path + "/activities",
            (api, actor) => new InventoryApiService(api).ActivityOptionsAsync(FarmId, actor));
    }

    [Fact]
    public async Task HarvestRequestsUseExpectedRoutes()
    {
        var path = $"api/farms/{FarmId}/crop-cycles/{CycleId}/harvests";
        await VerifyAsync(HttpMethod.Get, path,
            (api, actor) => new HarvestApiService(api).GetAsync(FarmId, CycleId, actor));
        await VerifyAsync(HttpMethod.Post, path,
            (api, actor) => new HarvestApiService(api).RecordAsync(FarmId, CycleId, actor, new RecordHarvestInput()));
        await VerifyAsync(HttpMethod.Post, path + $"/{HarvestId}/void",
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
