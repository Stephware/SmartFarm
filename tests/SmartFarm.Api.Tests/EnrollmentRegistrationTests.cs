using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using System.Net;
using System.Net.Http.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging.Abstractions;
using SmartFarm.Infrastructure.Identity;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class EnrollmentRegistrationTests
{
    [Fact]
    public async Task ApiRegistrationAcceptsThreeFieldsAndIgnoresOldEnrollmentFields()
    {
        using var users = new Users();
        using var app = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            builder.ConfigureTestServices(services => services.AddScoped<UserManager<SmartFarmUser>>(_ => users)));
        using var client = app.CreateClient();
        using var response = await client.PostAsJsonAsync("/api/auth/register", new
        {
            fullName = "New Farmer", email = "farmer@example.test", password = "StrongPassword123",
            joinFarm = true, farmCode = "invalid"
        });
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        Assert.Single(users.Created);
        Assert.DoesNotContain("approval", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task RegistrationCreatesOnlyAnAccountWithTrimmedNameAndEmail()
    {
        using var users = new Users();
        var result = await new AccountRegistrationService(users).RegisterAsync(
            " New Farmer ", " farmer@example.test ", "StrongPassword123");
        Assert.True(result.Succeeded);
        var created = Assert.Single(users.Created);
        Assert.Equal("New Farmer", created.FullName);
        Assert.Equal("farmer@example.test", created.Email);
        Assert.Equal(created.Email, created.UserName);
        Assert.True(created.LockoutEnabled);
    }

    [Fact]
    public async Task DuplicateIdentityReturnsItsOriginalError()
    {
        using var users = new Users { Failure = IdentityResult.Failed(new IdentityError { Code = "DuplicateEmail" }) };
        var result = await new AccountRegistrationService(users).RegisterAsync("Farmer", "farmer@example.test", "StrongPassword123");
        Assert.False(result.Succeeded);
        Assert.Equal("DuplicateEmail", Assert.Single(result.Errors).Code);
        Assert.Empty(users.Created);
    }

    [Fact]
    public async Task DatabaseFailureReturnsSafeRegistrationError()
    {
        using var users = new Users { DatabaseFailure = true };
        var result = await new AccountRegistrationService(users).RegisterAsync("Farmer", "farmer@example.test", "StrongPassword123");
        Assert.False(result.Succeeded);
        Assert.Equal("Registration", Assert.Single(result.Errors).Code);
        Assert.DoesNotContain("private database details", Assert.Single(result.Errors).Description);
    }

    [Theory]
    [InlineData("x", "farmer@example.test")]
    [InlineData("Farmer", "x")]
    public async Task InvalidLengthsDoNotCreateAccount(string name, string email)
    {
        using var users = new Users();
        Assert.False((await new AccountRegistrationService(users).RegisterAsync(name, email, "StrongPassword123")).Succeeded);
        Assert.Empty(users.Created);
    }

    private sealed class Users() : UserManager<SmartFarmUser>(new Store(),
        Microsoft.Extensions.Options.Options.Create(new IdentityOptions()), new PasswordHasher<SmartFarmUser>(), [], [],
        new UpperInvariantLookupNormalizer(), new IdentityErrorDescriber(), null!, NullLogger<UserManager<SmartFarmUser>>.Instance)
    {
        public List<SmartFarmUser> Created { get; } = [];
        public IdentityResult? Failure { get; init; }
        public bool DatabaseFailure { get; init; }
        public override Task<IdentityResult> CreateAsync(SmartFarmUser user, string password)
        {
            if (DatabaseFailure) throw new DbUpdateException("private database details");
            if (Failure is not null) return Task.FromResult(Failure);
            Created.Add(user);
            return Task.FromResult(IdentityResult.Success);
        }
    }

    private sealed class Store : IUserStore<SmartFarmUser>
    {
        public void Dispose() { }
        public Task<string> GetUserIdAsync(SmartFarmUser user, CancellationToken ct) => Task.FromResult(user.Id.ToString());
        public Task<string?> GetUserNameAsync(SmartFarmUser user, CancellationToken ct) => Task.FromResult(user.UserName);
        public Task SetUserNameAsync(SmartFarmUser user, string? name, CancellationToken ct) => throw new NotImplementedException();
        public Task<string?> GetNormalizedUserNameAsync(SmartFarmUser user, CancellationToken ct) => throw new NotImplementedException();
        public Task SetNormalizedUserNameAsync(SmartFarmUser user, string? name, CancellationToken ct) => throw new NotImplementedException();
        public Task<IdentityResult> CreateAsync(SmartFarmUser user, CancellationToken ct) => throw new NotImplementedException();
        public Task<IdentityResult> UpdateAsync(SmartFarmUser user, CancellationToken ct) => throw new NotImplementedException();
        public Task<IdentityResult> DeleteAsync(SmartFarmUser user, CancellationToken ct) => throw new NotImplementedException();
        public Task<SmartFarmUser?> FindByIdAsync(string userId, CancellationToken ct) => throw new NotImplementedException();
        public Task<SmartFarmUser?> FindByNameAsync(string name, CancellationToken ct) => throw new NotImplementedException();
    }
}
