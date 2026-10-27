using System.Data;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Infrastructure.Identity;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class EnrollmentRegistrationTests
{
    [Fact]
    public async Task JoiningRegistrationCommitsAccountAndRequestTogether()
    {
        using var state = new State();
        var result = await state.Registration.RegisterAsync(" New Farmer ", " farmer@example.test ", "password", true, "ABCD12345678");
        Assert.True(result.Succeeded);
        Assert.True(state.Committed);
        Assert.Equal(Assert.Single(state.CreatedUsers).Id, state.RequestUser);
        Assert.Equal("New Farmer", state.CreatedUsers[0].FullName);
    }

    [Fact]
    public async Task InvalidCodeRollsBackNewAccountAndReturnsFarmCodeError()
    {
        using var state = new State { Failure = new FarmOperationException(FarmError.InvalidInput, "Invalid farm code.") };
        var result = await state.Registration.RegisterAsync("New Farmer", "farmer@example.test", "password", true, "invalid");
        Assert.False(result.Succeeded);
        Assert.Contains(result.Errors, error => error.Code == "FarmCode");
        Assert.Empty(state.CreatedUsers);
        Assert.False(state.Committed);
    }

    [Fact]
    public async Task PersistenceFailureRollsBackAccount()
    {
        using var state = new State { Failure = new DbUpdateException("Database unavailable") };
        var result = await state.Registration.RegisterAsync("New Farmer", "farmer@example.test", "password", true, "ABCD12345678");
        Assert.False(result.Succeeded);
        Assert.Empty(state.CreatedUsers);
        Assert.False(state.Committed);
    }

    [Fact]
    public async Task RejectedIdentityNeverCreatesRequest()
    {
        using var state = new State { IdentityFailure = true };
        var result = await state.Registration.RegisterAsync("New Farmer", "farmer@example.test", "password", true, "ABCD12345678");
        Assert.False(result.Succeeded);
        Assert.Null(state.RequestUser);
        Assert.Empty(state.CreatedUsers);
    }

    [Fact]
    public async Task OwnerRegistrationKeepsOriginalPath()
    {
        using var state = new State();
        Assert.True((await state.Registration.RegisterAsync("New Owner", "owner@example.test", "password")).Succeeded);
        Assert.Single(state.CreatedUsers);
        Assert.Null(state.RequestUser);
        Assert.False(state.Started);
    }

    private sealed class State : UserManager<SmartFarmUser>, IFarmEnrollmentService, IUnitOfWork, IUnitOfWorkTransaction
    {
        public List<SmartFarmUser> CreatedUsers { get; } = [];
        public Exception? Failure { get; init; }
        public bool IdentityFailure { get; init; }
        public bool Committed { get; private set; }
        public bool Started { get; private set; }
        public Guid? RequestUser { get; private set; }
        public AccountRegistrationService Registration => new(this, this, this);
        public State() : base(new Store(), Microsoft.Extensions.Options.Options.Create(new IdentityOptions()), new PasswordHasher<SmartFarmUser>(), [], [],
            new UpperInvariantLookupNormalizer(), new IdentityErrorDescriber(), null!, NullLogger<UserManager<SmartFarmUser>>.Instance) { }
        public override Task<IdentityResult> CreateAsync(SmartFarmUser user, string password)
        {
            if (IdentityFailure) return Task.FromResult(IdentityResult.Failed(new IdentityError { Code = "DuplicateEmail" }));
            CreatedUsers.Add(user);
            return Task.FromResult(IdentityResult.Success);
        }
        public Task<FarmJoinRequestDetails> RequestAsync(Guid actor, string code, CancellationToken ct = default)
        {
            if (Failure is not null) throw Failure;
            Assert.True(HasActiveTransaction);
            RequestUser = actor;
            return Task.FromResult(new FarmJoinRequestDetails(Guid.NewGuid(), Guid.NewGuid(), "Farm", actor, "Farmer", "farmer@example.test", "Pending", DateTime.UtcNow, null));
        }
        public Task<IReadOnlyList<FarmJoinRequestDetails>> MyRequestsAsync(Guid actor, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<FarmEnrollmentOverview> OwnerOverviewAsync(Guid farmId, Guid actor, CancellationToken ct = default) => throw new NotImplementedException();
        public Task DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, CancellationToken ct = default) => throw new NotImplementedException();
        public bool HasActiveTransaction => Started && !Committed;
        public Task<int> SaveChangesAsync(CancellationToken ct = default) => throw new NotImplementedException();
        public Task<IUnitOfWorkTransaction> BeginTransactionAsync(IsolationLevel isolationLevel, CancellationToken ct = default) { Assert.Equal(IsolationLevel.ReadCommitted, isolationLevel); Started = true; return Task.FromResult<IUnitOfWorkTransaction>(this); }
        public Task CommitAsync(CancellationToken ct = default) { Committed = true; return Task.CompletedTask; }
        public ValueTask DisposeAsync() { if (!Committed) CreatedUsers.Clear(); return ValueTask.CompletedTask; }
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
