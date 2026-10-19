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
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Security;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Services.Farms;

public sealed class FarmManagementService(
    IFarmRepository farms,
    IPlotRepository plots,
    IFarmMemberRepository members,
    ICropCycleRepository cycles,
    IInventoryItemRepository stock,
    IUserRepository people,
    IUnitOfWork unit,
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

    private async Task Save(CancellationToken ct)
    {
        try
        {
            await unit.SaveChangesAsync(ct);
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
        if (!await people.IsActiveAsync(actor, ct))
            return [];
        return await farms.ListSummariesAsync(actor, ct);
    }

    public async Task<FarmDetails?> GetAsync(Guid farmId, Guid actor, CancellationToken ct = default)
    {
        var level = await access.GetAccessLevelAsync(farmId, actor, ct);
        if (!FarmAccessRules.Allows(level, FarmAction.View)) return null;
        var farm = await farms.GetActiveAsync(farmId, ct);
        return farm is null ? null : Map(farm, level);
    }

    public async Task<FarmDetails> CreateAsync(Guid actor, FarmInput input, CancellationToken ct = default)
    {
        ValidateFarm(input);
        if (!await people.IsActiveAsync(actor, ct))
            throw Missing();
        var farm = new Farm
        {
            OwnerUserId = actor, Name = input.Name.Trim(), LocationText = input.LocationText.Trim(),
            TimeZoneId = input.TimeZoneId.Trim(), Latitude = input.Latitude, Longitude = input.Longitude
        };
        farms.Add(farm);
        await Save(ct);
        return Map(farm, FarmAccessLevel.Owner);
    }

    public async Task<FarmDetails> UpdateAsync(Guid farmId, Guid actor, FarmInput input, CancellationToken ct = default)
    {
        ValidateFarm(input);
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var farm = await farms.GetActiveAsync(farmId, ct) ?? throw Missing();
        farms.SetOriginalRowVersion(farm, ParseVersion(input.RowVersion));
        farm.Name = input.Name.Trim();
        farm.LocationText = input.LocationText.Trim();
        farm.TimeZoneId = input.TimeZoneId.Trim();
        farm.Latitude = input.Latitude;
        farm.Longitude = input.Longitude;
        await Save(ct);
        return Map(farm, await access.GetAccessLevelAsync(farmId, actor, ct));
    }

    public async Task ArchiveAsync(Guid farmId, Guid actor, string rowVersion, CancellationToken ct = default)
    {
        if (await access.GetAccessLevelAsync(farmId, actor, ct) != FarmAccessLevel.Owner)
            throw Missing();
        var farm = await farms.GetActiveAsync(farmId, ct) ?? throw Missing();
        if (await cycles.HasActiveForFarmAsync(farmId, ct))
            throw Conflict("End active crop cycles before archiving this farm.");
        if (await stock.HasPositiveStockAsync(farmId, ct))
            throw Conflict("Reduce or transfer stock before archiving this farm.");
        farms.SetOriginalRowVersion(farm, ParseVersion(rowVersion));
        farm.ArchivedAtUtc = DateTime.UtcNow;
        await Save(ct);
    }

    public async Task<IReadOnlyList<PlotDetails>> ListPlotsAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.View, ct);
        var listed = await plots.ListActiveAsync(farmId, ct);
        return listed.Select(Map).ToList();
    }

    public async Task<PlotDetails?> GetPlotAsync(
        Guid farmId, Guid plotId, Guid actor, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.View, ct);
        var plot = await plots.GetActiveAsync(farmId, plotId, ct);
        return plot is null ? null : Map(plot);
    }

    public async Task<PlotDetails> CreatePlotAsync(
        Guid farmId, Guid actor, PlotInput input, CancellationToken ct = default)
    {
        ValidatePlot(input);
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var plot = new Plot { FarmId = farmId, Name = input.Name.Trim(), AreaSqMeters = input.AreaSqMeters };
        plots.Add(plot);
        await Save(ct);
        return Map(plot);
    }

    public async Task<PlotDetails> UpdatePlotAsync(
        Guid farmId, Guid plotId, Guid actor, PlotInput input, CancellationToken ct = default)
    {
        ValidatePlot(input);
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var plot = await plots.GetActiveAsync(farmId, plotId, ct) ?? throw Missing();
        plots.SetOriginalRowVersion(plot, ParseVersion(input.RowVersion));
        plot.Name = input.Name.Trim();
        plot.AreaSqMeters = input.AreaSqMeters;
        await Save(ct);
        return Map(plot);
    }

    public async Task ArchivePlotAsync(
        Guid farmId, Guid plotId, Guid actor, string rowVersion, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.Manage, ct);
        var plot = await plots.GetActiveAsync(farmId, plotId, ct) ?? throw Missing();
        if (await cycles.HasStatusOnPlotAsync(plotId, 1, ct))
            throw Conflict("End the active crop cycle before archiving this plot.");
        plots.SetOriginalRowVersion(plot, ParseVersion(rowVersion));
        plot.ArchivedAtUtc = DateTime.UtcNow;
        await Save(ct);
    }

    public async Task<IReadOnlyList<FarmMemberDetails>> MembersAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await RequireAccess(farmId, actor, FarmAction.View, ct);
        return await members.ListDetailsAsync(farmId, ct);
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
        if (await members.GetActiveAsync(farmId, target.Id, ct) is not null)
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

        var farm = await farms.GetActiveAsync(payload.FarmId, ct);
        if (farm is null || farm.OwnerUserId != payload.OwnerId ||
            !await people.IsActiveAsync(actor, ct))
            throw Missing();

        // No invitation table exists in the SQL-first schema. Prior removal invalidates
        // a previously issued invitation to prevent an old link from restoring access.
        var oldMemberships = await members.ListForUserAsync(payload.FarmId, actor, ct);
        if (oldMemberships.Any(m => m.RemovedAtUtc is null))
            throw Conflict("Already a farm member.");
        if (oldMemberships.Any(m => m.RemovedAtUtc.HasValue &&
            new DateTimeOffset(DateTime.SpecifyKind(m.RemovedAtUtc.Value, DateTimeKind.Utc)) >= payload.IssuedAtUtc))
            throw Conflict("Invitation was revoked by a membership removal.");

        members.Add(new FarmMember
        {
            FarmId = payload.FarmId, UserId = actor, Role = payload.Role
        });
        await Save(ct);
        return payload.FarmId;
    }

    public async Task ChangeMemberRoleAsync(
        Guid farmId, Guid memberId, Guid actor, byte role, CancellationToken ct = default)
    {
        if (role is not (1 or 2)) throw Invalid("Role must be Manager or Field Worker.");
        if (await access.GetAccessLevelAsync(farmId, actor, ct) != FarmAccessLevel.Owner)
            throw Missing();
        var member = await members.GetByMemberIdAsync(farmId, memberId, ct) ?? throw Missing();
        member.Role = role;
        await Save(ct);
    }

    public async Task RemoveMemberAsync(
        Guid farmId, Guid memberId, Guid actor, CancellationToken ct = default)
    {
        if (await access.GetAccessLevelAsync(farmId, actor, ct) != FarmAccessLevel.Owner)
            throw Missing();
        var member = await members.GetByMemberIdAsync(farmId, memberId, ct) ?? throw Missing();
        member.RemovedAtUtc = DateTime.UtcNow;
        await Save(ct);
    }
}

