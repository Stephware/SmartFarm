using System.Data;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Infrastructure.Services.Farms;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class FarmEnrollmentServiceTests
{
    [Fact]
    public async Task NormalizedRepeatedRequestsStayPendingWithoutMembership()
    {
        var state = new State();
        var first = await state.Service.RequestAsync(state.Applicant, "  abcd-1234-5678  ");
        var second = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        Assert.Equal(first.Id, second.Id);
        Assert.Equal("Pending", first.Status);
        Assert.Single(state.Requests);
        Assert.Empty(state.Members);
        Assert.NotEqual(default, first.RequestedAtUtc);
    }

    [Theory]
    [InlineData("")]
    [InlineData("01234567890Z")]
    [InlineData("000000000000")]
    public async Task InvalidCodeCannotCreateRequest(string code)
    {
        var state = new State();
        var error = await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.RequestAsync(state.Applicant, code));
        Assert.Equal(FarmError.InvalidInput, error.Error);
        Assert.Empty(state.Requests);
    }

    [Fact]
    public async Task OnlyOwnerCanSeeApplicantsOrDecideStoredRequest()
    {
        var state = new State();
        var request = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        var stranger = Guid.NewGuid();
        Assert.Equal(FarmError.NotFound, (await Assert.ThrowsAsync<FarmOperationException>(() =>
            state.Service.OwnerOverviewAsync(state.Farm.Id, stranger))).Error);
        Assert.Equal(FarmError.NotFound, (await Assert.ThrowsAsync<FarmOperationException>(() =>
            state.Service.DecideAsync(state.Farm.Id, request.Id, stranger, true))).Error);
        await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.DecideAsync(Guid.NewGuid(), request.Id, state.Owner, true));
        Assert.Empty(state.Members);
        var overview = await state.Service.OwnerOverviewAsync(state.Farm.Id, state.Owner);
        Assert.Equal(state.Farm.JoinCode, overview.JoinCode);
        Assert.Equal(state.Applicant, Assert.Single(overview.Requests).UserId);
    }

    [Fact]
    public async Task ApprovalGrantsFieldWorkerOnceAndDoesNotOverwriteDecision()
    {
        var state = new State();
        var request = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        await state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, true);
        await state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, true);
        var conflict = await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, false));
        Assert.Equal(FarmError.Conflict, conflict.Error);
        Assert.Equal((byte)2, Assert.Single(state.Members).Role);
        var decided = Assert.Single(state.Requests);
        Assert.Equal("Approved", decided.Status);
        Assert.Equal(state.Owner, decided.ReviewedByUserId);
        Assert.NotNull(decided.ReviewedAtUtc);
        Assert.Empty((await state.Service.OwnerOverviewAsync(state.Farm.Id, state.Owner)).Requests);
        Assert.Equal("Approved", Assert.Single(await state.Service.MyRequestsAsync(state.Applicant)).Status);
    }

    [Fact]
    public async Task RejectionAndRemovedMembershipAllowNewRequestsWithHistory()
    {
        var state = new State();
        var rejected = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        await state.Service.DecideAsync(state.Farm.Id, rejected.Id, state.Owner, false);
        Assert.Empty(state.Members);
        var reapplied = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        Assert.NotEqual(rejected.Id, reapplied.Id);
        await state.Service.DecideAsync(state.Farm.Id, reapplied.Id, state.Owner, true);
        Assert.Single(state.Members).RemovedAtUtc = DateTime.UtcNow;
        var removed = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        Assert.NotEqual(reapplied.Id, removed.Id);
        Assert.Equal(3, state.Requests.Count);
    }

    [Theory]
    [InlineData("applicant")]
    [InlineData("owner")]
    [InlineData("farm")]
    public async Task ArchivedFarmOrDisabledPersonCannotApprove(string inactive)
    {
        var state = new State();
        var request = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        if (inactive == "farm") state.Farm.ArchivedAtUtc = DateTime.UtcNow;
        else state.Inactive.Add(inactive == "owner" ? state.Owner : state.Applicant);
        await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, true));
        Assert.Empty(state.Members);
        Assert.Equal("Pending", Assert.Single(state.Requests).Status);
    }

    [Fact]
    public async Task ExistingMembershipCannotBeDemotedByApprovalOrRequest()
    {
        var state = new State();
        var request = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        state.Members.Add(new FarmMember { FarmId = state.Farm.Id, UserId = state.Applicant, Role = 1 });
        await state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, true);
        Assert.Equal((byte)1, Assert.Single(state.Members).Role);
        await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode));
    }

    [Fact]
    public async Task ApplicantCanCancelAndReapplyWithoutErasingHistory()
    {
        var state = new State();
        var pending = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.CancelAsync(state.Farm.Id, pending.Id, state.Owner));
        await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.CancelAsync(Guid.NewGuid(), pending.Id, state.Applicant));
        await state.Service.CancelAsync(state.Farm.Id, pending.Id, state.Applicant);
        await state.Service.CancelAsync(state.Farm.Id, pending.Id, state.Applicant);
        var canceled = Assert.Single(state.Requests);
        Assert.Equal("Canceled", canceled.Status);
        Assert.Equal(state.Applicant, canceled.ReviewedByUserId);
        Assert.NotNull(canceled.ReviewedAtUtc);
        Assert.Empty(state.Members);
        Assert.Equal(FarmError.Conflict, (await Assert.ThrowsAsync<FarmOperationException>(() =>
            state.Service.DecideAsync(state.Farm.Id, pending.Id, state.Owner, true))).Error);
        var next = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        Assert.NotEqual(pending.Id, next.Id);
        var overview = await state.Service.OwnerOverviewAsync(state.Farm.Id, state.Owner);
        Assert.Equal(next.Id, Assert.Single(overview.Requests).Id);
        Assert.Equal(pending.Id, Assert.Single(overview.History!).Id);
    }

    [Fact]
    public async Task DecidedRequestCannotBeCanceledOrGrantAccessTwice()
    {
        var state = new State();
        var request = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        await state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, true);
        Assert.Equal(FarmError.Conflict, (await Assert.ThrowsAsync<FarmOperationException>(() =>
            state.Service.CancelAsync(state.Farm.Id, request.Id, state.Applicant))).Error);
        Assert.Equal("Approved", Assert.Single(state.Requests).Status);
        Assert.Single(state.Members);
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task NewRequestsAndDecisionsNotifyOnceInsideTheirTransaction(bool approve)
    {
        var state = new State();
        var request = await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        await state.Service.RequestAsync(state.Applicant, state.Farm.JoinCode);
        var incoming = Assert.Single(state.Notifications);
        Assert.Equal(state.Owner, incoming.UserId);
        Assert.Equal((byte)4, incoming.Type);
        Assert.Contains(state.Farm.Name, incoming.Message);
        Assert.DoesNotContain(state.Farm.JoinCode, incoming.Message);
        await state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, approve);
        await state.Service.DecideAsync(state.Farm.Id, request.Id, state.Owner, approve);
        var decision = Assert.Single(state.Notifications, n => n.UserId == state.Applicant);
        Assert.Contains(approve ? "approved" : "rejected", decision.Message);
        Assert.Equal(2, state.Notifications.Count);
        Assert.NotEqual(incoming.DeduplicationKey, decision.DeduplicationKey);
    }

    [Fact]
    public async Task OnlyOwnerCanRotateCodeAndStaleRotationCannotReplaceNewerCode()
    {
        var state = new State();
        var old = state.Farm.JoinCode;
        var request = await state.Service.RequestAsync(state.Applicant, old);
        await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.RegenerateCodeAsync(state.Farm.Id, state.Applicant, old));
        var code = await state.Service.RegenerateCodeAsync(state.Farm.Id, state.Owner, old);
        Assert.Matches("^[A-F0-9]{12}$", code);
        Assert.NotEqual(old, code);
        Assert.Equal(code, state.Farm.JoinCode);
        Assert.Equal(FarmError.Conflict, (await Assert.ThrowsAsync<FarmOperationException>(() =>
            state.Service.RegenerateCodeAsync(state.Farm.Id, state.Owner, old))).Error);
        Assert.Equal(code, state.Farm.JoinCode);
        await Assert.ThrowsAsync<FarmOperationException>(() => state.Service.RequestAsync(Guid.NewGuid(), old));
        Assert.Equal(request.Id, (await state.Service.RequestAsync(state.Applicant, code)).Id);
        Assert.Single(state.Requests);
        Assert.Empty(state.Members);
    }

    private sealed class State : IFarmEnrollmentRepository, IFarmMemberRepository, IUnitOfWork, IUnitOfWorkTransaction
    {
        public Guid Owner { get; } = Guid.NewGuid();
        public Guid Applicant { get; } = Guid.NewGuid();
        public Farm Farm { get; }
        public FarmEnrollmentService Service { get; }
        public List<FarmJoinRequest> Requests { get; } = [];
        public List<FarmMember> Members { get; } = [];
        public List<Notification> Notifications { get; } = [];
        public HashSet<Guid> Inactive { get; } = [];
        public State() { Farm = new Farm { OwnerUserId = Owner, JoinCode = "ABCD12345678", Name = "Test Farm" }; Service = new(this, this, this); }
        public Task<Farm?> FindFarmByCodeForUpdateAsync(string code, CancellationToken ct) => Task.FromResult(Farm.JoinCode == code && Farm.ArchivedAtUtc is null ? Farm : null);
        public Task<Farm?> FindFarmForUpdateAsync(Guid farmId, CancellationToken ct) => Task.FromResult(Farm.Id == farmId && Farm.ArchivedAtUtc is null ? Farm : null);
        public Task<bool> IsActiveUserAsync(Guid userId, CancellationToken ct) => Task.FromResult(!Inactive.Contains(userId));
        public Task<FarmJoinRequest?> FindPendingAsync(Guid farmId, Guid userId, CancellationToken ct) => Task.FromResult(Requests.SingleOrDefault(r => r.FarmId == farmId && r.UserId == userId && r.Status == "Pending"));
        public Task<FarmJoinRequest?> FindAsync(Guid farmId, Guid requestId, CancellationToken ct) => Task.FromResult(Requests.SingleOrDefault(r => r.FarmId == farmId && r.Id == requestId));
        private FarmJoinRequestDetails Map(FarmJoinRequest r) => new(r.Id, r.FarmId, Farm.Name, r.UserId, "Farmer", "farmer@example.test", r.Status, r.RequestedAtUtc, r.ReviewedAtUtc);
        public Task<FarmJoinRequestDetails> DetailsAsync(Guid requestId, CancellationToken ct) => Task.FromResult(Map(Requests.Single(r => r.Id == requestId)));
        public Task<IReadOnlyList<FarmJoinRequestDetails>> ListForUserAsync(Guid userId, CancellationToken ct) => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>(Requests.Where(r => r.UserId == userId).Select(Map).ToList());
        public Task<IReadOnlyList<FarmJoinRequestDetails>> ListForFarmAsync(Guid farmId, CancellationToken ct) => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>(Requests.Where(r => r.FarmId == farmId).Select(Map).ToList());
        public void Add(Notification notification) { Assert.True(HasActiveTransaction); Notifications.Add(notification); }
        public void Add(FarmJoinRequest request) => Requests.Add(request);
        public Task<FarmMember?> GetActiveAsync(Guid farmId, Guid userId, CancellationToken ct = default) => Task.FromResult(Members.SingleOrDefault(m => m.FarmId == farmId && m.UserId == userId && m.RemovedAtUtc is null));
        public Task<IReadOnlyList<FarmMember>> ListActiveAsync(Guid farmId, CancellationToken ct = default) => throw new NotImplementedException();
        public void Add(FarmMember member) => Members.Add(member);
        public Task<IReadOnlyList<FarmMemberDetails>> ListDetailsAsync(Guid farmId, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<IReadOnlyList<FarmMember>> ListForUserAsync(Guid farmId, Guid userId, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<FarmMember?> GetByMemberIdAsync(Guid farmId, Guid memberId, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<byte?> GetActiveRoleAsync(Guid farmId, Guid userId, CancellationToken ct = default) => throw new NotImplementedException();
        public bool HasActiveTransaction { get; private set; }
        public Task<int> SaveChangesAsync(CancellationToken ct = default) => Task.FromResult(1);
        public Task<IUnitOfWorkTransaction> BeginTransactionAsync(IsolationLevel isolationLevel, CancellationToken ct = default) { Assert.Equal(IsolationLevel.Serializable, isolationLevel); HasActiveTransaction = true; return Task.FromResult<IUnitOfWorkTransaction>(this); }
        public Task CommitAsync(CancellationToken ct = default) => Task.CompletedTask;
        public ValueTask DisposeAsync() { HasActiveTransaction = false; return ValueTask.CompletedTask; }
    }
}
