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
    public async Task RegistrationOffersFarmCodeAndSeparateOwnerSignup()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Account/Register");
        Assert.Contains("name=\"JoinFarm\"", html);
        Assert.Contains("name=\"FarmCode\"", html);
        Assert.Contains("Join an existing farm", html);
        Assert.Contains("Create/manage my own farm", html);
        Assert.Contains("owner approves", html);
        var ids = Regex.Matches(html, "\\bid=\"([^\"]+)\"").Select(match => match.Groups[1].Value).ToList();
        Assert.Equal(ids.Count, ids.Distinct().Count());
    }

    [Fact]
    public async Task JoiningWithoutCodeKeepsInputsAndShowsLinkedFieldError()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Account/Register");
        var token = WebUtility.HtmlDecode(Regex.Match(html,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        using var response = await client.PostAsync("/Account/Register", new FormUrlEncodedContent(new Dictionary<string, string>
        {
            ["FullName"] = "New Farmer", ["Email"] = "newfarmer@example.test",
            ["Password"] = "StrongPassword123", ["ConfirmPassword"] = "StrongPassword123",
            ["JoinFarm"] = "true", ["__RequestVerificationToken"] = token
        }));
        html = await response.Content.ReadAsStringAsync();
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Contains("href=\"#FarmCode\"", html);
        Assert.Contains("newfarmer@example.test", html);
        Assert.DoesNotContain("value=\"StrongPassword123\"", html);
    }

    private static FormUrlEncodedContent Form(string html, params (string Key, string Value)[] values)
    {
        var fields = values.ToDictionary(v => v.Key, v => v.Value);
        fields["__RequestVerificationToken"] = WebUtility.HtmlDecode(Regex.Match(html,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        return new(fields);
    }

    private static WebApplicationFactory<Program> App(string role = "Owner", Func<string, object?[]?, object>? enroll = null) =>
        new WebApplicationFactory<Program>().WithWebHostBuilder(builder => builder.ConfigureTestServices(services =>
        {
            services.AddSingleton<IAuthenticationService, SignedIn>();
            services.AddScoped(_ => Stub<IFarmManagementService>((name, _) => name switch
            {
                "ListAsync" => Task.FromResult<IReadOnlyList<FarmSummary>>([]),
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
