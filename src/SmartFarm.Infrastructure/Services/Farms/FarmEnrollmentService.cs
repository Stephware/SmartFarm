using System.Data;
using System.Security.Cryptography;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;

namespace SmartFarm.Infrastructure.Services.Farms;

public sealed class FarmEnrollmentService(IFarmEnrollmentRepository requests, IFarmMemberRepository members,
    IUnitOfWork unit) : IFarmEnrollmentService
{
    private static FarmOperationException Missing() => new(FarmError.NotFound, "Farm or request not found.");
    private static FarmOperationException InvalidCode() => new(FarmError.InvalidInput, "Enter a valid code for an active farm.");

    public async Task<FarmJoinRequestDetails> RequestAsync(Guid actor, string code, CancellationToken ct = default)
    {
        var normalized = (code ?? "").Trim().Replace("-", "").ToUpperInvariant();
        if (normalized.Length != 12 || normalized.Any(c => !char.IsAsciiHexDigit(c))) throw InvalidCode();
        await using var transaction = unit.HasActiveTransaction ? null :
            await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        // Every writer locks the farm first, so requests and decisions for it are serialized.
        var farm = await requests.FindFarmByCodeForUpdateAsync(normalized, ct) ?? throw InvalidCode();
        if (!await requests.IsActiveUserAsync(actor, ct)) throw Missing();
        if (farm.OwnerUserId == actor || await members.GetActiveAsync(farm.Id, actor, ct) is not null)
            throw new FarmOperationException(FarmError.Conflict, "You already belong to this farm.");
        var request = await requests.FindPendingAsync(farm.Id, actor, ct);
        if (request is null)
        {
            request = new FarmJoinRequest { FarmId = farm.Id, UserId = actor, RequestedAtUtc = DateTime.UtcNow };
            requests.Add(request);
            Notify(farm.OwnerUserId, request.Id, "requested", "New farm join request",
                $"A farmer requested to join {farm.Name}. Review join requests in My Farms.");
            await Save(ct);
        }
        var result = await requests.DetailsAsync(request.Id, ct);
        if (transaction is not null) await transaction.CommitAsync(ct);
        return result;
    }

    public async Task<IReadOnlyList<FarmJoinRequestDetails>> MyRequestsAsync(Guid actor, CancellationToken ct = default)
    {
        if (!await requests.IsActiveUserAsync(actor, ct)) return [];
        return await requests.ListForUserAsync(actor, ct);
    }

    private async Task<Farm> RequireOwner(Guid farmId, Guid actor, CancellationToken ct)
    {
        var farm = await requests.FindFarmForUpdateAsync(farmId, ct) ?? throw Missing();
        if (farm.OwnerUserId != actor || !await requests.IsActiveUserAsync(actor, ct)) throw Missing();
        return farm;
    }

    public async Task<FarmEnrollmentOverview> OwnerOverviewAsync(Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var farm = await RequireOwner(farmId, actor, ct);
        var history = await requests.ListForFarmAsync(farmId, ct);
        var result = new FarmEnrollmentOverview(farm.JoinCode,
            history.Where(r => r.Status == "Pending").ToList(),
            history.Where(r => r.Status != "Pending").OrderByDescending(r => r.RequestedAtUtc).ToList());
        await transaction.CommitAsync(ct);
        return result;
    }

    public async Task DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var farm = await RequireOwner(farmId, actor, ct);
        var request = await requests.FindAsync(farmId, requestId, ct) ?? throw Missing();
        if (request.Status != "Pending" && request.Status != (approve ? "Approved" : "Rejected"))
            throw new FarmOperationException(FarmError.Conflict, "This request has already been decided. Reload to see its status.");
        if (request.Status == "Pending")
        {
            if (approve)
            {
                if (!await requests.IsActiveUserAsync(request.UserId, ct)) throw Missing();
                if (await members.GetActiveAsync(farmId, request.UserId, ct) is null)
                    members.Add(new FarmMember { FarmId = farmId, UserId = request.UserId, Role = 2, JoinedAtUtc = DateTime.UtcNow });
            }
            request.Status = approve ? "Approved" : "Rejected";
            request.ReviewedByUserId = actor;
            request.ReviewedAtUtc = DateTime.UtcNow;
            var decision = approve ? "approved" : "rejected";
            Notify(request.UserId, request.Id, "decided", "Farm join request " + decision,
                $"Your request to join {farm.Name} was {decision}." +
                (approve ? " Open My Farms to access the farm." : " You can submit a new request with a valid farm code."));
            await Save(ct);
        }
        await transaction.CommitAsync(ct);
    }

    public async Task CancelAsync(Guid farmId, Guid requestId, Guid actor, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        if (await requests.FindFarmForUpdateAsync(farmId, ct) is null ||
            !await requests.IsActiveUserAsync(actor, ct)) throw Missing();
        var request = await requests.FindAsync(farmId, requestId, ct) ?? throw Missing();
        if (request.UserId != actor) throw Missing();
        if (request.Status is not ("Pending" or "Canceled"))
            throw new FarmOperationException(FarmError.Conflict, "This request has already been decided. Reload to see its status.");
        if (request.Status == "Pending")
        {
            request.Status = "Canceled";
            request.ReviewedByUserId = actor;
            request.ReviewedAtUtc = DateTime.UtcNow;
            await Save(ct);
        }
        await transaction.CommitAsync(ct);
    }

    public async Task<string> RegenerateCodeAsync(Guid farmId, Guid actor, string currentCode, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var farm = await RequireOwner(farmId, actor, ct);
        if (!string.Equals(farm.JoinCode, currentCode, StringComparison.Ordinal))
            throw new FarmOperationException(FarmError.Conflict, "The farm code changed. Reload before regenerating it.");
        for (var attempt = 0; ; attempt++)
        {
            do { farm.JoinCode = Convert.ToHexString(RandomNumberGenerator.GetBytes(6)); }
            while (farm.JoinCode == currentCode);
            try { await unit.SaveChangesAsync(ct); break; }
            catch (DbUpdateException error) when (attempt < 4 &&
                error.InnerException is SqlException { Number: 2601 or 2627 } sql &&
                sql.Message.Contains("UX_Farm_JoinCode", StringComparison.Ordinal)) { }
        }
        await transaction.CommitAsync(ct);
        return farm.JoinCode;
    }

    private void Notify(Guid userId, Guid requestId, string action, string title, string message) =>
        requests.Add(new Notification
        {
            UserId = userId, Type = 4, DeduplicationKey = $"enrollment:{requestId:N}:{action}",
            Title = title, Message = message, CreatedAtUtc = DateTime.UtcNow
        });

    private async Task Save(CancellationToken ct)
    {
        try { await unit.SaveChangesAsync(ct); }
        catch (DbUpdateException error) when (error.InnerException is SqlException { Number: 2601 or 2627 })
        {
            throw new FarmOperationException(FarmError.Conflict, "This enrollment changed. Reload and try again.");
        }
    }
}
