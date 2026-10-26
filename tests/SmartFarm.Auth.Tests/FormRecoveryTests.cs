using System.Net;
using System.Reflection;
using System.Security.Claims;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Controllers;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Http;
using System.Data.Common;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FormRecoveryTests
{
    private static readonly Guid FarmId = Guid.NewGuid(), CycleId = Guid.NewGuid();
    private static readonly FarmDetails Farm = new(FarmId, "Test farm", "Test location", "Asia/Manila", null, null, "Owner", "version");
    private static readonly CropCycleDetails Cycle = new(CycleId, FarmId, Guid.NewGuid(), "North plot",
        Guid.NewGuid(), "Rice", CropCycleStatus.Planned, new DateOnly(2026, 10, 26), null, null,
        "kg", null, null, null, "version");

    [Theory]
    [InlineData("Crops/CreateCropType", "CropType.Name", "My new crop")]
    [InlineData("Crops/Plan", "Plan.HarvestUnit", "baskets")]
    [InlineData("Crops/Transition", "Transition.TerminationReason", "Flood damage")]
    [InlineData("Crops/EditPlan", "Input.HarvestUnit", "baskets")]
    [InlineData("Monitoring/Activity", "Activity.Notes", "Watered the north field")]
    [InlineData("Monitoring/Observation", "Observation.Notes", "Leaves need attention")]
    public async Task FailedSubmissionRendersItsDraftAndAccessibleErrors(string action, string key, string value)
    {
        using var app = CreateApp();
        using var client = app.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
        var query = $"?farmId={FarmId}&cycleId={CycleId}&plotId={Cycle.PlotId}";
        var getPath = action.StartsWith("Monitoring") ? "/Monitoring" :
            action.EndsWith("EditPlan") ? "/Crops/EditPlan" : "/Crops";
        var page = await client.GetStringAsync(getPath + query);
        var token = WebUtility.HtmlDecode(Regex.Match(page,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        Assert.NotEmpty(token);
        using var response = await client.PostAsync("/" + action + query,
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                [key] = value, ["__RequestVerificationToken"] = token,
                ["Transition.Status"] = "Cancelled", ["Transition.RowVersion"] = "version"
            }));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains(value, html);
        Assert.Contains("data-error-summary", html);
        Assert.Contains("tabindex=\"-1\"", html);
    }

    [Fact]
    public async Task InvalidDateKeepsTheAttemptedValueAndLinksItsError()
    {
        using var app = CreateApp();
        using var client = app.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
        var page = await client.GetStringAsync($"/Crops?farmId={FarmId}");
        var token = WebUtility.HtmlDecode(Regex.Match(page,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        using var response = await client.PostAsync($"/Crops/Plan?farmId={FarmId}",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["Plan.PlannedPlantingDate"] = "not-a-date", ["__RequestVerificationToken"] = token
            }));
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("href=\"#Plan_PlannedPlantingDate\"", html);
        Assert.Contains("not-a-date", html);
    }

    [Fact]
    public async Task NewPlantingDateStartsBlank()
    {
        using var app = CreateApp();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Crops?farmId={FarmId}");
        var field = Regex.Match(html, "<input[^>]*name=\"Plan.PlannedPlantingDate\"[^>]*>").Value;
        Assert.NotEmpty(field);
        Assert.DoesNotContain("0001-01-01", field);
    }

    [Fact]
    public async Task FailedTransitionOnlyRestoresTheSubmittedAction()
    {
        using var app = CreateApp(CropCycleStatus.Active);
        using var client = app.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Crops?farmId={FarmId}");
        var token = WebUtility.HtmlDecode(Regex.Match(html,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        using var response = await client.PostAsync($"/Crops/Transition?farmId={FarmId}&cycleId={CycleId}",
            new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["Transition.Status"] = "Failed", ["Transition.RowVersion"] = "version",
                ["Transition.TerminationReason"] = "Flood damage", ["__RequestVerificationToken"] = token
            }));
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        html = await response.Content.ReadAsStringAsync();
        Assert.Contains("Flood damage", Regex.Match(html, "<input[^>]*id=\"Transition_FailureReason_[^\"]+\"[^>]*>").Value);
        Assert.DoesNotContain("Flood damage", Regex.Match(html, "<input[^>]*id=\"Transition_TerminationReason_[^\"]+\"[^>]*>").Value);
    }

    [Fact]
    public async Task ReadOutageOffersTheSameLocalPageWithoutTechnicalDetails()
    {
        using var app = CreateApp(unavailable: true);
        using var client = app.CreateClient();
        var path = $"/Farms/Details/{FarmId}?section=plots";
        using var response = await client.GetAsync(path);
        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains($"href=\"{path}\"", html);
        Assert.DoesNotContain("private connection details", html);
    }

    [Fact]
    public async Task UnconfirmedWriteDoesNotOfferBlindRetry()
    {
        using var app = CreateApp(unavailable: true);
        using var client = app.CreateClient();
        var path = $"?farmId={FarmId}&cycleId={CycleId}";
        var page = await client.GetStringAsync("/Monitoring" + path);
        var token = WebUtility.HtmlDecode(Regex.Match(page,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        using var response = await client.PostAsync("/Monitoring/Activity" + path,
            new FormUrlEncodedContent(new Dictionary<string, string>
            { ["Activity.ActivityType"] = "1", ["__RequestVerificationToken"] = token }));
        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("before submitting it again", html);
        Assert.DoesNotContain(">Try again<", html);
        Assert.DoesNotContain("private connection details", html);
    }

    private sealed class OfflineDatabase : DbException
    {
        public OfflineDatabase() : base("private connection details") { }
    }

    private static WebApplicationFactory<Program> CreateApp(CropCycleStatus status = CropCycleStatus.Planned, bool unavailable = false) => new WebApplicationFactory<Program>()
        .WithWebHostBuilder(builder => builder.ConfigureTestServices(services =>
        {
            services.AddSingleton<IAuthenticationService, SignedIn>();
            services.AddScoped(_ => Stub<IFarmManagementService>(method => method switch
            {
                "GetAsync" => unavailable ? throw new OfflineDatabase() : Task.FromResult<FarmDetails?>(Farm),
                "ListPlotsAsync" => Task.FromResult<IReadOnlyList<PlotDetails>>([new(Cycle.PlotId, FarmId, "North plot", 100, "version")]),
                _ => throw new NotSupportedException(method)
            }));
            services.AddScoped(_ => Stub<ICropManagementService>(method => method switch
            {
                "GetCycleAsync" => Task.FromResult<CropCycleDetails?>(Cycle with { Status = status }),
                "ListCyclesAsync" => Task.FromResult<IReadOnlyList<CropCycleDetails>>([Cycle with { Status = status }]),
                "ListCropTypesAsync" => Task.FromResult<IReadOnlyList<CropTypeDetails>>([new(Cycle.CropTypeId, "Rice", null, null, "kg")]),
                _ => throw new FarmOperationException(FarmError.InvalidInput, "Check the submitted values.")
            }));
            services.AddScoped(_ => Stub<IMonitoringService>(method => method == "GetAsync"
                ? Task.FromResult(new MonitoringDetails(FarmId, CycleId, true, [], []))
                : throw (unavailable ? new OfflineDatabase() : new FarmOperationException(FarmError.InvalidInput, "Check the submitted values."))));
            services.AddScoped(_ => Stub<IFarmTaskService>(method => method == "ListAsync"
                ? Task.FromResult<IReadOnlyList<FarmTaskDetails>>([]) : throw new NotSupportedException(method)));
        }));

    [Theory]
    [InlineData(StockTransactionType.Consumption, "2.5", "-2.5")]
    [InlineData(StockTransactionType.Receipt, "2.5", "2.5")]
    [InlineData(StockTransactionType.Adjustment, "-2.5", "-2.5")]
    public async Task WebStockEntryConvertsOnlyPositiveConsumption(StockTransactionType type, string amount, string expected)
    {
        using var app = new WebApplicationFactory<Program>();
        using var scope = app.Services.CreateScope();
        var inventory = Stub<IInventoryService>(_ => Task.FromResult<InventoryItemDetails>(null!));
        var controller = new InventoryController(scope.ServiceProvider.GetRequiredService<UserManager<SmartFarmUser>>(), null!, inventory)
        { ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() } };
        var input = new InventoryMovementInput { TransactionType = type,
            QuantityChange = decimal.Parse(amount, System.Globalization.CultureInfo.InvariantCulture), RowVersion = "version" };
        Assert.IsType<RedirectToActionResult>(await controller.Post(FarmId, Guid.NewGuid(), input, default));
        Assert.Equal(decimal.Parse(expected, System.Globalization.CultureInfo.InvariantCulture), input.QuantityChange);
    }

    private static T Stub<T>(Func<string, object> invoke) where T : class
    {
        var proxy = DispatchProxy.Create<T, ServiceStub>();
        ((ServiceStub)(object)proxy).Call = invoke;
        return proxy;
    }

    public class ServiceStub : DispatchProxy
    {
        public Func<string, object> Call { get; set; } = null!;
        protected override object? Invoke(MethodInfo? method, object?[]? args) => Call(method!.Name);
    }

    private sealed class SignedIn : IAuthenticationService
    {
        public Task<AuthenticateResult> AuthenticateAsync(Microsoft.AspNetCore.Http.HttpContext context, string? scheme) =>
            Task.FromResult(AuthenticateResult.Success(new AuthenticationTicket(new ClaimsPrincipal(
                new ClaimsIdentity([new Claim(ClaimTypes.NameIdentifier, "11111111-1111-1111-1111-111111111111")],
                    IdentityConstants.ApplicationScheme)), IdentityConstants.ApplicationScheme)));
        public Task ChallengeAsync(Microsoft.AspNetCore.Http.HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task ForbidAsync(Microsoft.AspNetCore.Http.HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task SignInAsync(Microsoft.AspNetCore.Http.HttpContext context, string? scheme, ClaimsPrincipal principal, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task SignOutAsync(Microsoft.AspNetCore.Http.HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
    }
}
