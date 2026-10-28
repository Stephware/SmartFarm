using System.Net;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using System.Reflection;
using System.Security.Claims;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FarmEnrollmentWebTests
{
    private static readonly Guid Actor = Guid.NewGuid(), FarmId = Guid.NewGuid(), RequestId = Guid.NewGuid();
    private static readonly FarmJoinRequestDetails Pending = new(RequestId, FarmId, "Green Farm", Guid.NewGuid(),
        "New Farmer", "applicant@example.test", "Pending", DateTime.UtcNow, null);

    [Fact]
    public async Task MyFarmsShowsPendingStatusAndAcceptsCodeWithAntiforgery()
    {
        string? submitted = null;
        using var app = App(enroll: (method, args) => method switch
        {
            "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
            "RequestAsync" => CaptureCode(args),
            _ => throw new NotSupportedException(method)
        });
        Task<FarmJoinRequestDetails> CaptureCode(object?[]? args) { submitted = args![1]?.ToString(); return Task.FromResult(Pending); }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        Assert.Contains("Pending", html);
        Assert.Contains("Green Farm", html);
        Assert.Contains("owner approves", html);
        Assert.DoesNotContain("Invitation token", html);
        using var missingToken = await client.PostAsync("/Farms/RequestJoin", new FormUrlEncodedContent(new Dictionary<string,string> { ["FarmCode"] = "ABCD-1234-5678" }));
        Assert.Equal(HttpStatusCode.BadRequest, missingToken.StatusCode);
        using var posted = await client.PostAsync("/Farms/RequestJoin", Form(html, ("FarmCode", "ABCD-1234-5678")));
        Assert.Equal(HttpStatusCode.Redirect, posted.StatusCode);
        Assert.Equal("ABCD-1234-5678", submitted);
    }

    [Fact]
    public async Task OwnerDashboardShowsNamedDecisionsButWorkerDashboardDoesNotExposeApplicants()
    {
        foreach (var role in new[] { "Owner", "Manager", "FieldWorker" })
        {
            using var app = App(role);
            using var client = app.CreateClient();
            var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
            if (role == "Owner")
            {
                Assert.Contains("ABCD12345678", html);
                Assert.Contains("applicant@example.test", html);
                Assert.Contains("Approve New Farmer", html);
                Assert.Contains("Reject New Farmer", html);
            }
            else
            {
                Assert.DoesNotContain("ABCD12345678", html);
                Assert.DoesNotContain("applicant@example.test", html);
            }
        }
    }

    [Fact]
    public async Task DecisionUsesRouteRequestAndOwnerActorAndRequiresAntiforgery()
    {
        object?[]? decision = null;
        using var app = App(enroll: (name, args) => name switch
        {
            "OwnerOverviewAsync" => Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending])),
            "DecideAsync" => Capture(args),
            _ => throw new NotSupportedException(name)
        });
        Task Capture(object?[]? args) { decision = args; return Task.CompletedTask; }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
        var path = $"/Farms/DecideJoin?id={FarmId}&requestId={RequestId}&dashboard=true";
        using var missing = await client.PostAsync(path, new FormUrlEncodedContent(new Dictionary<string,string> { ["approve"] = "true" }));
        Assert.Equal(HttpStatusCode.BadRequest, missing.StatusCode);
        using var missingDecision = await client.PostAsync(path, Form(html));
        Assert.Equal(HttpStatusCode.BadRequest, missingDecision.StatusCode);
        Assert.Null(decision);
        using var response = await client.PostAsync(path, Form(html, ("approve", "true"), ("UserId", Guid.NewGuid().ToString()), ("Role", "Owner")));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal(FarmId, decision![0]); Assert.Equal(RequestId, decision[1]);
        Assert.Equal(Actor, decision[2]); Assert.Equal(true, decision[3]);
    }

    [Fact]
    public async Task RetiredInvitationLinksCannotGrantMembership()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync("/Farms/AcceptInvitation?token=old-link");
        Assert.Equal(HttpStatusCode.Gone, response.StatusCode);
        Assert.Contains("farm code", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task RegistrationAsksOnlyForNameEmailAndPassword()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Account/Register");
        foreach (var field in new[] { "FullName", "Email", "Password" })
            Assert.Contains($"name=\"{field}\"", html);
        foreach (var field in new[] { "JoinFarm", "FarmCode", "ConfirmPassword" })
            Assert.DoesNotContain($"name=\"{field}\"", html);
        var ids = Regex.Matches(html, "\\bid=\"([^\"]+)\"").Select(match => match.Groups[1].Value).ToList();
        Assert.Equal(ids.Count, ids.Distinct().Count());
    }

    [Fact]
    public async Task InvalidRegistrationKeepsEmailAndLinksItsErrorWithoutEchoingPassword()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Account/Register");
        using var response = await client.PostAsync("/Account/Register", Form(html,
            ("FullName", "New Farmer"), ("Email", "not-an-email"), ("Password", "StrongPassword123")));
        html = await response.Content.ReadAsStringAsync();
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Contains("href=\"#Email\"", html);
        Assert.Contains("not-an-email", html);
        Assert.DoesNotContain("value=\"StrongPassword123\"", html);
    }

    [Fact]
    public async Task SignedInLoginGoesToMyFarms()
    {
        using var app = App();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync("/Account/Login");
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/Farms", response.Headers.Location?.OriginalString);
    }

    [Fact]
    public async Task PendingRequestCanBeCanceledWithAntiforgeryAndAuthenticatedActor()
    {
        object?[]? canceled = null;
        using var app = App(enroll: (name, args) => name switch
        {
            "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
            "CancelAsync" => Capture(args),
            _ => throw new NotSupportedException(name)
        });
        Task Capture(object?[]? args) { canceled = args; return Task.CompletedTask; }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        Assert.Contains("Cancel request", html);
        var path = $"/Farms/CancelJoin?farmId={FarmId}&requestId={RequestId}";
        using var missing = await client.PostAsync(path, new FormUrlEncodedContent([]));
        Assert.Equal(HttpStatusCode.BadRequest, missing.StatusCode);
        Assert.Null(canceled);
        using var response = await client.PostAsync(path, Form(html, ("actor", Guid.NewGuid().ToString())));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/Farms", response.Headers.Location?.OriginalString);
        Assert.Equal(FarmId, canceled![0]); Assert.Equal(RequestId, canceled[1]); Assert.Equal(Actor, canceled[2]);
    }

    [Fact]
    public async Task OwnerCanRegenerateCodeWithAntiforgeryAndCurrentCode()
    {
        object?[]? regenerated = null;
        using var app = App(enroll: (name, args) => name switch
        {
            "OwnerOverviewAsync" => Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending])),
            "RegenerateCodeAsync" => Capture(args),
            _ => throw new NotSupportedException(name)
        });
        Task<string> Capture(object?[]? args) { regenerated = args; return Task.FromResult("NEWC12345678"); }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        Assert.Contains("Copy code", html);
        Assert.Contains("Regenerate code", html);
        var path = $"/Farms/RegenerateCode?id={FarmId}";
        using var missing = await client.PostAsync(path, new FormUrlEncodedContent(new Dictionary<string,string> { ["currentCode"] = "ABCD12345678" }));
        Assert.Equal(HttpStatusCode.BadRequest, missing.StatusCode);
        Assert.Null(regenerated);
        using var response = await client.PostAsync(path, Form(html, ("currentCode", "ABCD12345678")));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal(FarmId, regenerated![0]); Assert.Equal(Actor, regenerated[1]); Assert.Equal("ABCD12345678", regenerated[2]);
    }

    [Fact]
    public async Task StaleCodeRegenerationShowsConflictAndCurrentOwnerPanel()
    {
        using var app = App(enroll: (name, _) => name switch
        {
            "OwnerOverviewAsync" => Task.FromResult(new FarmEnrollmentOverview("NEWC12345678", [])),
            "RegenerateCodeAsync" => throw new FarmOperationException(FarmError.Conflict, "Farm code changed. Reload before trying again."),
            _ => throw new NotSupportedException(name)
        });
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        using var response = await client.PostAsync($"/Farms/RegenerateCode?id={FarmId}", Form(html, ("currentCode", "ABCD12345678")));
        Assert.Equal(HttpStatusCode.Conflict, response.StatusCode);
        html = await response.Content.ReadAsStringAsync();
        Assert.Contains("NEWC12345678", html);
        Assert.Contains("Farm code changed", html);
    }

    [Fact]
    public async Task MyFarmsShowsMultipleRolesAndTerminalRequestHistoryWithoutCancelControls()
    {
        var requests = new[] { Pending with { Status = "Approved" }, Pending with { Id = Guid.NewGuid(), Status = "Rejected" }, Pending with { Id = Guid.NewGuid(), Status = "Canceled" } };
        using var app = App(farms: [new(FarmId, "Green Farm", "Location", "Owner", "Asia/Manila"),
                new(Guid.NewGuid(), "Second Farm", "Location", "Manager", "Asia/Manila")],
            enroll: (name, _) => name == "MyRequestsAsync"
                ? Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>(requests) : throw new NotSupportedException(name));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Farms");
        Assert.Contains("Create farm", html); Assert.Contains("Join farm", html);
        Assert.Contains("Owner", html); Assert.Contains("Manager", html); Assert.Contains("Second Farm", html);
        foreach (var status in new[] { "Approved", "Rejected", "Canceled" }) Assert.Contains(status, html);
        Assert.DoesNotContain("Cancel request", html);
        Assert.Contains("valid farm code", html);
    }

    [Fact]
    public async Task OwnerPanelSeparatesPendingDecisionsFromTerminalHistory()
    {
        using var app = App(enroll: (name, _) => name == "OwnerOverviewAsync"
            ? Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending],
                [Pending with { Id = Guid.NewGuid(), FullName = "Former Applicant", Status = "Canceled" }]))
            : throw new NotSupportedException(name));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        Assert.Contains("Approve New Farmer", html);
        Assert.Contains("Former Applicant", html); Assert.Contains("Canceled", html);
        Assert.DoesNotContain("Approve Former Applicant", html);
    }

    [Theory]
    [InlineData("/Farms/CancelJoin", "CancelAsync")]
    [InlineData("/Farms/RegenerateCode", "RegenerateCodeAsync")]
    public async Task EnrollmentDenialsReturnNotFound(string action, string operation)
    {
        using var app = App(enroll: (name, _) => name == operation
            ? throw new FarmOperationException(FarmError.NotFound, "Not found")
            : name == "MyRequestsAsync" ? Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending])
            : throw new NotSupportedException(name));
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        using var response = await client.PostAsync($"{action}?id={FarmId}&farmId={FarmId}&requestId={RequestId}",
            Form(html, ("currentCode", "ABCD12345678")));
        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task RegistrationRejectsMissingAntiforgery()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        using var response = await client.PostAsync("/Account/Register", new FormUrlEncodedContent(new Dictionary<string,string>
            { ["FullName"] = "Test Farmer", ["Email"] = "farmer@example.test", ["Password"] = "StrongPassword123" }));
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task OwnerDecisionsShareEnrollmentWriteBudgetAndReturnRetryAfter()
    {
        var decisions = 0;
        using var app = App(enroll: (name, _) => name switch
        {
            "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
            "DecideAsync" => Decide(),
            _ => throw new NotSupportedException(name)
        });
        Task Decide() { decisions++; return Task.CompletedTask; }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        for (var attempt = 0; attempt < 9; attempt++)
        {
            using var response = await client.PostAsync("/Farms/RequestJoin", Form(html));
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        }
        var path = $"/Farms/DecideJoin?id={FarmId}&requestId={RequestId}";
        using (var allowed = await client.PostAsync(path, Form(html, ("approve", "true"))))
            Assert.Equal(HttpStatusCode.Redirect, allowed.StatusCode);
        using var rejected = await client.PostAsync("/Farms/RequestJoin", Form(html));
        Assert.Equal(HttpStatusCode.TooManyRequests, rejected.StatusCode);
        Assert.True(rejected.Headers.RetryAfter?.Delta > TimeSpan.Zero);
        Assert.Contains("Too many attempts", await rejected.Content.ReadAsStringAsync());
        foreach (var action in new[] { path, $"/Farms/CancelJoin?farmId={FarmId}&requestId={RequestId}", $"/Farms/RegenerateCode?id={FarmId}" })
        {
            using var response = await client.PostAsync(action, Form(html, ("approve", "true"), ("CurrentCode", "ABCD12345678")));
            Assert.Equal(HttpStatusCode.TooManyRequests, response.StatusCode);
            Assert.True(response.Headers.RetryAfter?.Delta > TimeSpan.Zero);
        }
        Assert.Equal(1, decisions);
        using var read = await client.GetAsync("/Farms");
        Assert.Equal(HttpStatusCode.OK, read.StatusCode);
    }

    private static FormUrlEncodedContent Form(string html, params (string Key, string Value)[] values)
    {
        var fields = values.ToDictionary(v => v.Key, v => v.Value);
        fields["__RequestVerificationToken"] = WebUtility.HtmlDecode(Regex.Match(html,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        return new(fields);
    }

    private static WebApplicationFactory<Program> App(string role = "Owner", Func<string, object?[]?, object>? enroll = null, IReadOnlyList<FarmSummary>? farms = null) =>
        new WebApplicationFactory<Program>().WithWebHostBuilder(builder => builder.ConfigureTestServices(services =>
        {
            services.AddSingleton<IAuthenticationService, SignedIn>();
            services.AddScoped(_ => Stub<IFarmManagementService>((name, _) => name switch
            {
                "ListAsync" => Task.FromResult<IReadOnlyList<FarmSummary>>(farms ?? []),
                "GetAsync" => Task.FromResult<FarmDetails?>(new(FarmId, "Green Farm", "Location", "Asia/Manila", null, null, role, "version")),
                "ListPlotsAsync" => Task.FromResult<IReadOnlyList<PlotDetails>>([]),
                "MembersAsync" => Task.FromResult<IReadOnlyList<FarmMemberDetails>>([]),
                _ => throw new NotSupportedException(name)
            }));
            services.AddScoped(_ => Stub<IFarmReportService>((_, _) => Task.FromResult(
                new FarmDashboard(FarmId, "Green Farm", new DateOnly(2026,10,27), 0,0,0,0,0,0,0,0,[],[]))));
            services.AddScoped(_ => Stub<IFarmEnrollmentService>(enroll ?? ((name, _) => name switch
            {
                "OwnerOverviewAsync" => role == "Owner" ? Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending])) : throw new InvalidOperationException("Non-owner must not request applicants"),
                "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
                _ => throw new NotSupportedException(name)
            })));
        }));

    private static T Stub<T>(Func<string, object?[]?, object> call) where T : class
    {
        var proxy = DispatchProxy.Create<T, Proxy>(); ((Proxy)(object)proxy).Call = call; return proxy;
    }
    public class Proxy : DispatchProxy
    {
        public Func<string, object?[]?, object> Call { get; set; } = null!;
        protected override object? Invoke(MethodInfo? method, object?[]? args) => Call(method!.Name, args);
    }
    private sealed class SignedIn : IAuthenticationService
    {
        public Task<AuthenticateResult> AuthenticateAsync(HttpContext context, string? scheme) => Task.FromResult(AuthenticateResult.Success(new AuthenticationTicket(
            new ClaimsPrincipal(new ClaimsIdentity([new Claim(ClaimTypes.NameIdentifier, Actor.ToString())], IdentityConstants.ApplicationScheme)), IdentityConstants.ApplicationScheme)));
        public Task ChallengeAsync(HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task ForbidAsync(HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task SignInAsync(HttpContext context, string? scheme, ClaimsPrincipal principal, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task SignOutAsync(HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
    }
}
