using System.Security.Cryptography;
using System.Text.Json;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.Identity;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Security;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Farms;

public sealed class FarmManagementService(
    SmartFarmDbContext db,
    IFarmAccessService access,
    UserManager<SmartFarmUser> users,
    IDataProtectionProvider protection) : IFarmManagementService
{
    private readonly ITimeLimitedDataProtector _inviteTokens = protection
        .CreateProtector("SmartFarm.FarmInvitation.v1").ToTimeLimitedDataProtector();

    private static FarmOperationException Missing() =>
        new(FarmError.NotFound, "Farm or resource not found.");

    private static FarmOperationException Invalid(string reason) =>
        new(FarmError.InvalidInput, reason);

    private static FarmOperationException Conflict(string reason) =>
        new(FarmError.Conflict, reason);

    private async Task<FarmAccessLevel> RequireAccess(Guid farmId, Guid actor,
        FarmAction action, CancellationToken ct)
    {
        var level = await access.GetAccessLevelAsync(farmId, actor, ct);
        if (!FarmAccessRules.Allows(level, action)) throw Missing();
        return level;
    }

    private static byte[] ParseVersion(string? version)
    {
        if (string.IsNullOrWhiteSpace(version)) throw Invalid("A current row version is required.");
        try
        {
            var parsed = Convert.FromBase64String(version);
            return parsed.Length == 8 ? parsed : throw Invalid("Invalid row version.");
        }
        catch (FormatException)
        {
            throw Invalid("Invalid row version.");
        }
    }

    private static void ValidateFarm(FarmInput input)
    {
        if (string.IsNullOrWhiteSpace(input.Name) || input.Name.Trim().Length is < 2 or > 120
            || string.IsNullOrWhiteSpace(input.LocationText) || input.LocationText.Trim().Length is < 2 or > 250
            || string.IsNullOrWhiteSpace(input.TimeZoneId) || input.TimeZoneId.Trim().Length > 64)
            throw Invalid("Supply a valid farm name, location and time zone.");
        if (input.Latitude.HasValue != input.Longitude.HasValue
            || input.Latitude is < -90 or > 90 || input.Longitude is < -180 or > 180
            || input.Latitude is { } latitude && decimal.Round(latitude, 6) != latitude
            || input.Longitude is { } longitude && decimal.Round(longitude, 6) != longitude)
            throw Invalid("Coordinates must be a valid latitude/longitude pair with up to six decimal places.");
    }

    private static void ValidatePlot(PlotInput input)
    {
        if (string.IsNullOrWhiteSpace(input.Name) || input.Name.Trim().Length is < 2 or > 100
            || input.AreaSqMeters < .01m || input.AreaSqMeters > 9999999999.99m
            || decimal.Round(input.AreaSqMeters, 2) != input.AreaSqMeters)
            throw Invalid("Use a valid plot name and positive area with up to two decimal places.");
    }

    private static FarmDetails Map(Farm farm, FarmAccessLevel level) =>
        new(farm.Id, farm.Name, farm.LocationText, farm.TimeZoneId,
            farm.Latitude, farm.Longitude, level.ToString(), Convert.ToBase64String(farm.RowVersion));

    private static PlotDetails Map(Plot plot) =>
        new(plot.Id, plot.FarmId, plot.Name, plot.AreaSqMeters, Convert.ToBase64String(plot.RowVersion));

    private static async Task Save(SmartFarmDbContext db, CancellationToken ct)
    {
        try
        {
            await db.SaveChangesAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            throw Conflict("This record changed. Reload and try again.");
        }
        catch (DbUpdateException error) when (error.InnerException is SqlException sql &&
            sql.Number is 2601 or 2627)
        {
            throw Conflict("This name or membership is already in use.");
        }
    }

    public async Task<IReadOnlyList<FarmSummary>> ListAsync(Guid actor, CancellationToken ct = default)
    {
        if (!await db.Users.AsNoTracking().AnyAsync(u => u.Id == actor && u.DisabledAtUtc == null, ct))
            return [];
        return await db.Farms.AsNoTracking()
            .Where(f => f.ArchivedAtUtc == null &&
                (f.OwnerUserId == actor || db.FarmMembers.Any(m =>
                    m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null)))
            .OrderBy(f => f.Name).Select(f => new FarmSummary(
                f.Id, f.Name, f.LocationText,
                f.OwnerUserId == actor ? "Owner" :
                db.FarmMembers.Where(m => m.FarmId == f.Id && m.UserId == actor && m.RemovedAtUtc == null)
                    .Select(m => m.Role).FirstOrDefault() == 1 ? "Manager" : "FieldWorker",
                f.TimeZoneId)).ToListAsync(ct);
    }

    public async Task<FarmDetails?> GetAsync(Guid farmId, Guid actor, CancellationToken ct = default)
    {
        var level = await access.GetAccessLevelAsync(farmId, actor, ct);
        if (!FarmAccessRules.Allows(level, FarmAction.View)) return null;
        var farm = await db.Farms.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Id == farmId && f.ArchivedAtUtc == null, ct);
        return farm is null ? null : Map(farm, level);
    }

    public async Task<FarmDetails> CreateAsync(Guid actor, FarmInput input, CancellationToken ct = default)
    {
        ValidateFarm(input);
        if (!await db.Users.AnyAsync(u => u.Id == actor && u.DisabledAtUtc == null, ct))
            throw Missing();
        var farm = new Farm
        {
            OwnerUserId = actor, Name = input.Name.Trim(), LocationText = input.LocationText.Trim(),
            TimeZoneId = input.TimeZoneId.Trim(), Latitude = input.Latitude, Longitude = input.Longitude
        };
        db.Farms.Add(farm);
        await Save(db, ct);
        return Map(farm, FarmAccessLevel.Owner);
    }

    public async Task<FarmDetails> UpdateAsync(Guid farmId, Guid actor, FarmInput input, CancellationToken ct = default)
    {
        ValidateFarm(input);
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var farm = await db.Farms.SingleOrDefaultAsync(f => f.Id == farmId && f.ArchivedAtUtc == null, ct)
            ?? throw Missing();
        db.Entry(farm).Property(f => f.RowVersion).OriginalValue = ParseVersion(input.RowVersion);
        farm.Name = input.Name.Trim();
        farm.LocationText = input.LocationText.Trim();
        farm.TimeZoneId = input.TimeZoneId.Trim();
        farm.Latitude = input.Latitude;
        farm.Longitude = input.Longitude;
        await Save(db, ct);
        return Map(farm, await access.GetAccessLevelAsync(farmId, actor, ct));
    }

    public async Task ArchiveAsync(Guid farmId, Guid actor, string rowVersion, CancellationToken ct = default)
    {
        if (await access.GetAccessLevelAsync(farmId, actor, ct) != FarmAccessLevel.Owner)
            throw Missing();
        var farm = await db.Farms.SingleOrDefaultAsync(f => f.Id == farmId && f.ArchivedAtUtc == null, ct)
            ?? throw Missing();
        if (await db.CropCycles.AnyAsync(c => c.Status == 1 &&
            db.Plots.Any(p => p.Id == c.PlotId && p.FarmId == farmId), ct))
            throw Conflict("End active crop cycles before archiving this farm.");
        db.Entry(farm).Property(f => f.RowVersion).OriginalValue = ParseVersion(rowVersion);
        farm.ArchivedAtUtc = DateTime.UtcNow;
        await Save(db, ct);
    }

    public async Task<IReadOnlyList<PlotDetails>> ListPlotsAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.View, ct);
        return await db.Plots.AsNoTracking().Where(p => p.FarmId == farmId && p.ArchivedAtUtc == null)
            .OrderBy(p => p.Name)
            .Select(p => new PlotDetails(p.Id, p.FarmId, p.Name, p.AreaSqMeters,
                Convert.ToBase64String(p.RowVersion))).ToListAsync(ct);
    }

    public async Task<PlotDetails?> GetPlotAsync(
        Guid farmId, Guid plotId, Guid actor, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.View, ct);
        var plot = await db.Plots.AsNoTracking()
            .SingleOrDefaultAsync(p => p.Id == plotId && p.FarmId == farmId && p.ArchivedAtUtc == null, ct);
        return plot is null ? null : Map(plot);
    }

    public async Task<PlotDetails> CreatePlotAsync(
        Guid farmId, Guid actor, PlotInput input, CancellationToken ct = default)
    {
        ValidatePlot(input);
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var plot = new Plot { FarmId = farmId, Name = input.Name.Trim(), AreaSqMeters = input.AreaSqMeters };
        db.Plots.Add(plot);
        await Save(db, ct);
        return Map(plot);
    }

    public async Task<PlotDetails> UpdatePlotAsync(
        Guid farmId, Guid plotId, Guid actor, PlotInput input, CancellationToken ct = default)
    {
        ValidatePlot(input);
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var plot = await db.Plots.SingleOrDefaultAsync(
            p => p.Id == plotId && p.FarmId == farmId && p.ArchivedAtUtc == null, ct) ?? throw Missing();
        db.Entry(plot).Property(p => p.RowVersion).OriginalValue = ParseVersion(input.RowVersion);
        plot.Name = input.Name.Trim();
        plot.AreaSqMeters = input.AreaSqMeters;
        await Save(db, ct);
        return Map(plot);
    }

    public async Task ArchivePlotAsync(
        Guid farmId, Guid plotId, Guid actor, string rowVersion, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var plot = await db.Plots.SingleOrDefaultAsync(
            p => p.Id == plotId && p.FarmId == farmId && p.ArchivedAtUtc == null, ct) ?? throw Missing();
        if (await db.CropCycles.AnyAsync(c => c.PlotId == plotId && c.Status == 1, ct))
            throw Conflict("End the active crop cycle before archiving this plot.");
        db.Entry(plot).Property(p => p.RowVersion).OriginalValue = ParseVersion(rowVersion);
        plot.ArchivedAtUtc = DateTime.UtcNow;
        await Save(db, ct);
    }

    public async Task<IReadOnlyList<FarmMemberDetails>> MembersAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.View, ct);
        return await db.FarmMembers.AsNoTracking()
            .Where(m => m.FarmId == farmId && m.RemovedAtUtc == null)
            .Join(db.Users, m => m.UserId, u => u.Id, (m, u) =>
                new FarmMemberDetails(m.Id, m.UserId, u.FullName, u.Email ?? "",
                    m.Role == 1 ? "Manager" : "FieldWorker"))
            .OrderBy(m => m.FullName).ToListAsync(ct);
    }

    private sealed record InvitationPayload(Guid FarmId, Guid OwnerId,
        Guid TargetUserId, byte Role, DateTimeOffset IssuedAtUtc);

    public async Task<InvitationDetails> CreateInvitationAsync(
        Guid farmId, Guid actor, InviteMemberInput input, CancellationToken ct = default)
    {
        if (await access.GetAccessLevelAsync(farmId, actor, ct) != FarmAccessLevel.Owner)
            throw Missing();
        if (input.Role is not (1 or 2) || string.IsNullOrWhiteSpace(input.Email))
            throw Invalid("Enter a registered email and a valid role.");
        var target = await users.FindByEmailAsync(input.Email.Trim());
        if (target is null || target.DisabledAtUtc is not null || target.Id == actor)
            throw Invalid("An eligible registered account is required.");
        if (await db.FarmMembers.AnyAsync(m =>
            m.FarmId == farmId && m.UserId == target.Id && m.RemovedAtUtc == null, ct))
            throw Conflict("This user already belongs to the farm.");

        var issued = DateTimeOffset.UtcNow;
        var payload = JsonSerializer.Serialize(new InvitationPayload(farmId, actor, target.Id, input.Role, issued));
        var token = _inviteTokens.Protect(payload, TimeSpan.FromHours(24));
        return new InvitationDetails(token, issued.AddHours(24), target.Email ?? "", input.Role == 1 ? "Manager" : "FieldWorker");
    }

    public async Task<Guid> AcceptInvitationAsync(Guid actor, string token, CancellationToken ct = default)
    {
        InvitationPayload? payload;
        try
        {
            payload = JsonSerializer.Deserialize<InvitationPayload>(_inviteTokens.Unprotect(token));
        }
        catch (Exception e) when (e is CryptographicException or FormatException or JsonException or ArgumentException)
        {
            throw Invalid("The invitation is invalid or expired.");
        }
        if (payload is null || payload.TargetUserId != actor || payload.Role is not (1 or 2))
            throw Invalid("The invitation does not belong to this account.");

        var farm = await db.Farms.AsNoTracking()
            .SingleOrDefaultAsync(f => f.Id == payload.FarmId && f.ArchivedAtUtc == null, ct);
        if (farm is null || farm.OwnerUserId != payload.OwnerId ||
            !await db.Users.AnyAsync(u => u.Id == actor && u.DisabledAtUtc == null, ct))
            throw Missing();

        // No invitation table exists in the SQL-first schema. Prior removal invalidates
        // a previously issued invitation to prevent an old link from restoring access.
        var oldMemberships = await db.FarmMembers
            .Where(m => m.FarmId == payload.FarmId && m.UserId == actor)
            .ToListAsync(ct);
        if (oldMemberships.Any(m => m.RemovedAtUtc is null))
            throw Conflict("Already a farm member.");
        if (oldMemberships.Any(m => m.RemovedAtUtc.HasValue &&
            new DateTimeOffset(DateTime.SpecifyKind(m.RemovedAtUtc.Value, DateTimeKind.Utc)) >= payload.IssuedAtUtc))
            throw Conflict("Invitation was revoked by a membership removal.");

        db.FarmMembers.Add(new FarmMember
        {
            FarmId = payload.FarmId, UserId = actor, Role = payload.Role
        });
        await Save(db, ct);
        return payload.FarmId;
    }

    public async Task ChangeMemberRoleAsync(
        Guid farmId, Guid memberId, Guid actor, byte role, CancellationToken ct = default)
    {
        if (role is not (1 or 2)) throw Invalid("Role must be Manager or Field Worker.");
        if (await access.GetAccessLevelAsync(farmId, actor, ct) != FarmAccessLevel.Owner)
            throw Missing();
        var member = await db.FarmMembers.SingleOrDefaultAsync(
            m => m.Id == memberId && m.FarmId == farmId && m.RemovedAtUtc == null, ct) ?? throw Missing();
        member.Role = role;
        await Save(db, ct);
    }

    public async Task RemoveMemberAsync(
        Guid farmId, Guid memberId, Guid actor, CancellationToken ct = default)
    {
        if (await access.GetAccessLevelAsync(farmId, actor, ct) != FarmAccessLevel.Owner)
            throw Missing();
        var member = await db.FarmMembers.SingleOrDefaultAsync(
            m => m.Id == memberId && m.FarmId == farmId && m.RemovedAtUtc == null, ct) ?? throw Missing();
        member.RemovedAtUtc = DateTime.UtcNow;
        await Save(db, ct);
    }
}

