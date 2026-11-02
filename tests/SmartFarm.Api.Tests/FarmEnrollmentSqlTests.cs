using System.Data;
using Microsoft.AspNetCore.Builder;
using System.Threading.RateLimiting;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.Extensions.Options;
using SmartFarm.Infrastructure.Security;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Repositories;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class FarmEnrollmentSqlTests
{
    private const string Password = "EnrollmentTest2026!";

    [Fact]
    public async Task IndependentRegistrationAndConcurrentOwnerApprovalUseRealSql_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        using var factory = Factory(connection, synchronizeSignups: true);
        await AssertDisposableDatabase(factory, connection);
        using var owner = factory.CreateClient();
        var suffix = Guid.NewGuid().ToString("N");
        var ownerEmail = $"owner-{suffix}@example.test";
        await Register(owner, ownerEmail);
        await Login(owner, ownerEmail);
        var farm = await Post<FarmDetails>(owner, "/api/farms", new FarmInput { Name = "Approval " + suffix, LocationText = "Test farm" });
        var farmPath = $"/api/farms/{farm.Id}";
        var overview = await Get<FarmEnrollmentOverview>(owner, farmPath + "/join-requests");
        Assert.Matches("^[A-F0-9]{12}$", overview.JoinCode);

        // Parallel account signups remain independent; farm access is requested only after login.
        using var parallelOne = factory.CreateClient();
        using var parallelTwo = factory.CreateClient();
        var parallelSignups = await Task.WhenAll(
            parallelOne.PostAsJsonAsync("/api/auth/register", Registration($"parallel-a-{suffix}@example.test", true, overview.JoinCode)),
            parallelTwo.PostAsJsonAsync("/api/auth/register", Registration($"parallel-b-{suffix}@example.test", true, overview.JoinCode)));
        foreach (var signup in parallelSignups) { using (signup) Assert.Equal(HttpStatusCode.Created, signup.StatusCode); }
        Assert.Empty((await Get<FarmEnrollmentOverview>(owner, farmPath + "/join-requests")).Requests);
        await Login(parallelOne, $"parallel-a-{suffix}@example.test");
        await Login(parallelTwo, $"parallel-b-{suffix}@example.test");
        await Task.WhenAll(
            Post<FarmJoinRequestDetails>(parallelOne, "/api/farm-join-requests", new { farmCode = overview.JoinCode }),
            Post<FarmJoinRequestDetails>(parallelTwo, "/api/farm-join-requests", new { farmCode = overview.JoinCode }));
        var parallelRequests = (await Get<FarmEnrollmentOverview>(owner, farmPath + "/join-requests")).Requests;
        foreach (var request in parallelRequests)
        {
            using var rejection = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{request.Id}/decision", new { approve = false });
            Assert.Equal(HttpStatusCode.NoContent, rejection.StatusCode);
        }

        using var applicant = factory.CreateClient();
        var invalidEmail = $"invalid-{suffix}@example.test";
        // Obsolete signup farm fields are ignored: even an invalid code cannot block account creation.
        using (var invalid = await applicant.PostAsJsonAsync("/api/auth/register", Registration(invalidEmail, true, "000000000000")))
            Assert.Equal(HttpStatusCode.Created, invalid.StatusCode);
        await WithDb(factory, async db => Assert.True(await db.Users.AnyAsync(u => u.Email == invalidEmail)));

        var applicantEmail = $"farmer-{suffix}@example.test";
        await Register(applicant, applicantEmail, true, "  " + overview.JoinCode[..4].ToLowerInvariant() + "-" + overview.JoinCode[4..].ToLowerInvariant() + "  ");
        using (var duplicate = await applicant.PostAsJsonAsync("/api/auth/register", Registration(applicantEmail, true, overview.JoinCode))) Assert.Equal(HttpStatusCode.BadRequest, duplicate.StatusCode);
        await WithDb(factory, async db => Assert.Equal(1, await db.Users.CountAsync(u => u.Email == applicantEmail)));
        await Login(applicant, applicantEmail);
        Assert.Empty(await Get<List<FarmJoinRequestDetails>>(applicant, "/api/farm-join-requests"));
        await Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        using (var pendingAccess = await applicant.GetAsync(farmPath)) Assert.Equal(HttpStatusCode.NotFound, pendingAccess.StatusCode);
        using (var hiddenOverview = await applicant.GetAsync(farmPath + "/join-requests")) Assert.Equal(HttpStatusCode.NotFound, hiddenOverview.StatusCode);
        var pending = Assert.Single(await Get<List<FarmJoinRequestDetails>>(applicant, "/api/farm-join-requests"));
        Assert.Equal("Pending", pending.Status);
        Assert.Equal(applicantEmail, pending.Email);
        var repeats = await Task.WhenAll(Enumerable.Range(0, 4).Select(_ => Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode })));
        Assert.All(repeats, r => Assert.Equal(pending.Id, r.Id));
        Assert.Single((await Get<FarmEnrollmentOverview>(owner, farmPath + "/join-requests")).Requests);
        using (var wrongFarm = await owner.PostAsJsonAsync($"/api/farms/{Guid.NewGuid()}/join-requests/{pending.Id}/decision", new { approve = true })) Assert.Equal(HttpStatusCode.NotFound, wrongFarm.StatusCode);
        using (var missingDecision = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{pending.Id}/decision", new { })) Assert.Equal(HttpStatusCode.BadRequest, missingDecision.StatusCode);
        var decisions = await Task.WhenAll(Enumerable.Range(0, 4).Select(_ => owner.PostAsJsonAsync(farmPath + $"/join-requests/{pending.Id}/decision", new { approve = true, role = 1, userId = Guid.NewGuid(), farmId = Guid.NewGuid() })));
        foreach (var decision in decisions) { using (decision) Assert.Equal(HttpStatusCode.NoContent, decision.StatusCode); }
        using (var oppositeDecision = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{pending.Id}/decision", new { approve = false })) Assert.Equal(HttpStatusCode.Conflict, oppositeDecision.StatusCode);
        Assert.Equal("FieldWorker", (await Get<FarmDetails>(applicant, farmPath)).Role);
        using (var workerOverview = await applicant.GetAsync(farmPath + "/join-requests")) Assert.Equal(HttpStatusCode.NotFound, workerOverview.StatusCode);
        await WithDb(factory, async db =>
        {
            var membership = Assert.Single(await db.FarmMembers.Where(m => m.FarmId == farm.Id && m.UserId == pending.UserId && m.RemovedAtUtc == null).ToListAsync());
            Assert.Equal((byte)2, membership.Role);
            Assert.Equal("Approved", (await db.FarmJoinRequests.SingleAsync(r => r.Id == pending.Id)).Status);
        });

        await Login(parallelOne, $"parallel-a-{suffix}@example.test");
        var contested = await Post<FarmJoinRequestDetails>(parallelOne, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        var conflictingDecisions = await Task.WhenAll(
            owner.PostAsJsonAsync(farmPath + $"/join-requests/{contested.Id}/decision", new { approve = true }),
            owner.PostAsJsonAsync(farmPath + $"/join-requests/{contested.Id}/decision", new { approve = false }));
        Assert.Equal(1, conflictingDecisions.Count(r => r.StatusCode == HttpStatusCode.NoContent));
        Assert.Equal(1, conflictingDecisions.Count(r => r.StatusCode == HttpStatusCode.Conflict));
        foreach (var decision in conflictingDecisions) decision.Dispose();

        // Removed memberships may reapply, and a rejection preserves the earlier approvals.
        var member = (await Get<List<FarmMemberDetails>>(owner, farmPath + "/members")).Single(m => m.UserId == pending.UserId);
        using (var removal = await owner.DeleteAsync(farmPath + $"/members/{member.Id}")) Assert.Equal(HttpStatusCode.NoContent, removal.StatusCode);
        var reapplied = await Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        using (var rejection = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{reapplied.Id}/decision", new { approve = false })) Assert.Equal(HttpStatusCode.NoContent, rejection.StatusCode);
        using (var rejectedAccess = await applicant.GetAsync(farmPath)) Assert.Equal(HttpStatusCode.NotFound, rejectedAccess.StatusCode);
        var afterRejection = await Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        Assert.Equal(3, (await Get<List<FarmJoinRequestDetails>>(applicant, "/api/farm-join-requests")).Count);

        // A disabled applicant and an archived farm never receive access.
        await WithDb(factory, async db => { (await db.Users.SingleAsync(u => u.Id == pending.UserId)).DisabledAtUtc = DateTime.UtcNow; await db.SaveChangesAsync(); });
        using (var disabled = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{afterRejection.Id}/decision", new { approve = true })) Assert.Equal(HttpStatusCode.NotFound, disabled.StatusCode);
        using (var archived = await owner.SendAsync(new HttpRequestMessage(HttpMethod.Delete, farmPath) { Content = JsonContent.Create(new RowVersionInput { RowVersion = farm.RowVersion }) })) Assert.Equal(HttpStatusCode.NoContent, archived.StatusCode);
        var archivedEmail = $"archived-{suffix}@example.test";
        using (var invalidArchived = await applicant.PostAsJsonAsync("/api/auth/register", Registration(archivedEmail, true, overview.JoinCode))) Assert.Equal(HttpStatusCode.Created, invalidArchived.StatusCode);
        await WithDb(factory, async db => Assert.True(await db.Users.AnyAsync(u => u.Email == archivedEmail)));
    }

    [Fact]
    public async Task RegistrationDoesNotDependOnEnrollmentAvailability_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        using var factory = Factory(connection, failEnrollment: true);
        await AssertDisposableDatabase(factory, connection);
        using var client = factory.CreateClient();
        var email = $"rollback-{Guid.NewGuid():N}@example.test";
        using var response = await client.PostAsJsonAsync("/api/auth/register", Registration(email, true, "ABCD12345678"));
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        await WithDb(factory, async db => Assert.True(await db.Users.AnyAsync(u => u.Email == email)));
    }

    [Fact]
    public async Task ActiveOwnerAndApplicantReadsCannotDeadlockAcrossFarms_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        using var factory = Factory(connection);
        await AssertDisposableDatabase(factory, connection);
        var first = new SmartFarmUser { FullName = "Lock test owner one" };
        var second = new SmartFarmUser { FullName = "Lock test owner two" };
        await WithDb(factory, async db => { db.Users.AddRange(first, second); await db.SaveChangesAsync(); });
        var ready = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var arrivals = 0;
        async Task CrossRead(Guid owner, Guid applicant)
        {
            await using var scope = factory.Services.CreateAsyncScope();
            var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();
            await using var transaction = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable);
            var repository = new FarmEnrollmentRepository(db);
            Assert.True(await repository.IsActiveUserAsync(owner, default));
            if (Interlocked.Increment(ref arrivals) == 2) ready.TrySetResult();
            await ready.Task.WaitAsync(TimeSpan.FromSeconds(15));
            Assert.True(await repository.IsActiveUserAsync(applicant, default));
            await transaction.CommitAsync();
        }
        await Task.WhenAll(CrossRead(first.Id, second.Id), CrossRead(second.Id, first.Id));
    }

    [Fact]
    public async Task FarmCreationRetriesARandomJoinCodeCollision_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        var collision = new ForceJoinCodeCollision();
        using var factory = Factory(connection, interceptor: collision);
        await AssertDisposableDatabase(factory, connection);
        using var owner = factory.CreateClient();
        var email = $"collision-{Guid.NewGuid():N}@example.test";
        await Register(owner, email);
        await Login(owner, email);
        await WithDb(factory, async db =>
        {
            var user = await db.Users.SingleAsync(u => u.Email == email);
            db.Farms.Add(new SmartFarm.Core.Entities.Farm { OwnerUserId = user.Id, Name = "Occupied join code", LocationText = "Test", JoinCode = collision.OccupiedCode });
            await db.SaveChangesAsync();
        });
        var created = await Post<FarmDetails>(owner, "/api/farms", new FarmInput { Name = "Collision retry farm", LocationText = "Test farm" });
        Assert.Equal(2, collision.Attempts);
        await WithDb(factory, async db => Assert.NotEqual(collision.OccupiedCode, (await db.Farms.SingleAsync(f => f.Id == created.Id)).JoinCode));
    }

    [Fact]
    public async Task CancellationRotationAndNotificationsAreAtomicUnderConcurrency_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        using var factory = Factory(connection);
        await AssertDisposableDatabase(factory, connection);
        using var owner = factory.CreateClient();
        using var applicant = factory.CreateClient();
        var suffix = Guid.NewGuid().ToString("N");
        var ownerEmail = $"rotate-owner-{suffix}@example.test";
        var email = $"cancel-applicant-{suffix}@example.test";
        await Register(owner, ownerEmail); await Login(owner, ownerEmail);
        await Register(applicant, email); await Login(applicant, email);
        var farm = await Post<FarmDetails>(owner, "/api/farms", new FarmInput { Name = "Cancel " + suffix, LocationText = "Test farm" });
        var path = $"/api/farms/{farm.Id}";
        var original = await Get<FarmEnrollmentOverview>(owner, path + "/join-requests");
        var duplicates = await Task.WhenAll(Enumerable.Range(0, 4).Select(_ =>
            Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = original.JoinCode })));
        var request = duplicates[0];
        Assert.All(duplicates, r => Assert.Equal(request.Id, r.Id));
        using (var forbidden = await owner.PostAsync(path + $"/join-requests/{request.Id}/cancel", null)) Assert.Equal(HttpStatusCode.NotFound, forbidden.StatusCode);
        var cancellations = await Task.WhenAll(Enumerable.Range(0, 3).Select(_ => applicant.PostAsync(path + $"/join-requests/{request.Id}/cancel", null)));
        foreach (var response in cancellations) { using (response) Assert.Equal(HttpStatusCode.NoContent, response.StatusCode); }
        var canceled = Assert.Single(await Get<List<FarmJoinRequestDetails>>(applicant, "/api/farm-join-requests"));
        Assert.Equal("Canceled", canceled.Status);
        var contested = await Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = original.JoinCode });
        var race = await Task.WhenAll(
            owner.PostAsJsonAsync(path + $"/join-requests/{contested.Id}/decision", new { approve = true }),
            applicant.PostAsync(path + $"/join-requests/{contested.Id}/cancel", null));
        Assert.Equal(1, race.Count(r => r.StatusCode == HttpStatusCode.NoContent));
        Assert.Equal(1, race.Count(r => r.StatusCode == HttpStatusCode.Conflict));
        foreach (var response in race) response.Dispose();
        var rotations = await Task.WhenAll(Enumerable.Range(0, 2).Select(_ => owner.PostAsJsonAsync(path + "/join-code/regenerate", new { currentCode = original.JoinCode })));
        Assert.Equal(1, rotations.Count(r => r.StatusCode == HttpStatusCode.OK));
        Assert.Equal(1, rotations.Count(r => r.StatusCode == HttpStatusCode.Conflict));
        foreach (var response in rotations) response.Dispose();
        var current = await Get<FarmEnrollmentOverview>(owner, path + "/join-requests");
        Assert.NotEqual(original.JoinCode, current.JoinCode);
        Assert.Empty(current.Requests); Assert.Equal(2, current.History!.Count);
        using var newApplicant = factory.CreateClient();
        var newEmail = $"new-applicant-{suffix}@example.test";
        await Register(newApplicant, newEmail); await Login(newApplicant, newEmail);
        using (var oldCode = await newApplicant.PostAsJsonAsync("/api/farm-join-requests", new { farmCode = original.JoinCode })) Assert.Equal(HttpStatusCode.BadRequest, oldCode.StatusCode);
        await Post<FarmJoinRequestDetails>(newApplicant, "/api/farm-join-requests", new { farmCode = current.JoinCode });
        await WithDb(factory, async db =>
        {
            var stored = await db.FarmJoinRequests.SingleAsync(r => r.Id == contested.Id);
            Assert.Contains(stored.Status, new[] { "Approved", "Canceled" });
            var membership = await db.FarmMembers.CountAsync(m => m.FarmId == farm.Id && m.UserId == request.UserId && m.RemovedAtUtc == null);
            Assert.Equal(stored.Status == "Approved" ? 1 : 0, membership);
            var ownerId = (await db.Users.SingleAsync(u => u.Email == ownerEmail)).Id;
            Assert.Equal(3, await db.Notifications.CountAsync(n => n.UserId == ownerId && n.Type == 4));
            Assert.Equal(stored.Status == "Approved" ? 1 : 0, await db.Notifications.CountAsync(n => n.UserId == request.UserId && n.Type == 4));
            Assert.Equal(3, await db.FarmJoinRequests.CountAsync(r => r.FarmId == farm.Id));
        });
    }

    [Fact]
    public async Task EnrollmentNotificationsRollBackWithRequestAndMembership_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        var failure = new FailAfterEnrollmentSave();
        using var factory = Factory(connection, interceptor: failure);
        using var owner = factory.CreateClient();
        using var applicant = factory.CreateClient();
        var suffix = Guid.NewGuid().ToString("N");
        var ownerEmail = $"atomic-owner-{suffix}@example.test";
        var email = $"atomic-applicant-{suffix}@example.test";
        await Register(owner, ownerEmail); await Login(owner, ownerEmail);
        await Register(applicant, email); await Login(applicant, email);
        var farm = await Post<FarmDetails>(owner, "/api/farms", new FarmInput { Name = "Atomic " + suffix, LocationText = "Test farm" });
        var overview = await Get<FarmEnrollmentOverview>(owner, $"/api/farms/{farm.Id}/join-requests");
        Guid actor = default, ownerId = default;
        await WithDb(factory, async db =>
        {
            actor = (await db.Users.SingleAsync(u => u.Email == email)).Id;
            ownerId = (await db.Users.SingleAsync(u => u.Email == ownerEmail)).Id;
        });
        failure.Enabled = true;
        await using (var scope = factory.Services.CreateAsyncScope())
            await Assert.ThrowsAsync<DbUpdateException>(() => scope.ServiceProvider.GetRequiredService<IFarmEnrollmentService>().RequestAsync(actor, overview.JoinCode));
        await WithDb(factory, async db =>
        {
            Assert.False(await db.FarmJoinRequests.AnyAsync(r => r.FarmId == farm.Id));
            Assert.False(await db.Notifications.AnyAsync(n => n.UserId == ownerId));
        });
        failure.Enabled = false;
        var request = await Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        failure.Enabled = true;
        await using (var scope = factory.Services.CreateAsyncScope())
            await Assert.ThrowsAsync<DbUpdateException>(() => scope.ServiceProvider.GetRequiredService<IFarmEnrollmentService>().DecideAsync(farm.Id, request.Id, ownerId, true));
        await WithDb(factory, async db =>
        {
            Assert.Equal("Pending", (await db.FarmJoinRequests.SingleAsync(r => r.Id == request.Id)).Status);
            Assert.False(await db.FarmMembers.AnyAsync(m => m.FarmId == farm.Id));
            Assert.False(await db.Notifications.AnyAsync(n => n.UserId == actor));
            Assert.Single(await db.Notifications.Where(n => n.UserId == ownerId).ToListAsync());
        });
    }

    private static string? TestConnection()
    {
        var connection = Environment.GetEnvironmentVariable("SMARTFARM_ENROLLMENT_TEST_CONNECTION");
        if (string.IsNullOrWhiteSpace(connection)) return null;
        var database = new SqlConnectionStringBuilder(connection).InitialCatalog;
        Assert.True(database.StartsWith("SmartFarmEnrollmentTest_", StringComparison.Ordinal) || database.StartsWith("SmartFarmEnrollmentBackend_", StringComparison.Ordinal), "Enrollment SQL tests require a separately initialized disposable enrollment database.");
        return connection;
    }

    private static WebApplicationFactory<Program> Factory(string connection, bool failEnrollment = false, bool synchronizeSignups = false, IInterceptor? interceptor = null) =>
        new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.ConfigureAppConfiguration((_, config) => config.AddInMemoryCollection(new Dictionary<string, string?> { ["ConnectionStrings:SmartFarm"] = connection }));
            builder.ConfigureServices(services =>
            {
                services.RemoveAll<IConfigureOptions<RateLimiterOptions>>();
                services.AddRateLimiter(options =>
                {
                    foreach (var name in new[] { EnrollmentRateLimiting.Registration, EnrollmentRateLimiting.Login, EnrollmentRateLimiting.Enrollment })
                        options.AddPolicy(name, _ => RateLimitPartition.GetNoLimiter("sql-workflow"));
                });
                services.RemoveAll<SmartFarmDbContext>();
                services.RemoveAll<DbContextOptions<SmartFarmDbContext>>();
                services.AddDbContext<SmartFarmDbContext>(options =>
                {
                    options.UseSqlServer(connection);
                    if (interceptor is not null) options.AddInterceptors(interceptor);
                });
                if (failEnrollment) services.AddScoped<IFarmEnrollmentService, FailedEnrollment>();
                if (synchronizeSignups) services.AddSingleton<IUserValidator<SmartFarmUser>, ParallelSignupBarrier>();
            });
        });

    private static Task AssertDisposableDatabase(WebApplicationFactory<Program> factory, string connection) =>
        WithDb(factory, db =>
        {
            Assert.Equal(new SqlConnectionStringBuilder(connection).InitialCatalog, db.Database.GetDbConnection().Database);
            return Task.CompletedTask;
        });

    private static object Registration(string email, bool joinFarm, string? code) => new { fullName = "Enrollment Tester", email, password = Password, confirmPassword = Password, joinFarm, farmCode = code };
    private static async Task Register(HttpClient client, string email, bool joinFarm = false, string? code = null)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/register", Registration(email, joinFarm, code));
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }
    private static async Task Login(HttpClient client, string email)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/login", new { email, password = Password });
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", json.RootElement.GetProperty("accessToken").GetString());
    }
    private static async Task<T> Post<T>(HttpClient client, string path, object input)
    {
        using var response = await client.PostAsJsonAsync(path, input);
        Assert.True(response.IsSuccessStatusCode, $"{path}: {(int)response.StatusCode} {(await response.Content.ReadAsStringAsync()).Split("HEADERS\n")[0]}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }
    private static async Task<T> Get<T>(HttpClient client, string path)
    {
        using var response = await client.GetAsync(path);
        Assert.True(response.IsSuccessStatusCode, $"{path}: {(int)response.StatusCode} {(await response.Content.ReadAsStringAsync()).Split("HEADERS\n")[0]}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }
    private static async Task WithDb(WebApplicationFactory<Program> factory, Func<SmartFarmDbContext, Task> work)
    {
        await using var scope = factory.Services.CreateAsyncScope();
        await work(scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>());
    }
    private sealed class FailAfterEnrollmentSave : SaveChangesInterceptor
    {
        public bool Enabled { get; set; }
        private bool fail;
        public override ValueTask<InterceptionResult<int>> SavingChangesAsync(DbContextEventData eventData, InterceptionResult<int> result, CancellationToken ct = default)
        {
            fail = Enabled && eventData.Context!.ChangeTracker.Entries<SmartFarm.Core.Entities.Notification>().Any(e => e.State == EntityState.Added && e.Entity.Type == 4);
            return ValueTask.FromResult(result);
        }
        public override ValueTask<int> SavedChangesAsync(SaveChangesCompletedEventData eventData, int result, CancellationToken ct = default)
        {
            if (fail) throw new DbUpdateException("Simulated failure after writing enrollment and notification rows.");
            return ValueTask.FromResult(result);
        }
    }

    private sealed class ForceJoinCodeCollision : SaveChangesInterceptor
    {
        public string OccupiedCode { get; } = Guid.NewGuid().ToString("N")[..12].ToUpperInvariant();
        public int Attempts { get; private set; }
        public override ValueTask<InterceptionResult<int>> SavingChangesAsync(DbContextEventData eventData, InterceptionResult<int> result, CancellationToken ct = default)
        {
            var farm = eventData.Context!.ChangeTracker.Entries<SmartFarm.Core.Entities.Farm>()
                .SingleOrDefault(e => e.State == EntityState.Added && e.Entity.Name == "Collision retry farm");
            if (farm is not null && ++Attempts == 1) farm.Entity.JoinCode = OccupiedCode;
            return ValueTask.FromResult(result);
        }
    }

    private sealed class ParallelSignupBarrier : IUserValidator<SmartFarmUser>
    {
        private int arrivals;
        private readonly TaskCompletionSource ready = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public async Task<IdentityResult> ValidateAsync(UserManager<SmartFarmUser> manager, SmartFarmUser user)
        {
            if (user.Email?.StartsWith("parallel-", StringComparison.Ordinal) == true)
            {
                if (Interlocked.Increment(ref arrivals) == 2) ready.TrySetResult();
                await ready.Task.WaitAsync(TimeSpan.FromSeconds(15));
            }
            return IdentityResult.Success;
        }
    }

    private sealed class FailedEnrollment : IFarmEnrollmentService
    {
        public Task CancelAsync(Guid farmId, Guid requestId, Guid actor, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<string> RegenerateCodeAsync(Guid farmId, Guid actor, string currentCode, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<FarmJoinRequestDetails> RequestAsync(Guid actor, string code, CancellationToken ct = default) => throw new DbUpdateException("Simulated enrollment persistence failure.");
        public Task<IReadOnlyList<FarmJoinRequestDetails>> MyRequestsAsync(Guid actor, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<FarmEnrollmentOverview> OwnerOverviewAsync(Guid farmId, Guid actor, CancellationToken ct = default) => throw new NotImplementedException();
        public Task DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, CancellationToken ct = default) => throw new NotImplementedException();
    }
}
