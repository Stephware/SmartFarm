# SmartFarm features source snapshot

> Source: Local SmartFarm repository at 850bfc7dab94675d5a70c13a95a9d00c481c1784
> Collected: 2026-10-27
> Published: Unknown

Read-only capture of repository file contents; not proof of runtime behavior.


## Repository file: src/SmartFarm.Core/Services/Crops/CropCycleRules.cs

using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Crops;

public static class CropCycleRules
{
    public static bool CanTransition(CropCycleStatus from, CropCycleStatus to) => (from, to) switch
    {
        (CropCycleStatus.Planned, CropCycleStatus.Active or CropCycleStatus.Cancelled) => true,
        (CropCycleStatus.Active, CropCycleStatus.Completed or CropCycleStatus.Failed or CropCycleStatus.Cancelled) => true,
        _ => false
    };

    public static bool IsTerminal(CropCycleStatus status) =>
        status is CropCycleStatus.Completed or CropCycleStatus.Failed or CropCycleStatus.Cancelled;
}



## Repository file: src/SmartFarm.Core/Services/Harvests/HarvestRules.cs

namespace SmartFarm.Core.Services.Harvests;

public static class HarvestRules
{
    public const decimal MaxQuantity = 999999999999999.999m;

    public static bool IsValidQuantity(decimal quantity) =>
        quantity > 0 && quantity <= MaxQuantity && decimal.Round(quantity, 3) == quantity;

    // The SQL-first design deliberately separates partial harvesting from
    // the crop cycle's explicit completion action.
    public static bool CanHarvest(byte cycleStatus) => cycleStatus == 1;

    public static bool IsValidVoidReason(string? reason) =>
        reason?.Trim().Length is >= 3 and <= 300;
}



## Repository file: src/SmartFarm.Core/Services/Inventory/StockRules.cs

using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Inventory;

public static class StockRules
{
    public const decimal MaxQuantity = 999999999999999.999m;

    public static bool IsValidQuantity(decimal value, bool allowZero = false) =>
        value >= 0 && value <= MaxQuantity &&
        decimal.Round(value, 3) == value &&
        (allowZero || value > 0);

    public static bool CanPost(StockTransactionType type, decimal change, Guid? activityId) =>
        change != 0 && decimal.Round(change, 3) == change &&
        change >= -MaxQuantity && change <= MaxQuantity &&
        (type switch
        {
            StockTransactionType.Receipt => change > 0 && activityId is null,
            StockTransactionType.Consumption => change < 0 && activityId is not null,
            StockTransactionType.Adjustment => activityId is null,
            _ => false
        });

    public static bool TryBalance(decimal current, decimal change, out decimal result)
    {
        result = 0;
        try
        {
            result = checked(current + change);
            return IsValidQuantity(result, allowZero: true);
        }
        catch (OverflowException) { return false; }
    }
}



## Repository file: src/SmartFarm.Core/Services/Monitoring/MonitoringRules.cs

namespace SmartFarm.Core.Services.Monitoring;

public static class MonitoringRules
{
    public const int MaxPhotoBytes = 5 * 1024 * 1024;

    public static bool ValidActivity(byte kind) => kind is >= 1 and <= 5;
    public static bool ValidHealth(byte health) => health <= 3;
    public static bool CanRecord(byte cropStatus) => cropStatus == 1;

    // Content type alone is untrusted. Accept only recognized file signatures
    // and match the declared type to prevent arbitrary executable uploads.
    public static string? PhotoExtension(ReadOnlySpan<byte> data, string? mime) =>
        mime?.Trim().ToLowerInvariant() switch
        {
            "image/jpeg" when data.Length >= 3 &&
                data[0] == 0xff && data[1] == 0xd8 && data[2] == 0xff => ".jpg",
            "image/png" when data.Length >= 8 &&
                data[..8].SequenceEqual(new byte[] {137,80,78,71,13,10,26,10}) => ".png",
            "image/webp" when data.Length >= 12 &&
                data[..4].SequenceEqual("RIFF"u8) &&
                data.Slice(8,4).SequenceEqual("WEBP"u8) => ".webp",
            _ => null
        };
}



## Repository file: src/SmartFarm.Core/Services/Reports/HarvestReportRules.cs

using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Core.Services.Reports;

public static class HarvestReportRules
{
    public static HarvestReport Build(Guid farmId, string farmName, string timeZoneId,
        DateOnly? from, DateOnly? to, Guid? plotId, Guid? cropTypeId,
        IEnumerable<HarvestReportEntry> harvests)
    {
        var entries = harvests.OrderByDescending(h => h.HarvestedAtUtc)
            .ThenByDescending(h => h.Id).ToList();
        var valid = entries.Where(h => !h.IsVoided).ToList();
        var units = valid.GroupBy(h => h.Unit)
            .Select(g => new HarvestUnitTotal(g.Key, g.Sum(h => h.Quantity), g.Count()))
            .OrderBy(g => g.Unit).ToList();
        var crops = valid.GroupBy(h => new
            { h.CropTypeId, h.CropTypeName, h.PlotId, h.PlotName, h.Unit })
            .Select(g => new HarvestCropPlotTotal(g.Key.CropTypeId, g.Key.CropTypeName,
                g.Key.PlotId, g.Key.PlotName, g.Key.Unit,
                g.Sum(h => h.Quantity), g.Count()))
            .OrderBy(g => g.CropTypeName).ThenBy(g => g.PlotName)
            .ThenBy(g => g.Unit).ToList();
        var monthly = valid.GroupBy(h => new
            { Month = h.FarmDate.ToString("yyyy-MM"), h.Unit })
            .Select(g => new HarvestMonthlyTotal(g.Key.Month, g.Key.Unit,
                g.Sum(h => h.Quantity), g.Count()))
            .OrderByDescending(g => g.Month).ThenBy(g => g.Unit).ToList();
        return new HarvestReport(farmId, farmName, timeZoneId,
            from, to, plotId, cropTypeId, valid.Count, entries.Count - valid.Count,
            units, crops, monthly, entries);
    }
}


## Repository file: src/SmartFarm.Core/Services/Security/FarmAccessRules.cs

using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Security;

public static class FarmAccessRules
{
    public static bool Allows(FarmAccessLevel level, FarmAction action) => action switch
    {
        FarmAction.View => level is FarmAccessLevel.Owner or FarmAccessLevel.Manager or FarmAccessLevel.FieldWorker,
        FarmAction.Manage => level is FarmAccessLevel.Owner or FarmAccessLevel.Manager,
        FarmAction.ManageMembers => level == FarmAccessLevel.Owner,
        _ => false
    };
}



## Repository file: src/SmartFarm.Core/Services/Tasks/TaskRules.cs

using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;

namespace SmartFarm.Core.Services.Tasks;

public static class TaskRules
{
    public static bool IsValidType(byte type) => type is >= 1 and <= 6;
    public static bool IsValidCloseStatus(byte status) => status is >= 1 and <= 3;

    public static string? Validate(FarmTaskInput input)
    {
        if (!IsValidType(input.TaskType) || input.DueDate == default)
            return "Choose a task type and due date.";
        if (input.Description?.Trim().Length > 500)
            return "Task description cannot exceed 500 characters.";
        if (input.CropCycleId == Guid.Empty || input.AssignedToUserId == Guid.Empty)
            return "Choose a valid crop and assignee.";
        return null;
    }

    public static byte? ForActivity(byte activityType) => activityType switch
    {
        1 => (byte)2, // Watering
        2 => (byte)3, // Fertilizing
        _ => null
    };

    public static string? ValidateUpdate(FarmTaskUpdateInput input)
    {
        if (input.DueDate == default || input.Description?.Trim().Length > 500 ||
            input.AssignedToUserId == Guid.Empty)
            return "Choose a valid due date, assignee, and description up to 500 characters.";
        return null;
    }

    // Recording actual work may fulfill a generated farm-calendar task.
    // Manual assignments remain limited to their assignee or farm managers.
    public static bool CanFulfillFromRecord(FarmTask task, Guid actor, bool mayManage) =>
        mayManage || task.AssignedToUserId == actor || task.DedupeKey is not null;

    public static bool Close(FarmTask task, byte status, Guid actor, DateTime utcNow)
    {
        if (task.Status != 0 || !IsValidCloseStatus(status)) return false;
        task.Status = status;
        task.ClosedAtUtc = utcNow;
        task.ClosedByUserId = actor;
        return true;
    }

    // Farm dates use their configured time zone, not the worker's UTC calendar day.
    public static DateOnly FarmDate(string timeZoneId, DateTime utcNow) =>
        DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
            utcNow, TimeZoneInfo.FindSystemTimeZoneById(timeZoneId)));

    // Create upcoming planting, watering and harvest dates from the crop calendar.
    public static IEnumerable<(byte Type, DateOnly Due)> ScheduledTasks(
        CropCycle crop, DateOnly today, DateOnly lastDay)
    {
        if (crop.Status == 0 && crop.PlannedPlantingDate <= lastDay)
            yield return (1, crop.PlannedPlantingDate);

        if (crop.Status != 1) yield break;

        if (crop.PlantedDate is { } planted && crop.WateringIntervalDays is > 0)
        {
            var interval = crop.WateringIntervalDays.Value;
            var days = today.DayNumber - planted.DayNumber;
            var next = Math.Max(1, (days + interval - 1) / interval);
            for (var due = planted.AddDays(next * interval); due <= lastDay; due = due.AddDays(interval))
            {
                if (due >= today) yield return (2, due);
            }
        }

        if (crop.ExpectedHarvestDate is { } harvest && harvest <= lastDay)
            yield return (5, harvest);
    }

    public static string ReminderKey(Guid taskId, DateOnly due) =>
        $"task:{taskId:N}:due:{due:yyyyMMdd}";

    public static bool IsReminderRelevant(FarmTask task, byte? cropStatus) =>
        task.Status == 0 && (task.CropCycleId is null ||
            (cropStatus is 0 or 1 && !(task.TaskType == 1 && cropStatus == 1)));

    // Deterministic keys keep recurring jobs safe to rerun.
    public static string ScheduleKey(Guid cropId, byte taskType, DateOnly due) =>
        $"cycle:{cropId:N}:type:{taskType}:date:{due:yyyyMMdd}";
}


## Repository file: src/SmartFarm.Core/Services/Tasks/WeatherRules.cs

using SmartFarm.Core.Contracts.Weather;

namespace SmartFarm.Core.Services.Tasks;

public sealed record WeatherWarning(byte Type, byte Severity, string Message);

public static class WeatherRules
{
    // Reject implausible provider values before writing decimal SQL columns.
    public static bool IsPlausible(DailyWeatherForecast day) =>
        (day.MinimumTemperatureC is null or >= -100m and <= 100m) &&
        (day.MaximumTemperatureC is null or >= -100m and <= 100m) &&
        (day.MinimumTemperatureC is null || day.MaximumTemperatureC is null ||
            day.MinimumTemperatureC <= day.MaximumTemperatureC) &&
        (day.RainfallMm is null or >= 0m and <= 10000m) &&
        (day.MaxPrecipitationProbability is null or >= 0m and <= 100m) &&
        (day.MaxWindKph is null or >= 0m and <= 300m);

    // Missing provider readings are not evidence that an existing warning has cleared.
    public static bool HasSignal(DailyWeatherForecast day, byte warningType) => warningType switch
    {
        1 => day.RainfallMm.HasValue,
        2 => day.MaxWindKph.HasValue,
        3 => day.MaximumTemperatureC.HasValue,
        _ => false
    };

    public static IReadOnlyList<WeatherWarning> Evaluate(DailyWeatherForecast day)
    {
        var results = new List<WeatherWarning>();
        if (day.RainfallMm is >= 20m)
            results.Add(new WeatherWarning(1, day.RainfallMm >= 50m ? (byte)3 : (byte)2,
                $"Heavy rain is forecast ({day.RainfallMm:0.#} mm). Check drainage and outdoor farm work."));
        if (day.MaxWindKph is >= 50m)
            results.Add(new WeatherWarning(2, day.MaxWindKph >= 80m ? (byte)3 : (byte)2,
                $"Strong wind is forecast ({day.MaxWindKph:0.#} km/h). Secure equipment and assess outdoor work."));
        if (day.MaximumTemperatureC is >= 35m)
            results.Add(new WeatherWarning(3, day.MaximumTemperatureC >= 40m ? (byte)3 : (byte)2,
                $"High temperature is forecast ({day.MaximumTemperatureC:0.#}°C). Monitor crop heat stress."));
        return results;
    }
}


## Repository file: src/SmartFarm.Infrastructure/Services/Crops/CropManagementService.cs

using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Crops;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Services.Crops;

public sealed class CropManagementService(
    ICropTypeRepository cropTypes,
    ICropCycleRepository cycles,
    IPlotRepository plots,
    IFarmTaskRepository tasks,
    IUnitOfWork unit,
    IFarmAccessService access) : ICropManagementService
{
    private static FarmOperationException NotFound() =>
        new(FarmError.NotFound, "Resource not found.");

    private static FarmOperationException Invalid(string description) =>
        new(FarmError.InvalidInput, description);

    private static FarmOperationException Conflict(string description) =>
        new(FarmError.Conflict, description);

    private async Task Require(Guid farmId, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, action, ct)) throw NotFound();
    }

    private static byte[] ParseVersion(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) throw Invalid("A current row version is required.");
        try
        {
            var bytes = Convert.FromBase64String(value);
            if (bytes.Length == 8) return bytes;
        }
        catch (FormatException) { }
        throw Invalid("Invalid row version.");
    }

    private static void ValidatePlan(CropCyclePlanInput input)
    {
        if (input.CropTypeId == Guid.Empty || input.PlannedPlantingDate == default)
            throw Invalid("Choose a crop type and planting date.");
        if (input.ExpectedHarvestDate is { } harvest && harvest < input.PlannedPlantingDate)
            throw Invalid("Expected harvest cannot precede planned planting.");
        if (input.WateringIntervalDays is <= 0 ||
            input.HarvestUnit?.Trim().Length > 20)
            throw Invalid("Invalid watering interval or harvest unit.");
    }

    private async Task Save(CancellationToken ct)
    {
        try
        {
            await unit.SaveChangesAsync(ct);
        }
        catch (DbUpdateConcurrencyException)
        {
            throw Conflict("This crop cycle has changed. Reload and retry.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number is 2601 or 2627)
        {
            throw Conflict("A crop with this name already exists or the plot already has an active crop.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number is 547)
        {
            throw Invalid("The crop operation violates an existing database rule.");
        }
    }

    private static CropTypeDetails Map(CropType type) => new(
        type.Id, type.Name, type.DefaultGrowthDays,
        type.DefaultWateringIntervalDays, type.DefaultHarvestUnit);

    private static CropCycleDetails Map(CropCycle cycle, string plotName, string typeName, Guid farmId) => new(
        cycle.Id, farmId, cycle.PlotId, plotName, cycle.CropTypeId, typeName,
        (CropCycleStatus)cycle.Status, cycle.PlannedPlantingDate,
        cycle.PlantedDate, cycle.ExpectedHarvestDate,
        cycle.HarvestUnit, cycle.WateringIntervalDays,
        cycle.CompletedAtUtc, cycle.TerminationReason,
        Convert.ToBase64String(cycle.RowVersion));

    private async Task<CropCycleDetails> MapLoaded(CropCycle cycle, Guid farmId, CancellationToken ct)
    {
        var plotName = await plots.GetNameAsync(cycle.PlotId, ct);
        var typeName = await cropTypes.GetNameAsync(cycle.CropTypeId, ct);
        return Map(cycle, plotName, typeName, farmId);
    }

    public async Task<IReadOnlyList<CropTypeDetails>> ListCropTypesAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var catalog = await cropTypes.ListActiveAsync(ct);
        return catalog.Select(Map).ToList();
    }

    public async Task<CropTypeDetails> CreateCropTypeAsync(
        Guid farmId, Guid actor, CropTypeInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var name = input.Name?.Trim() ?? "";
        var unit = input.DefaultHarvestUnit?.Trim() ?? "";
        if (name.Length is < 2 or > 100 || unit.Length is < 1 or > 20 ||
            input.DefaultGrowthDays is <= 0 || input.DefaultWateringIntervalDays is <= 0)
            throw Invalid("Enter a valid crop name, harvest unit, and positive intervals.");

        // CropType is a global shared catalog in the existing schema. To avoid
        // cross-farm edits, this phase permits only creation, never editing/deactivation.
        var type = new CropType
        {
            Name = name, DefaultHarvestUnit = unit,
            DefaultGrowthDays = input.DefaultGrowthDays,
            DefaultWateringIntervalDays = input.DefaultWateringIntervalDays,
            IsActive = true
        };
        cropTypes.Add(type);
        await Save(ct);
        return Map(type);
    }

    public async Task<IReadOnlyList<CropCycleDetails>> ListCyclesAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);

        // History includes completed, failed, cancelled and planned cycles;
        // exclude data from archived plots only when creating new cycles.
        var listed = await cycles.ListWithNamesForFarmAsync(farmId, ct);
        return listed.Select(c => Map(c.Cycle, c.PlotName, c.TypeName, farmId)).ToList();
    }

    public async Task<CropCycleDetails?> GetCycleAsync(
        Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, false, ct);
        return cycle is null ? null : await MapLoaded(cycle, farmId, ct);
    }

    public async Task<CropCycleDetails> PlanAsync(
        Guid farmId, Guid plotId, Guid actor, CropCyclePlanInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidatePlan(input);
        if (!await plots.IsActiveAsync(farmId, plotId, ct))
            throw NotFound();

        var type = await cropTypes.GetActiveAsync(input.CropTypeId, ct) ?? throw Invalid("Choose an active crop type.");

        var cycle = new CropCycle
        {
            PlotId = plotId, CropTypeId = type.Id,
            PlannedPlantingDate = input.PlannedPlantingDate,
            ExpectedHarvestDate = input.ExpectedHarvestDate,
            HarvestUnit = string.IsNullOrWhiteSpace(input.HarvestUnit) ? type.DefaultHarvestUnit : input.HarvestUnit.Trim(),
            WateringIntervalDays = input.WateringIntervalDays ?? type.DefaultWateringIntervalDays,
            Status = (byte)CropCycleStatus.Planned
        };
        cycles.Add(cycle);
        await Save(ct);
        return await MapLoaded(cycle, farmId, ct);
    }

    public async Task<CropCycleDetails> UpdatePlanAsync(
        Guid farmId, Guid cycleId, Guid actor, CropCyclePlanInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidatePlan(input);
        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, true, ct) ?? throw NotFound();
        if (cycle.Status != (byte)CropCycleStatus.Planned)
            throw Conflict("Only planned crops can be edited.");

        var type = await cropTypes.GetActiveAsync(input.CropTypeId, ct) ?? throw Invalid("Choose an active crop type.");
        cycles.SetOriginalRowVersion(cycle, ParseVersion(input.RowVersion));
        cycle.CropTypeId = type.Id;
        cycle.PlannedPlantingDate = input.PlannedPlantingDate;
        cycle.ExpectedHarvestDate = input.ExpectedHarvestDate;
        cycle.HarvestUnit = string.IsNullOrWhiteSpace(input.HarvestUnit) ? type.DefaultHarvestUnit : input.HarvestUnit.Trim();
        cycle.WateringIntervalDays = input.WateringIntervalDays ?? type.DefaultWateringIntervalDays;
        // Keep existing generated planting assignments aligned when the plan date moves.
        var changedTasks = await tasks.ListPendingForCycleAsync(cycleId, ct);
        var changedAt = DateTime.UtcNow;
        foreach (var task in changedTasks.Where(t => t.TaskType == 1 && t.DedupeKey is not null))
        {
            if (task.DueDate == cycle.PlannedPlantingDate) continue;
            task.DueDate = cycle.PlannedPlantingDate;
            task.DedupeKey = TaskRules.ScheduleKey(cycleId, 1, cycle.PlannedPlantingDate);
            await tasks.DismissRemindersAsync(task.Id, changedAt, ct);
        }
        await Save(ct);
        return await MapLoaded(cycle, farmId, ct);
    }

    public async Task<CropCycleDetails> TransitionAsync(
        Guid farmId, Guid cycleId, Guid actor, CropCycleTransitionInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, true, ct) ?? throw NotFound();

        var before = (CropCycleStatus)cycle.Status;
        var after = input.Status;
        if (!CropCycleRules.CanTransition(before, after))
            throw Conflict("This crop status change is not allowed.");
        if (after == CropCycleStatus.Active)
        {
            if (input.PlantedDate is not { } planted || planted == default)
                throw Invalid("Provide the actual planting date.");
            if (cycle.ExpectedHarvestDate is { } expected && expected < planted)
                throw Invalid("Expected harvest cannot precede actual planting. Edit the plan first.");
            cycle.PlantedDate = planted;
        }
        else if (input.PlantedDate is not null)
            throw Invalid("Only activation can set the planting date.");

        var reason = input.TerminationReason?.Trim();
        if (after is CropCycleStatus.Failed or CropCycleStatus.Cancelled &&
            (string.IsNullOrEmpty(reason) || reason.Length > 300))
            throw Invalid("A reason (up to 300 characters) is required to end a crop early.");
        if (after is CropCycleStatus.Active or CropCycleStatus.Completed &&
            !string.IsNullOrEmpty(reason))
            throw Invalid("A reason is only used for failed or cancelled crops.");

        cycles.SetOriginalRowVersion(cycle, ParseVersion(input.RowVersion));
        cycle.Status = (byte)after;
        cycle.TerminationReason = CropCycleRules.IsTerminal(after) ? reason : null;
        if (after == CropCycleStatus.Completed)
            cycle.CompletedAtUtc = DateTime.UtcNow;

        // Crop status and related task changes are saved in one transaction.
        if (after == CropCycleStatus.Active || CropCycleRules.IsTerminal(after))
        {
            var now = DateTime.UtcNow;
            var openTasks = await tasks.ListPendingForCycleAsync(cycleId, ct);
            foreach (var task in openTasks)
            {
                if (after == CropCycleStatus.Active && task.TaskType != 1) continue;
                var nextStatus = after == CropCycleStatus.Active ? (byte)1 : (byte)3;
                if (TaskRules.Close(task, nextStatus, actor, now))
                    await tasks.DismissRemindersAsync(task.Id, now, ct);
            }
        }

        // SQL has UX_CropCycle_OneActivePerPlot (filtered Status=1).
        // A second activation is rejected by the unique index, even under races.
        await Save(ct);
        return await MapLoaded(cycle, farmId, ct);
    }
}



## Repository file: src/SmartFarm.Infrastructure/Services/Farms/FarmEnrollmentService.cs

using System.Data;
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
        var result = new FarmEnrollmentOverview(farm.JoinCode, await requests.ListPendingAsync(farmId, ct));
        await transaction.CommitAsync(ct);
        return result;
    }

    public async Task DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, CancellationToken ct = default)
    {
        await using var transaction = await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        await RequireOwner(farmId, actor, ct);
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
            await Save(ct);
        }
        await transaction.CommitAsync(ct);
    }

    private async Task Save(CancellationToken ct)
    {
        try { await unit.SaveChangesAsync(ct); }
        catch (DbUpdateException error) when (error.InnerException is SqlException { Number: 2601 or 2627 })
        {
            throw new FarmOperationException(FarmError.Conflict, "This enrollment changed. Reload and try again.");
        }
    }
}


## Repository file: src/SmartFarm.Infrastructure/Services/Farms/FarmManagementService.cs

using System.Security.Cryptography;
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

namespace SmartFarm.Infrastructure.Services.Farms;

public sealed class FarmManagementService(
    IFarmRepository farms,
    IPlotRepository plots,
    IFarmMemberRepository members,
    ICropCycleRepository cycles,
    IInventoryItemRepository stock,
    IUserRepository people,
    IUnitOfWork unit,
    IFarmAccessService access) : IFarmManagementService
{
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
        // Reject invalid IANA/Windows time zones before storing a farm that jobs cannot process.
        try
        {
            _ = TimeZoneInfo.FindSystemTimeZoneById(input.TimeZoneId.Trim());
        }
        catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
        {
            throw Invalid("Use a recognized time zone, such as Asia/Manila.");
        }
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

    private async Task Save(CancellationToken ct, Farm? newFarm = null)
    {
        try
        {
            for (var attempt = 0; ; attempt++)
            {
                try
                {
                    await unit.SaveChangesAsync(ct);
                    break;
                }
                catch (DbUpdateException error) when (newFarm is not null && attempt < 4 &&
                    error.InnerException is SqlException { Number: 2601 or 2627 } sql &&
                    sql.Message.Contains("UX_Farm_JoinCode", StringComparison.Ordinal))
                {
                    newFarm.JoinCode = Convert.ToHexString(RandomNumberGenerator.GetBytes(6));
                }
            }
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
        await Save(ct, farm);
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

    public Task<InvitationDetails> CreateInvitationAsync(
        Guid farmId, Guid actor, InviteMemberInput input, CancellationToken ct = default) =>
        throw Invalid("Invitation links are retired. Use a farm code and owner approval.");

    public Task<Guid> AcceptInvitationAsync(Guid actor, string token, CancellationToken ct = default) =>
        throw Invalid("Invitation links are retired. Use a farm code and owner approval.");

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



## Repository file: src/SmartFarm.Infrastructure/Services/Harvests/HarvestService.cs

using System.Data;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Harvests;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Services.Harvests;

public sealed class HarvestService(
    ICropCycleRepository cycles,
    IPlotRepository plots,
    ICropTypeRepository cropTypes,
    IHarvestRepository harvests,
    IFarmTaskRepository tasks,
    IUnitOfWork unit,
    IFarmAccessService access) : IHarvestService
{
    private static FarmOperationException Missing() =>
        new(FarmError.NotFound, "Harvest resource not found.");
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);
    private static FarmOperationException Conflict(string message) =>
        new(FarmError.Conflict, message);

    private async Task Require(Guid farmId, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, action, ct))
            throw Missing();
    }

    private Task<CropCycle?> FindCycle(Guid farmId, Guid cycleId, bool activePlot, CancellationToken ct) =>
        cycles.GetForFarmAsync(farmId, cycleId, activePlot, ct);

    private static HarvestEntry Map(Harvest h) =>
        new(h.Id, h.CropCycleId, h.Quantity, h.Unit, h.HarvestedAtUtc,
            h.RecordedAtUtc, h.RecordedByUserId, h.Notes,
            h.VoidedAtUtc is not null, h.VoidedAtUtc, h.VoidedByUserId,
            h.VoidReason);

    public async Task<HarvestOverview> GetAsync(
        Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var cycle = await FindCycle(farmId, cycleId, false, ct) ?? throw Missing();
        var plotName = await plots.GetNameAsync(cycle.PlotId, ct);
        var cropName = await cropTypes.GetNameAsync(cycle.CropTypeId, ct);
        var records = (await harvests.ListForCycleAsync(cycleId, ct)).Select(Map).ToList();

        var valid = records.Where(h => !h.IsVoided).ToList();
        var permission = await access.GetAccessLevelAsync(farmId, actor, ct);
        var activePlot = await plots.IsActiveAsync(farmId, cycle.PlotId, ct);
        return new HarvestOverview(farmId, cycleId, cropName, plotName,
            cycle.HarvestUnit, cycle.Status,
            activePlot && HarvestRules.CanHarvest(cycle.Status),
            permission is FarmAccessLevel.Owner or FarmAccessLevel.Manager,
            valid.Sum(h => h.Quantity), valid.Count, records.Count - valid.Count, records);
    }

    public async Task<HarvestEntry> RecordAsync(
        Guid farmId, Guid cycleId, Guid actor, RecordHarvestInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct); // Field Workers may record partial harvests.
        if (!HarvestRules.IsValidQuantity(input.Quantity) || input.Notes?.Length > 1000)
            throw Invalid("Use a positive harvest quantity with at most 3 decimal places and notes up to 1000 characters.");

        // Hold the crop cycle read lock until the harvest insert commits. A
        // concurrent completion must not slip between checking Active and saving.
        // A sync request already has a serializable transaction that also saves its
        // idempotency record. Do not open a nested transaction inside that request.
        var transaction = !unit.HasActiveTransaction
            ? await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct)
            : null;
        try
        {
            var cycle = await FindCycle(farmId, cycleId, true, ct) ?? throw Missing();
            if (!HarvestRules.CanHarvest(cycle.Status))
                throw Conflict("Record harvests while the crop cycle is Active. Completion is a separate action.");

            var entry = new Harvest
            {
                CropCycleId = cycleId, HarvestedAtUtc = DateTime.UtcNow,
                Quantity = input.Quantity, Unit = cycle.HarvestUnit,
                RecordedByUserId = actor,
                Notes = string.IsNullOrWhiteSpace(input.Notes) ? null : input.Notes.Trim()
            };
            harvests.Add(entry);

            // A recorded harvest fulfills the nearest open harvest task for this crop,
            // including an early harvest before its scheduled due date.
            var timezone = await tasks.GetFarmTimeZoneAsync(farmId, ct);
            if (timezone is not null)
            {
                DateOnly today;
                try { today = TaskRules.FarmDate(timezone, entry.HarvestedAtUtc); }
                catch (TimeZoneNotFoundException) { today = DateOnly.FromDateTime(entry.HarvestedAtUtc); }

                var level = await access.GetAccessLevelAsync(farmId, actor, ct);
                var mayManage = level is FarmAccessLevel.Owner or FarmAccessLevel.Manager;
                var task = await tasks.FindDueAsync(farmId, cycleId, 5, today, actor, mayManage, ct);
                if (task is not null && TaskRules.Close(task, 1, actor, entry.HarvestedAtUtc))
                    await tasks.DismissRemindersAsync(task.Id, entry.HarvestedAtUtc, ct);
            }

            await unit.SaveChangesAsync(ct);
            if (transaction is not null)
                await transaction.CommitAsync(ct);
            return Map(entry);
        }
        catch (DbUpdateConcurrencyException)
        {
            throw Conflict("The related task was changed. Refresh and retry the harvest.");
        }
        catch (DbUpdateException ex) when (ex.InnerException is SqlException sql && sql.Number == 547)
        {
            throw Invalid("The harvest violates the existing SQL schema constraints.");
        }
        catch (DbUpdateException ex) when (ex.InnerException is SqlException sql && sql.Number == 1205)
        {
            throw Conflict("Harvest was not saved due to a concurrent update. Refresh before retrying.");
        }
        catch (SqlException ex) when (ex.Number == 1205)
        {
            throw Conflict("Harvest was not saved due to a concurrent update. Please retry.");
        }
        finally
        {
            if (transaction is not null)
                await transaction.DisposeAsync();
        }
    }

    public async Task<HarvestEntry> VoidAsync(
        Guid farmId, Guid cycleId, Guid harvestId, Guid actor,
        VoidHarvestInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (!HarvestRules.IsValidVoidReason(input.Reason))
            throw Invalid("Provide a reason of 3–300 characters.");

        // Read-only verification; the database UPDATE condition below ensures
        // two void requests cannot both succeed on the same harvest.
        if (await FindCycle(farmId, cycleId, false, ct) is null) throw Missing();
        var original = await harvests.GetByIdAsync(cycleId, harvestId, ct) ?? throw Missing();
        if (original.VoidedAtUtc is not null) throw Conflict("This harvest is already voided.");

        var when = DateTime.UtcNow;
        var voided = await harvests.TryVoidAsync(cycleId, harvestId, actor, when, input.Reason.Trim(), ct);
        if (!voided) throw Conflict("This harvest was already voided. Refresh the history.");

        return Map(new Harvest
        {
            Id = original.Id, CropCycleId = original.CropCycleId,
            Quantity = original.Quantity, Unit = original.Unit,
            HarvestedAtUtc = original.HarvestedAtUtc, RecordedAtUtc = original.RecordedAtUtc,
            RecordedByUserId = original.RecordedByUserId, Notes = original.Notes,
            VoidedAtUtc = when, VoidedByUserId = actor, VoidReason = input.Reason.Trim()
        });
    }
}



## Repository file: src/SmartFarm.Infrastructure/Services/Inventory/InventoryService.cs

using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Inventory;

namespace SmartFarm.Infrastructure.Services.Inventory;

public sealed class InventoryService(
    IInventoryItemRepository items,
    IInventoryTransactionRepository transactions,
    ICropActivityRepository activities,
    IUnitOfWork unit,
    IFarmAccessService access) : IInventoryService
{
    private static FarmOperationException Missing() =>
        new(FarmError.NotFound, "Inventory resource not found.");
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);
    private static FarmOperationException Conflict(string message) =>
        new(FarmError.Conflict, message);

    private async Task Require(Guid farmId, Guid actor, FarmAction permission, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, permission, ct)) throw Missing();
    }

    private static byte[] Version(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) throw Invalid("A current rowVersion is required.");
        try
        {
            var bytes = Convert.FromBase64String(value);
            if (bytes.Length == 8) return bytes;
        }
        catch (FormatException) { }
        throw Invalid("Invalid rowVersion.");
    }

    private static string? Optional(string? text) =>
        string.IsNullOrWhiteSpace(text) ? null : text.Trim();

    private static void ValidateItem(InventoryItemInput input, bool creating)
    {
        if (input.Name?.Trim().Length is not (>= 2 and <= 120) ||
            input.Unit?.Trim().Length is not (>= 1 and <= 20) ||
            !Enum.IsDefined(input.Category) ||
            input.Description?.Length > 500 ||
            input.ReorderLevel is { } reorder && !StockRules.IsValidQuantity(reorder, allowZero: true) ||
            creating && !StockRules.IsValidQuantity(input.InitialQuantity, allowZero: true))
            throw Invalid("Invalid inventory name, category, unit, or stock settings.");

        if (!creating && input.InitialQuantity != 0)
            throw Invalid("Update stock through a transaction, not item details.");
    }

    private static InventoryItemDetails Map(InventoryItem item) => new(
        item.Id, item.FarmId, item.Name, (InventoryCategory)item.Category,
        item.Unit, item.Description, item.QuantityOnHand, item.ReorderLevel,
        item.ArchivedAtUtc is null && item.ReorderLevel is { } level &&
            item.QuantityOnHand <= level,
        item.ArchivedAtUtc,
        Convert.ToBase64String(item.RowVersion));

    private static InventoryTransactionDetails Map(InventoryTransaction item) => new(
        item.Id, item.InventoryItemId, item.CropActivityId,
        item.ReversesTransactionId, (StockTransactionType)item.TransactionType,
        item.QuantityChange, item.OccurredAtUtc, item.RecordedAtUtc,
        item.RecordedByUserId, item.Reason);

    private async Task Save(CancellationToken ct)
    {
        try { await unit.SaveChangesAsync(ct); }
        catch (DbUpdateConcurrencyException)
        {
            throw Conflict("Inventory changed. Reload and retry with the latest rowVersion.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number is 2601 or 2627)
        {
            throw Conflict("This inventory operation is already recorded.");
        }
        catch (DbUpdateException exception) when (exception.InnerException is SqlException sql &&
            sql.Number == 547)
        {
            throw Invalid("The inventory change violates an existing database rule.");
        }
    }

    private async Task<InventoryItem> ActiveItem(Guid farmId, Guid itemId, CancellationToken ct) =>
        await items.GetActiveAsync(farmId, itemId, ct) ?? throw Missing();


    public async Task<IReadOnlyList<InventoryActivityOption>> ActivityOptionsAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var available = await activities.ListActiveFarmOptionsAsync(farmId, ct);

        return available.Select(a => new InventoryActivityOption(a.Id,
            a.PlotName + " · Activity " + a.ActivityType + " · " +
            a.OccurredAtUtc.ToString("yyyy-MM-dd HH:mm") + " UTC")).ToList();
    }

    public async Task<InventoryWorkspace> ListAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var listed = await items.ListActiveAsync(farmId, ct);
        var mapped = listed.Select(Map).ToList();
        return new InventoryWorkspace(mapped, mapped.Count(x => x.IsLowStock));
    }

    public async Task<InventoryItemDetails?> GetAsync(
        Guid farmId, Guid itemId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var item = await items.GetByIdAsync(farmId, itemId, ct);
        return item is null ? null : Map(item);
    }

    public async Task<InventoryItemDetails> CreateAsync(
        Guid farmId, Guid actor, InventoryItemInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidateItem(input, creating: true);
        var item = new InventoryItem
        {
            FarmId = farmId,
            Name = input.Name.Trim(), Category = (byte)input.Category,
            Unit = input.Unit.Trim(), Description = Optional(input.Description),
            ReorderLevel = input.ReorderLevel, QuantityOnHand = input.InitialQuantity,
            MetadataVersion = Guid.NewGuid()
        };
        items.Add(item);
        if (input.InitialQuantity > 0)
        {
            transactions.Add(new InventoryTransaction
            {
                InventoryItemId = item.Id,
                RecordedByUserId = actor, OccurredAtUtc = DateTime.UtcNow,
                QuantityChange = input.InitialQuantity,
                TransactionType = (byte)StockTransactionType.Opening,
                Reason = "Initial stock"
            });
        }
        // EF Core SaveChanges is transactional for the item and opening entry.
        await Save(ct);
        return Map(item);
    }

    public async Task<InventoryItemDetails> UpdateAsync(
        Guid farmId, Guid itemId, Guid actor, InventoryItemInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        ValidateItem(input, creating: false);
        var item = await ActiveItem(farmId, itemId, ct);
        if (!string.Equals(item.Unit, input.Unit.Trim(), StringComparison.OrdinalIgnoreCase) &&
            await transactions.AnyForItemAsync(itemId, ct))
            throw Conflict("Units cannot be changed once stock transactions exist.");
        items.SetOriginalRowVersion(item, Version(input.RowVersion));
        item.Name = input.Name.Trim();
        item.Category = (byte)input.Category;
        item.Unit = input.Unit.Trim();
        item.Description = Optional(input.Description);
        item.ReorderLevel = input.ReorderLevel;
        item.MetadataVersion = Guid.NewGuid();
        await Save(ct);
        return Map(item);
    }

    public async Task ArchiveAsync(
        Guid farmId, Guid itemId, Guid actor, string rowVersion, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        var item = await ActiveItem(farmId, itemId, ct);
        if (item.QuantityOnHand != 0)
            throw Conflict("Reduce stock to zero before archiving an inventory item.");
        items.SetOriginalRowVersion(item, Version(rowVersion));
        item.ArchivedAtUtc = DateTime.UtcNow;
        item.MetadataVersion = Guid.NewGuid();
        await Save(ct);
    }

    public async Task<IReadOnlyList<InventoryTransactionDetails>> HistoryAsync(
        Guid farmId, Guid itemId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        if (await items.GetByIdAsync(farmId, itemId, ct) is null)
            throw Missing();
        return (await transactions.ListForItemAsync(itemId, ct)).Select(Map).ToList();
    }

    public async Task<InventoryItemDetails> PostAsync(
        Guid farmId, Guid itemId, Guid actor, InventoryMovementInput input, CancellationToken ct = default)
    {
        // Field Workers can record consumption when linked to an activity in
        // their farm; receipts and adjustments require Owner/Manager.
        var type = input.TransactionType;
        await Require(farmId, actor, type == StockTransactionType.Consumption
            ? FarmAction.View : FarmAction.Manage, ct);
        if (!StockRules.CanPost(type, input.QuantityChange, input.CropActivityId) ||
            input.Reason?.Length > 300)
            throw Invalid("Invalid stock movement, quantity, activity, or reason.");

        var item = await ActiveItem(farmId, itemId, ct);
        if (type == StockTransactionType.Consumption)
        {
            var activityId = input.CropActivityId!.Value;
            var isOwnActivity = await activities.BelongsToActiveFarmCycleAsync(farmId, activityId, ct);
            if (!isOwnActivity) throw Invalid("Consumption must refer to an active-crop activity from this farm.");
        }
        else if (type == StockTransactionType.Adjustment &&
            string.IsNullOrWhiteSpace(input.Reason))
            throw Invalid("Stock adjustments require a reason.");

        if (!StockRules.TryBalance(item.QuantityOnHand, input.QuantityChange, out var next))
            throw Conflict("Insufficient stock or quantity exceeds SQL precision.");

        items.SetOriginalRowVersion(item, Version(input.RowVersion));
        item.QuantityOnHand = next;
        transactions.Add(new InventoryTransaction
        {
            InventoryItemId = itemId, CropActivityId = input.CropActivityId,
            RecordedByUserId = actor, TransactionType = (byte)type,
            QuantityChange = input.QuantityChange, OccurredAtUtc = DateTime.UtcNow,
            Reason = Optional(input.Reason)
        });
        await Save(ct);
        return Map(item);
    }

    public async Task<InventoryItemDetails> ReverseAsync(
        Guid farmId, Guid itemId, Guid transactionId, Guid actor,
        InventoryReversalInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (input.Reason?.Trim().Length is not (>= 3 and <= 300))
            throw Invalid("Provide a reversal reason of 3–300 characters.");

        var item = await ActiveItem(farmId, itemId, ct);
        var original = await transactions.GetByIdAsync(itemId, transactionId, ct) ?? throw Missing();
        if (original.TransactionType == (byte)StockTransactionType.Reversal)
            throw Conflict("Cannot reverse a reversal.");
        if (await transactions.HasReversalAsync(transactionId, ct))
            throw Conflict("This transaction has already been reversed.");
        if (!StockRules.TryBalance(item.QuantityOnHand, -original.QuantityChange, out var next))
            throw Conflict("Reversal would produce invalid or insufficient stock.");

        items.SetOriginalRowVersion(item, Version(input.RowVersion));
        item.QuantityOnHand = next;
        transactions.Add(new InventoryTransaction
        {
            InventoryItemId = itemId, TransactionType = (byte)StockTransactionType.Reversal,
            ReversesTransactionId = original.Id, QuantityChange = -original.QuantityChange,
            RecordedByUserId = actor, OccurredAtUtc = DateTime.UtcNow,
            Reason = input.Reason.Trim()
        });
        await Save(ct);
        return Map(item);
    }
}




## Repository file: src/SmartFarm.Infrastructure/Services/Monitoring/LocalObservationPhotoStore.cs

using SmartFarm.Core.Interfaces.Monitoring;

namespace SmartFarm.Infrastructure.Services.Monitoring;

// Local, private storage (outside wwwroot). Configure shared durable storage
// before using multiple production instances; files are not served statically.
public sealed class LocalObservationPhotoStore(string root) : IObservationPhotoStore
{
    private readonly string _directory = Path.GetFullPath(root);

    private static bool IsSafeKey(string key)
    {
        if (key.Length is not (36 or 37)) return false; // 32 hex chars + extension
        var dot = key.IndexOf('.');
        return dot == 32 &&
            key[..32].All(Uri.IsHexDigit) &&
            key[dot..] is ".jpg" or ".png" or ".webp";
    }

    private string FilePath(string key) =>
        IsSafeKey(key) ? Path.Combine(_directory, key) :
        throw new FileNotFoundException("Photo was not found.");

    public async Task<string> SaveAsync(byte[] bytes, string extension, CancellationToken ct = default)
    {
        if (extension is not (".jpg" or ".png" or ".webp"))
            throw new ArgumentException("Unsupported photo extension.", nameof(extension));
        Directory.CreateDirectory(_directory);
        var key = Guid.NewGuid().ToString("N") + extension;
        var full = FilePath(key);
        await using var output = new FileStream(full, FileMode.CreateNew,
            FileAccess.Write, FileShare.None, 81920, useAsync: true);
        try { await output.WriteAsync(bytes, ct); }
        catch
        {
            await output.DisposeAsync();
            File.Delete(full);
            throw;
        }
        return key;
    }

    public Task<Stream> OpenAsync(string storageKey, CancellationToken ct = default) =>
        Task.FromResult<Stream>(new FileStream(FilePath(storageKey),
            FileMode.Open, FileAccess.Read, FileShare.Read, 81920, useAsync: true));

    public Task DeleteAsync(string storageKey)
    {
        File.Delete(FilePath(storageKey));
        return Task.CompletedTask;
    }
}



## Repository file: src/SmartFarm.Infrastructure/Services/Monitoring/MonitoringService.cs

using System.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.Data.SqlClient;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Monitoring;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Services.Monitoring;

public sealed class MonitoringService(
    ICropCycleRepository cycles,
    ICropActivityRepository activities,
    IFarmTaskRepository tasks,
    ICropObservationRepository observations,
    IObservationPhotoRepository photos,
    IUnitOfWork unit,
    IFarmAccessService access,
    IObservationPhotoStore photoStore) : IMonitoringService
{
    private static FarmOperationException Missing() =>
        new(FarmError.NotFound, "Resource not found.");

    private static FarmOperationException Invalid(string msg) =>
        new(FarmError.InvalidInput, msg);
    private static FarmOperationException Conflict() =>
        new(FarmError.Conflict, "This farm task changed. Reload before recording the activity.");

    private static ActivityDetails Map(CropActivity a) =>
        new(a.Id, a.ActivityType, a.OccurredAtUtc, a.RecordedAtUtc,
            a.RecordedByUserId, a.Notes, a.FarmTaskId);

    private static PhotoDetails Map(ObservationPhoto p) =>
        new(p.Id, p.CropObservationId, p.ContentType, p.FileSizeBytes, p.UploadedAtUtc);

    private static ObservationDetails Map(CropObservation o, IReadOnlyList<PhotoDetails> photos) =>
        new(o.Id, o.HealthStatus, o.GrowthStage, o.ObservedAtUtc, o.RecordedAtUtc,
            o.RecordedByUserId, o.Notes, photos);

    private async Task<CropCycle> RequireCycle(Guid farmId, Guid cycleId, Guid actor,
        bool recording, CancellationToken ct)
    {
        // Worker recording is intentionally allowed on active crops,
        // unlike planning/status changes, which require Owner/Manager.
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct))
            throw Missing();

        var cycle = await cycles.GetForFarmAsync(farmId, cycleId, recording, ct)
            ?? throw Missing();

        if (recording && !MonitoringRules.CanRecord(cycle.Status))
            throw Invalid("Monitoring records can only be added to an active crop cycle.");
        return cycle;
    }

    public async Task<MonitoringDetails> GetAsync(
        Guid farmId, Guid cycleId, Guid actor, CancellationToken ct = default)
    {
        var cycle = await RequireCycle(farmId, cycleId, actor, false, ct);
        var activityRows = await activities.ListForCycleAsync(cycleId, ct);
        var observationRows = await observations.ListForCycleAsync(cycleId, ct);

        var observationIds = observationRows.Select(o => o.Id).ToArray();
        var photoRows = await photos.ListForObservationsAsync(observationIds, ct);

        var mappedPhotos = photoRows.GroupBy(MapObservationId)
            .ToDictionary(g => g.Key, g => (IReadOnlyList<PhotoDetails>)g.Select(Map).ToList());
        return new MonitoringDetails(farmId, cycleId, MonitoringRules.CanRecord(cycle.Status),
            activityRows.Select(Map).ToList(),
            observationRows.Select(o => Map(o,
                mappedPhotos.GetValueOrDefault(o.Id) ?? [])).ToList());
    }

    private static Guid MapObservationId(ObservationPhoto photo) => photo.CropObservationId;

    public async Task<ActivityDetails> RecordActivityAsync(
        Guid farmId, Guid cycleId, Guid actor, RecordActivityInput input, CancellationToken ct = default)
    {
        // Keep the active-crop check, task completion and activity insert together.
        // Offline sync already provides its own transaction and must not nest another.
        var transaction = !unit.HasActiveTransaction
            ? await unit.BeginTransactionAsync(IsolationLevel.Serializable, ct)
            : null;
        try
        {
            await RequireCycle(farmId, cycleId, actor, true, ct);
            if (!MonitoringRules.ValidActivity(input.ActivityType) || input.Notes?.Length > 1000 ||
                input.FarmTaskId == Guid.Empty)
                throw Invalid("Choose a valid activity, task and notes up to 1000 characters.");

            var now = DateTime.UtcNow;
            var level = await access.GetAccessLevelAsync(farmId, actor, ct);
            var mayManage = level is FarmAccessLevel.Owner or FarmAccessLevel.Manager;
            var taskType = TaskRules.ForActivity(input.ActivityType);
            FarmTask? linked = null;
            if (input.FarmTaskId is { } selectedId)
            {
                linked = await tasks.FindAsync(farmId, selectedId, ct);
                if (linked is null || taskType is null || linked.CropCycleId != cycleId ||
                    linked.TaskType != taskType.Value ||
                    !TaskRules.CanFulfillFromRecord(linked, actor, mayManage))
                    throw Invalid("The selected task does not match this crop activity.");
                if (linked.Status != 0) throw Conflict();
            }
            else if (taskType is { } dueType)
            {
                var timezone = await tasks.GetFarmTimeZoneAsync(farmId, ct);
                if (timezone is not null)
                {
                    DateOnly today;
                    try { today = TaskRules.FarmDate(timezone, now); }
                    catch (TimeZoneNotFoundException)
                    {
                        today = DateOnly.FromDateTime(now);
                    }
                    linked = await tasks.FindDueAsync(farmId, cycleId, dueType, today, actor, mayManage, ct);
                }
            }

            var activity = new CropActivity
            {
                CropCycleId = cycleId, FarmTaskId = linked?.Id,
                ActivityType = input.ActivityType, OccurredAtUtc = now,
                RecordedByUserId = actor,
                Notes = string.IsNullOrWhiteSpace(input.Notes) ? null : input.Notes.Trim()
            };
            activities.Add(activity);
            if (linked is not null && TaskRules.Close(linked, 1, actor, now))
                await tasks.DismissRemindersAsync(linked.Id, now, ct);
            try { await unit.SaveChangesAsync(ct); }
            catch (DbUpdateConcurrencyException) { throw Conflict(); }
            catch (DbUpdateException e) when (e.InnerException is SqlException { Number: 1205 })
            {
                throw Conflict();
            }
            if (transaction is not null)
                await transaction.CommitAsync(ct);
            return Map(activity);
        }
        finally
        {
            if (transaction is not null)
                await transaction.DisposeAsync();
        }
    }

    public async Task<ObservationDetails> RecordObservationAsync(
        Guid farmId, Guid cycleId, Guid actor, RecordObservationInput input, CancellationToken ct = default)
    {
        await RequireCycle(farmId, cycleId, actor, true, ct);
        if (!MonitoringRules.ValidHealth(input.HealthStatus) ||
            input.GrowthStage?.Trim().Length > 40 || input.Notes?.Length > 1000)
            throw Invalid("Choose valid health, growth stage and notes.");
        var observation = new CropObservation
        {
            CropCycleId = cycleId, HealthStatus = input.HealthStatus,
            GrowthStage = string.IsNullOrWhiteSpace(input.GrowthStage) ? null : input.GrowthStage.Trim(),
            ObservedAtUtc = DateTime.UtcNow,
            RecordedByUserId = actor,
            Notes = string.IsNullOrWhiteSpace(input.Notes) ? null : input.Notes.Trim()
        };
        observations.Add(observation);
        await unit.SaveChangesAsync(ct);
        return Map(observation, []);
    }

    public Task<PhotoDetails> AddPhotoAsync(Guid farmId, Guid cycleId,
        Guid observationId, Guid actor, Stream stream, long length,
        string? claimedContentType, CancellationToken ct = default) =>
        SavePhotoAsync(farmId, cycleId, observationId, null, actor,
            stream, length, claimedContentType, ct);

    public Task<PhotoDetails> AddPhotoWithIdAsync(Guid farmId, Guid cycleId,
        Guid observationId, Guid photoId, Guid actor, Stream stream, long length,
        string? claimedContentType, CancellationToken ct = default)
    {
        if (photoId == Guid.Empty)
            throw Invalid("A valid photo ID is required.");
        return SavePhotoAsync(farmId, cycleId, observationId, photoId, actor,
            stream, length, claimedContentType, ct);
    }

    private async Task<PhotoDetails> SavePhotoAsync(Guid farmId, Guid cycleId,
        Guid observationId, Guid? photoId, Guid actor, Stream stream, long length,
        string? claimedContentType, CancellationToken ct)
    {
        // A completed crop can replay a photo that the server already saved.
        // A genuinely new photo still requires an active crop.
        await RequireCycle(farmId, cycleId, actor, false, ct);
        if (await observations.GetByIdAsync(cycleId, observationId, ct) is null)
            throw Missing();
        if (length is <= 0 or > MonitoringRules.MaxPhotoBytes)
            throw Invalid("Photo must be 5 MB or smaller.");

        using var buffer = new MemoryStream();
        var block = new byte[81920];
        while (true)
        {
            var read = await stream.ReadAsync(block.AsMemory(), ct);
            if (read == 0) break;
            if (buffer.Length + read > MonitoringRules.MaxPhotoBytes || buffer.Length + read > length)
                throw Invalid("Invalid photo size.");
            await buffer.WriteAsync(block.AsMemory(0, read), ct);
        }
        if (buffer.Length != length) throw Invalid("Invalid photo size.");
        var bytes = buffer.ToArray();
        var ext = MonitoringRules.PhotoExtension(bytes, claimedContentType);
        if (ext is null)
            throw Invalid("Only genuine JPEG, PNG, and WebP photos are accepted.");
        var mediaType = ext switch
        {
            ".jpg" => "image/jpeg", ".png" => "image/png", _ => "image/webp"
        };

        async Task<PhotoDetails> CheckReplay(ObservationPhoto existing)
        {
            if (existing.UploadedByUserId != actor || existing.ContentType != mediaType ||
                existing.FileSizeBytes != bytes.LongLength)
                throw new FarmOperationException(FarmError.Conflict,
                    "Photo ID already belongs to another upload.");
            await using var source = await photoStore.OpenAsync(existing.StorageKey, ct);
            using var saved = new MemoryStream();
            await source.CopyToAsync(saved, ct);
            if (!saved.ToArray().AsSpan().SequenceEqual(bytes))
                throw new FarmOperationException(FarmError.Conflict,
                    "Photo ID already belongs to different photo content.");
            return Map(existing);
        }

        if (photoId is { } stableId &&
            await photos.GetByIdAsync(observationId, stableId, ct) is { } alreadySaved)
            return await CheckReplay(alreadySaved);

        await RequireCycle(farmId, cycleId, actor, true, ct);
        var key = await photoStore.SaveAsync(bytes, ext, ct);
        var photo = new ObservationPhoto
        {
            Id = photoId ?? Guid.NewGuid(),
            CropObservationId = observationId, StorageKey = key,
            ContentType = mediaType, FileSizeBytes = bytes.LongLength,
            UploadedByUserId = actor
        };
        try
        {
            photos.Add(photo);
            await unit.SaveChangesAsync(ct);
        }
        catch (DbUpdateException ex) when (photoId is not null &&
            ex.InnerException is SqlException { Number: 2601 or 2627 })
        {
            await photoStore.DeleteAsync(key);
            var prior = await photos.GetByIdAsync(observationId, photo.Id, ct);
            if (prior is null)
                throw new FarmOperationException(FarmError.Conflict, "Photo ID is already in use.");
            return await CheckReplay(prior);
        }
        catch
        {
            await photoStore.DeleteAsync(key);
            throw;
        }
        return Map(photo);
    }

    public async Task<(Stream Stream, string ContentType)> OpenPhotoAsync(
        Guid farmId, Guid cycleId, Guid photoId, Guid actor, CancellationToken ct = default)
    {
        await RequireCycle(farmId, cycleId, actor, false, ct);
        var photo = await photos.GetForCycleAsync(cycleId, photoId, ct) ?? throw Missing();
        try
        {
            return (await photoStore.OpenAsync(photo.StorageKey, ct), photo.ContentType);
        }
        catch (FileNotFoundException)
        {
            throw Missing();
        }
    }
}



## Repository file: src/SmartFarm.Infrastructure/Services/Reports/FarmReportService.cs

using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Reports;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Reports;

public sealed class FarmReportService(SmartFarmDbContext db, IFarmAccessService access)
    : IFarmReportService
{
    private static FarmOperationException NotFound() =>
        new(FarmError.NotFound, "Farm not found.");
    private static FarmOperationException Invalid(string message) =>
        new(FarmError.InvalidInput, message);

    private async Task<Farm> RequireFarm(Guid farmId, Guid actor, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct)) throw NotFound();
        return await db.Farms.AsNoTracking().SingleOrDefaultAsync(
            f => f.Id == farmId && f.ArchivedAtUtc == null, ct) ?? throw NotFound();
    }

    private static TimeZoneInfo Zone(Farm farm)
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId); }
        catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
        { return TimeZoneInfo.Utc; }
    }

    private static DateOnly FarmDate(DateTime utc, TimeZoneInfo zone) =>
        DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
            DateTime.SpecifyKind(utc, DateTimeKind.Utc), zone));

    private static DateTime BeginningUtc(DateOnly day, TimeZoneInfo zone) =>
        TimeZoneInfo.ConvertTimeToUtc(
            DateTime.SpecifyKind(day.ToDateTime(TimeOnly.MinValue), DateTimeKind.Unspecified),
            zone);

    public async Task<HarvestReport> HarvestsAsync(Guid farmId, Guid actor,
        DateOnly? from = null, DateOnly? to = null,
        Guid? plotId = null, Guid? cropTypeId = null, CancellationToken ct = default)
    {
        var farm = await RequireFarm(farmId, actor, ct);
        var earliest = new DateOnly(2000, 1, 1);
        var latest = new DateOnly(2100, 12, 30);
        if (from is { } start && (start < earliest || start > latest) ||
            to is { } end && (end < earliest || end > latest) ||
            from is not null && to is not null && from.Value > to.Value ||
            plotId == Guid.Empty || cropTypeId == Guid.Empty)
            throw Invalid("Choose valid report dates between 2000 and 2100, with From before To.");

        var zone = Zone(farm);
        // Filter with the farm's local calendar day boundaries. The end is exclusive
        // so every harvest on the selected end date is included, including late evenings.
        DateTime? startUtc = from is { } first ? BeginningUtc(first, zone) : null;
        DateTime? endUtc = to is { } last ? BeginningUtc(last.AddDays(1), zone) : null;
        var query =
            from harvest in db.Harvests.AsNoTracking()
            join crop in db.CropCycles.AsNoTracking() on harvest.CropCycleId equals crop.Id
            join plot in db.Plots.AsNoTracking() on crop.PlotId equals plot.Id
            join type in db.CropTypes.AsNoTracking() on crop.CropTypeId equals type.Id
            where plot.FarmId == farmId
                && (plotId == null || plot.Id == plotId.Value)
                && (cropTypeId == null || type.Id == cropTypeId.Value)
                && (startUtc == null || harvest.HarvestedAtUtc >= startUtc.Value)
                && (endUtc == null || harvest.HarvestedAtUtc < endUtc.Value)
            select new
            {
                harvest.Id, CycleId = crop.Id, PlotId = plot.Id, PlotName = plot.Name,
                CropTypeId = type.Id, CropTypeName = type.Name,
                harvest.Quantity, harvest.Unit, harvest.HarvestedAtUtc,
                harvest.VoidedAtUtc, harvest.Notes
            };
        var raw = await query.ToListAsync(ct);
        var records = raw.Select(row => new HarvestReportEntry(
            row.Id, row.CycleId, row.PlotId, row.PlotName,
            row.CropTypeId, row.CropTypeName, row.Quantity, row.Unit,
            row.HarvestedAtUtc, FarmDate(row.HarvestedAtUtc, zone),
            row.VoidedAtUtc is not null, row.Notes));
        return HarvestReportRules.Build(farmId, farm.Name, farm.TimeZoneId,
            from, to, plotId, cropTypeId, records);
    }

    public async Task<FarmDashboard> DashboardAsync(Guid farmId, Guid actor,
        CancellationToken ct = default)
    {
        var farm = await RequireFarm(farmId, actor, ct);
        var today = FarmDate(DateTime.UtcNow, Zone(farm));
        var activePlots = await db.Plots.AsNoTracking().CountAsync(
            p => p.FarmId == farmId && p.ArchivedAtUtc == null, ct);
        var cropStatuses = await (
            from crop in db.CropCycles.AsNoTracking()
            join plot in db.Plots.AsNoTracking() on crop.PlotId equals plot.Id
            where plot.FarmId == farmId
            select crop.Status).ToListAsync(ct);
        var openTasks = await db.FarmTasks.AsNoTracking().CountAsync(
            task => task.FarmId == farmId && task.Status == 0, ct);
        var overdueTasks = await db.FarmTasks.AsNoTracking().CountAsync(
            task => task.FarmId == farmId && task.Status == 0 && task.DueDate < today, ct);
        var lowStock = await db.InventoryItems.AsNoTracking().CountAsync(
            item => item.FarmId == farmId && item.ArchivedAtUtc == null &&
                item.ReorderLevel != null && item.QuantityOnHand <= item.ReorderLevel, ct);
        var concerning = await (
            from obs in db.CropObservations.AsNoTracking()
            join cycle in db.CropCycles.AsNoTracking() on obs.CropCycleId equals cycle.Id
            join plot in db.Plots.AsNoTracking() on cycle.PlotId equals plot.Id
            where plot.FarmId == farmId && cycle.Status == 1 && obs.HealthStatus >= 2
            select obs.Id).CountAsync(ct);
        var recent = await HarvestsAsync(farmId, actor, today.AddDays(-29), today, ct: ct);
        return new FarmDashboard(
            farmId, farm.Name, today, activePlots,
            cropStatuses.Count(s => s == 0), cropStatuses.Count(s => s == 1),
            cropStatuses.Count(s => s == 2),
            openTasks, overdueTasks, lowStock, concerning,
            recent.UnitTotals, recent.Entries.Where(e => !e.IsVoided).Take(5).ToList());
    }
}


## Repository file: src/SmartFarm.Infrastructure/Services/Tasks/FarmTaskService.cs

using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Persistence;
using SmartFarm.Core.Interfaces.Repositories;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Infrastructure.Services.Tasks;

public sealed class FarmTaskService(
    IFarmTaskRepository tasks,
    IFarmAccessService access,
    IUnitOfWork unit) : IFarmTaskService
{
    private static FarmOperationException NotFound() => new(FarmError.NotFound, "Resource not found.");
    private static FarmOperationException Invalid(string message) => new(FarmError.InvalidInput, message);
    private static FarmOperationException Conflict() => new(FarmError.Conflict, "This task has changed. Reload and retry.");

    private static FarmTaskDetails Map(FarmTask task) => new(
        task.Id, task.FarmId, task.CropCycleId, task.AssignedToUserId,
        task.TaskType, task.DueDate, task.Status, task.Description,
        task.CreatedAtUtc, task.ClosedAtUtc, Convert.ToBase64String(task.RowVersion));

    private static NotificationDetails Map(Notification n) => new(
        n.Id, n.Type, n.Title, n.Message, n.FarmTaskId, n.WeatherAdvisoryId,
        n.CreatedAtUtc, n.ReadAtUtc);

    private async Task Require(Guid farmId, Guid actor, FarmAction action, CancellationToken ct)
    {
        if (!await access.CanAsync(farmId, actor, action, ct))
            throw NotFound();
    }

    private async Task Save(CancellationToken ct)
    {
        try { await unit.SaveChangesAsync(ct); }
        catch (DbUpdateConcurrencyException) { throw Conflict(); }
        catch (DbUpdateException e) when (e.InnerException is SqlException { Number: 547 })
        {
            throw Invalid("The task uses an invalid farm, crop, or user.");
        }
    }

    public async Task<IReadOnlyList<FarmTaskDetails>> ListAsync(Guid farmId, Guid actor, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        return (await tasks.ListAsync(farmId, ct)).Select(Map).ToList();
    }

    public async Task<FarmTaskDetails> CreateAsync(Guid farmId, Guid actor, FarmTaskInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (TaskRules.Validate(input) is { } error) throw Invalid(error);
        if (input.CropCycleId is { } cycle && !await tasks.CropBelongsToFarmAsync(farmId, cycle, ct))
            throw NotFound();
        if (input.AssignedToUserId is { } assignee && !await tasks.IsAssignableAsync(farmId, assignee, ct))
            throw Invalid("Assign the task to an active farm member.");

        var task = new FarmTask
        {
            FarmId = farmId,
            CropCycleId = input.CropCycleId,
            AssignedToUserId = input.AssignedToUserId,
            TaskType = input.TaskType,
            DueDate = input.DueDate,
            Description = input.Description?.Trim(),
            CreatedAtUtc = DateTime.UtcNow
        };
        tasks.Add(task);
        await Save(ct);
        return Map(task);
    }

    private static byte[] ParseVersion(string? version)
    {
        try
        {
            var parsed = Convert.FromBase64String(version ?? "");
            if (parsed.Length == 8) return parsed;
        }
        catch (FormatException) { }
        throw Invalid("A current row version is required.");
    }

    public async Task<FarmTaskDetails> UpdateAsync(Guid farmId, Guid taskId, Guid actor,
        FarmTaskUpdateInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.Manage, ct);
        if (TaskRules.ValidateUpdate(input) is { } error) throw Invalid(error);
        var task = await tasks.FindAsync(farmId, taskId, ct) ?? throw NotFound();
        if (task.Status != 0) throw Conflict();
        if (task.CropCycleId is { } cycle &&
            !await tasks.CropBelongsToFarmAsync(farmId, cycle, ct))
            throw Conflict();
        if (input.AssignedToUserId is { } assignee &&
            !await tasks.IsAssignableAsync(farmId, assignee, ct))
            throw Invalid("Assign the task to an active farm member.");

        tasks.SetOriginalVersion(task, ParseVersion(input.RowVersion));
        if (task.DueDate != input.DueDate || task.AssignedToUserId != input.AssignedToUserId)
            await tasks.DismissRemindersAsync(task.Id, DateTime.UtcNow, ct);
        task.DueDate = input.DueDate;
        task.AssignedToUserId = input.AssignedToUserId;
        task.Description = input.Description?.Trim();
        await Save(ct);
        return Map(task);
    }

    public async Task<FarmTaskDetails> CloseAsync(Guid farmId, Guid taskId, Guid actor,
        FarmTaskCloseInput input, CancellationToken ct = default)
    {
        await Require(farmId, actor, FarmAction.View, ct);
        var task = await tasks.FindAsync(farmId, taskId, ct) ?? throw NotFound();
        var level = await access.GetAccessLevelAsync(farmId, actor, ct);
        if (level == FarmAccessLevel.FieldWorker && task.AssignedToUserId != actor)
            throw NotFound();
        if (task.Status != 0) throw Conflict();
        if (!TaskRules.IsValidCloseStatus(input.Status))
            throw Invalid("Choose Completed, Skipped, or Cancelled.");

        tasks.SetOriginalVersion(task, ParseVersion(input.RowVersion));
        TaskRules.Close(task, input.Status, actor, DateTime.UtcNow);
        await tasks.DismissRemindersAsync(task.Id, task.ClosedAtUtc!.Value, ct);
        await Save(ct);
        return Map(task);
    }

    public async Task<IReadOnlyList<NotificationDetails>> NotificationsAsync(Guid actor, CancellationToken ct = default)
    {
        if (actor == Guid.Empty) throw NotFound();
        return (await tasks.ListNotificationsAsync(actor, ct)).Select(Map).ToList();
    }

    public async Task MarkNotificationReadAsync(Guid actor, Guid notificationId, CancellationToken ct = default)
    {
        if (actor == Guid.Empty) throw NotFound();
        var notification = await tasks.FindNotificationAsync(actor, notificationId, ct) ?? throw NotFound();
        notification.ReadAtUtc ??= DateTime.UtcNow;
        await Save(ct);
    }
}


## Repository file: src/SmartFarm.Infrastructure/Services/Weather/FarmWeatherService.cs

using Microsoft.EntityFrameworkCore;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Services.Weather;

// Shared read model for MVC and API. Forecast retrieval remains a Worker concern.
public sealed class FarmWeatherService(IFarmAccessService access, SmartFarmDbContext db)
    : IFarmWeatherService
{
    public async Task<WeatherOverview> GetAsync(
        Guid farmId, Guid actor, CancellationToken ct = default)
    {
        if (!await access.CanAsync(farmId, actor, FarmAction.View, ct))
            throw new FarmOperationException(FarmError.NotFound, "Resource not found.");

        var farm = await db.Farms.AsNoTracking().SingleOrDefaultAsync(
            f => f.Id == farmId && f.ArchivedAtUtc == null, ct);
        if (farm is null)
            throw new FarmOperationException(FarmError.NotFound, "Resource not found.");

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        try
        {
            today = DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(
                DateTime.UtcNow, TimeZoneInfo.FindSystemTimeZoneById(farm.TimeZoneId)));
        }
        catch (Exception error) when (error is TimeZoneNotFoundException or InvalidTimeZoneException)
        {
            // Legacy rows with invalid zones can still view their weather data.
        }

        var forecasts = await db.WeatherForecasts.AsNoTracking()
            .Where(f => f.FarmId == farmId && f.ForecastDate >= today &&
                f.ForecastDate <= today.AddDays(4))
            .OrderByDescending(f => f.RetrievedAtUtc)
            .Take(100).ToListAsync(ct);
        var days = forecasts.GroupBy(f => f.ForecastDate)
            .Select(g => g.OrderByDescending(f => f.RetrievedAtUtc).First())
            .OrderBy(f => f.ForecastDate)
            .Select(f => new WeatherDayDetails(f.ForecastDate, f.MinTemperatureC,
                f.MaxTemperatureC, f.RainfallMm, f.RetrievedAtUtc, f.Provider,
                f.MaxPrecipitationProbability, f.MaxWindKph))
            .ToList();

        var alerts = await db.WeatherAdvisories.AsNoTracking()
            .Where(a => a.FarmId == farmId && a.AdvisoryDate >= today &&
                a.AdvisoryDate <= today.AddDays(4))
            .OrderBy(a => a.AdvisoryDate).ThenByDescending(a => a.Severity)
            .Select(a => new WeatherAdvisoryDetails(
                a.Id, a.AdvisoryDate, a.AdvisoryType, a.Severity,
                a.Status, a.Message, a.CreatedAtUtc))
            .ToListAsync(ct);

        return new WeatherOverview(farmId, farm.Latitude.HasValue && farm.Longitude.HasValue,
            farm.TimeZoneId, days, alerts);
    }
}


## Repository file: src/SmartFarm.Api/Controllers/AuthController.cs

using Microsoft.AspNetCore.Authentication;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Api.Models.Auth;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Route("api/auth")]
public sealed class AuthController(
    UserManager<SmartFarmUser> users,
    SignInManager<SmartFarmUser> signIn,
    AccountRegistrationService registration,
    IOptionsMonitor<BearerTokenOptions> tokenOptions) : ControllerBase
{
    [AllowAnonymous, HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterInput input)
    {
        var result = await registration.RegisterAsync(input.FullName, input.Email, input.Password, input.JoinFarm, input.FarmCode);
        if (!result.Succeeded)
            return BadRequest(new { errors = result.Errors.Select(error => error.Description), farmCodeError = result.Errors.FirstOrDefault(error => error.Code == "FarmCode")?.Description });

        return StatusCode(StatusCodes.Status201Created, new { message = input.JoinFarm ? "Account created. Farm access awaits owner approval. Please sign in." : "Account created. Please sign in." });
    }

    [AllowAnonymous, HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginInput input)
    {
        var user = await users.FindByEmailAsync(input.Email.Trim());
        if (user is null || user.DisabledAtUtc is not null)
            return Unauthorized(new { message = "Invalid email or password." });

        var result = await signIn.CheckPasswordSignInAsync(user, input.Password, lockoutOnFailure: true);
        if (!result.Succeeded)
            return Unauthorized(new { message = "Invalid email or password, or account temporarily unavailable." });

        var principal = await signIn.CreateUserPrincipalAsync(user);
        await HttpContext.SignInAsync(IdentityConstants.BearerScheme, principal);
        // The bearer handler writes tokenType/accessToken/expiresIn/refreshToken.
        return new EmptyResult();
    }

    [AllowAnonymous, HttpPost("refresh")]
    public async Task<IActionResult> Refresh([FromBody] RefreshTokenInput input)
    {
        AuthenticationTicket? ticket;
        try
        {
            ticket = tokenOptions.Get(IdentityConstants.BearerScheme)
                .RefreshTokenProtector.Unprotect(input.RefreshToken);
        }
        catch (Exception exception) when (exception is FormatException or ArgumentException or CryptographicException)
        {
            return Unauthorized();
        }

        if (ticket?.Properties?.ExpiresUtc is not { } expiration ||
            expiration <= DateTimeOffset.UtcNow ||
            await signIn.ValidateSecurityStampAsync(ticket.Principal) is not { } user ||
            user.DisabledAtUtc is not null)
            return Unauthorized();

        var principal = await signIn.CreateUserPrincipalAsync(user);
        await HttpContext.SignInAsync(IdentityConstants.BearerScheme, principal);
        return new EmptyResult();
    }

    [Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer), HttpGet("me")]
    public async Task<IActionResult> Me()
    {
        var user = await users.GetUserAsync(User);
        if (user is null || user.DisabledAtUtc is not null) return Unauthorized();
        return Ok(new { id = user.Id, fullName = user.FullName, email = user.Email });
    }
}


## Repository file: src/SmartFarm.Api/Controllers/CropCyclesController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}")]
public sealed class CropCyclesController(
    UserManager<SmartFarmUser> users,
    ICropManagementService crops) : FarmApiControllerBase(users)
{
    [HttpGet("crop-cycles")]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await crops.ListCyclesAsync(farmId, ActorId, ct)));

    [HttpGet("crop-cycles/{cycleId:guid}")]
    public Task<IActionResult> Get(Guid farmId, Guid cycleId, CancellationToken ct) =>
        Result(async () =>
            await crops.GetCycleAsync(farmId, cycleId, ActorId, ct) is { } cycle
                ? Ok(cycle) : NotFound());

    [HttpPost("plots/{plotId:guid}/crop-cycles")]
    public Task<IActionResult> Plan(Guid farmId, Guid plotId, [FromBody] CropCyclePlanInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var created = await crops.PlanAsync(farmId, plotId, ActorId, input, ct);
            return CreatedAtAction(nameof(Get), new { farmId, cycleId = created.Id }, created);
        });

    [HttpPut("crop-cycles/{cycleId:guid}")]
    public Task<IActionResult> UpdatePlan(Guid farmId, Guid cycleId,
        [FromBody] CropCyclePlanInput input, CancellationToken ct) =>
        Result(async () => Ok(await crops.UpdatePlanAsync(farmId, cycleId, ActorId, input, ct)));

    [HttpPost("crop-cycles/{cycleId:guid}/transitions")]
    public Task<IActionResult> Transition(Guid farmId, Guid cycleId,
        [FromBody] CropCycleTransitionInput input, CancellationToken ct) =>
        Result(async () => Ok(await crops.TransitionAsync(farmId, cycleId, ActorId, input, ct)));
}


## Repository file: src/SmartFarm.Api/Controllers/CropTypesController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/crop-types")]
public sealed class CropTypesController(
    UserManager<SmartFarmUser> users,
    ICropManagementService crops) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await crops.ListCropTypesAsync(farmId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Create(Guid farmId, [FromBody] CropTypeInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var created = await crops.CreateCropTypeAsync(farmId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, created);
        });
}


## Repository file: src/SmartFarm.Api/Controllers/FarmAccessController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Core.Services.Security;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

// A protected permission check, not farm CRUD. Future farm endpoints must
// perform the same server-side, per-farm authorization before reading or writing.
[ApiController]
[Route("api/farms/{farmId:guid}/access")]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
public sealed class FarmAccessController(
    UserManager<SmartFarmUser> users,
    IFarmAccessService farmAccess) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> Get(Guid farmId, CancellationToken cancellationToken)
    {
        var rawUserId = users.GetUserId(User);
        if (!Guid.TryParse(rawUserId, out var userId)) return Unauthorized();

        var level = await farmAccess.GetAccessLevelAsync(farmId, userId, cancellationToken);
        if (!FarmAccessRules.Allows(level, FarmAction.View)) return NotFound();
        return Ok(new
        {
            farmId,
            role = level.ToString(),
            canManage = FarmAccessRules.Allows(level, FarmAction.Manage),
            canManageMembers = FarmAccessRules.Allows(level, FarmAction.ManageMembers)
        });
    }
}


## Repository file: src/SmartFarm.Api/Controllers/FarmApiControllerBase.cs

using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

public abstract class FarmApiControllerBase(UserManager<SmartFarmUser> users) : ControllerBase
{
    protected Guid ActorId =>
        Guid.TryParse(users.GetUserId(User), out var id) ? id : Guid.Empty;

    protected async Task<IActionResult> Result(Func<Task<IActionResult>> work)
    {
        try { return await work(); }
        catch (FarmOperationException problem)
        {
            return problem.Error switch
            {
                FarmError.NotFound => NotFound(new { message = "Resource not found." }),
                FarmError.Conflict => Conflict(new { message = problem.Message }),
                _ => BadRequest(new { message = problem.Message })
            };
        }
    }
}


## Repository file: src/SmartFarm.Api/Controllers/FarmJoinRequestsController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farm-join-requests")]
public sealed class FarmJoinRequestsController(UserManager<SmartFarmUser> users, IFarmEnrollmentService enrollment)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Mine(CancellationToken ct) =>
        Result(async () => Ok(await enrollment.MyRequestsAsync(ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Submit([FromBody] FarmJoinRequestInput input, CancellationToken ct) =>
        Result(async () => Ok(await enrollment.RequestAsync(ActorId, input.FarmCode, ct)));

    [HttpGet("/api/farms/{farmId:guid}/join-requests")]
    public Task<IActionResult> Overview(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await enrollment.OwnerOverviewAsync(farmId, ActorId, ct)));

    [HttpPost("/api/farms/{farmId:guid}/join-requests/{requestId:guid}/decision")]
    public Task<IActionResult> Decision(Guid farmId, Guid requestId, [FromBody] FarmJoinDecisionInput input, CancellationToken ct) =>
        Result(async () =>
        {
            await enrollment.DecideAsync(farmId, requestId, ActorId, input.Approve!.Value, ct);
            return NoContent();
        });
}


## Repository file: src/SmartFarm.Api/Controllers/FarmMembersController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/members")]
public sealed class FarmMembersController(
    UserManager<SmartFarmUser> users, IFarmManagementService farms)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await farms.MembersAsync(farmId, ActorId, ct)));

    [AllowAnonymous, HttpPost("invitations")]
    public IActionResult Invite(Guid farmId) =>
        StatusCode(StatusCodes.Status410Gone, new { message = "Invitation links are retired. Use a farm code and owner approval." });

    [HttpPut("{memberId:guid}")]
    public Task<IActionResult> Role(Guid farmId, Guid memberId,
        [FromBody] UpdateMemberInput input, CancellationToken ct) => Result(async () =>
    {
        await farms.ChangeMemberRoleAsync(farmId, memberId, ActorId, input.Role, ct);
        return NoContent();
    });

    [HttpDelete("{memberId:guid}")]
    public Task<IActionResult> Remove(Guid farmId, Guid memberId, CancellationToken ct) => Result(async () =>
    {
        await farms.RemoveMemberAsync(farmId, memberId, ActorId, ct);
        return NoContent();
    });
}

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farm-invitations")]
public sealed class FarmInvitationsController : ControllerBase
{
    [AllowAnonymous, HttpPost("accept")]
    public IActionResult Accept() =>
        StatusCode(StatusCodes.Status410Gone, new { message = "Invitation links are retired. Use a farm code and owner approval." });
}


## Repository file: src/SmartFarm.Api/Controllers/FarmReportsController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}")]
public sealed class FarmReportsController(
    UserManager<SmartFarmUser> users, IFarmReportService reports)
    : FarmApiControllerBase(users)
{
    [HttpGet("dashboard")]
    public Task<IActionResult> Dashboard(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await reports.DashboardAsync(farmId, ActorId, ct)));

    [HttpGet("reports/harvests")]
    public Task<IActionResult> Harvests(Guid farmId, [FromQuery] DateOnly? from,
        [FromQuery] DateOnly? to, [FromQuery] Guid? plotId,
        [FromQuery] Guid? cropTypeId, CancellationToken ct) =>
        Result(async () => Ok(await reports.HarvestsAsync(
            farmId, ActorId, from, to, plotId, cropTypeId, ct)));
}


## Repository file: src/SmartFarm.Api/Controllers/FarmTasksController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/tasks")]
public sealed class FarmTasksController(
    UserManager<SmartFarmUser> users,
    IFarmTaskService tasks) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await tasks.ListAsync(farmId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Create(Guid farmId, [FromBody] FarmTaskInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var created = await tasks.CreateAsync(farmId, ActorId, input, ct);
            return Ok(created);
        });

    [HttpPut("{taskId:guid}")]
    public Task<IActionResult> Update(Guid farmId, Guid taskId, [FromBody] FarmTaskUpdateInput input,
        CancellationToken ct) =>
        Result(async () => Ok(await tasks.UpdateAsync(farmId, taskId, ActorId, input, ct)));

    [HttpPut("{taskId:guid}/close")]
    public Task<IActionResult> Close(Guid farmId, Guid taskId, [FromBody] FarmTaskCloseInput input, CancellationToken ct) =>
        Result(async () => Ok(await tasks.CloseAsync(farmId, taskId, ActorId, input, ct)));
}


## Repository file: src/SmartFarm.Api/Controllers/FarmWeatherController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/weather")]
public sealed class FarmWeatherController(
    UserManager<SmartFarmUser> users, IFarmWeatherService weather) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Get(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await weather.GetAsync(farmId, ActorId, ct)));
}


## Repository file: src/SmartFarm.Api/Controllers/FarmsController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms")]
public sealed class FarmsController(
    UserManager<SmartFarmUser> users, IFarmManagementService farms)
    : FarmApiControllerBase(users)
{
    [HttpGet]
    public async Task<IActionResult> List(CancellationToken ct) =>
        Ok(await farms.ListAsync(ActorId, ct));

    [HttpGet("{farmId:guid}")]
    public Task<IActionResult> Get(Guid farmId, CancellationToken ct) => Result(async () =>
        await farms.GetAsync(farmId, ActorId, ct) is { } farm ? Ok(farm) : NotFound());

    [HttpPost]
    public Task<IActionResult> Create([FromBody] FarmInput input, CancellationToken ct) => Result(async () =>
    {
        var created = await farms.CreateAsync(ActorId, input, ct);
        return CreatedAtAction(nameof(Get), new { farmId = created.Id }, created);
    });

    [HttpPut("{farmId:guid}")]
    public Task<IActionResult> Update(Guid farmId, [FromBody] FarmInput input, CancellationToken ct) => Result(async () =>
        Ok(await farms.UpdateAsync(farmId, ActorId, input, ct)));

    [HttpDelete("{farmId:guid}")]
    public Task<IActionResult> Archive(Guid farmId, [FromBody] RowVersionInput input, CancellationToken ct) => Result(async () =>
    {
        await farms.ArchiveAsync(farmId, ActorId, input.RowVersion, ct);
        return NoContent();
    });

    [HttpGet("{farmId:guid}/plots")]
    public Task<IActionResult> Plots(Guid farmId, CancellationToken ct) => Result(async () =>
        Ok(await farms.ListPlotsAsync(farmId, ActorId, ct)));

    [HttpGet("{farmId:guid}/plots/{plotId:guid}")]
    public Task<IActionResult> Plot(Guid farmId, Guid plotId, CancellationToken ct) => Result(async () =>
        await farms.GetPlotAsync(farmId, plotId, ActorId, ct) is { } plot ? Ok(plot) : NotFound());

    [HttpPost("{farmId:guid}/plots")]
    public Task<IActionResult> CreatePlot(Guid farmId, [FromBody] PlotInput input, CancellationToken ct) => Result(async () =>
    {
        var plot = await farms.CreatePlotAsync(farmId, ActorId, input, ct);
        return CreatedAtAction(nameof(Plot), new { farmId, plotId = plot.Id }, plot);
    });

    [HttpPut("{farmId:guid}/plots/{plotId:guid}")]
    public Task<IActionResult> UpdatePlot(Guid farmId, Guid plotId, [FromBody] PlotInput input, CancellationToken ct) =>
        Result(async () => Ok(await farms.UpdatePlotAsync(farmId, plotId, ActorId, input, ct)));

    [HttpDelete("{farmId:guid}/plots/{plotId:guid}")]
    public Task<IActionResult> ArchivePlot(Guid farmId, Guid plotId, [FromBody] RowVersionInput input, CancellationToken ct) => Result(async () =>
    {
        await farms.ArchivePlotAsync(farmId, plotId, ActorId, input.RowVersion, ct);
        return NoContent();
    });
}


## Repository file: src/SmartFarm.Api/Controllers/HarvestsApiController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/crop-cycles/{cycleId:guid}/harvests")]
public sealed class HarvestsApiController(
    UserManager<SmartFarmUser> users,
    IHarvestService harvests) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, Guid cycleId, CancellationToken ct) =>
        Result(async () => Ok(await harvests.GetAsync(farmId, cycleId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Record(Guid farmId, Guid cycleId,
        [FromBody] RecordHarvestInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var saved = await harvests.RecordAsync(farmId, cycleId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, saved);
        });

    [HttpPost("{harvestId:guid}/void")]
    public Task<IActionResult> Void(Guid farmId, Guid cycleId, Guid harvestId,
        [FromBody] VoidHarvestInput input, CancellationToken ct) =>
        Result(async () => Ok(await harvests.VoidAsync(
            farmId, cycleId, harvestId, ActorId, input, ct)));
}


## Repository file: src/SmartFarm.Api/Controllers/InventoryApiController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/inventory")]
public sealed class InventoryApiController(
    UserManager<SmartFarmUser> users, IInventoryService inventory) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await inventory.ListAsync(farmId, ActorId, ct)));

    [HttpPost]
    public Task<IActionResult> Create(Guid farmId, [FromBody] InventoryItemInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var item = await inventory.CreateAsync(farmId, ActorId, input, ct);
            return CreatedAtAction(nameof(Get), new { farmId, itemId = item.Id }, item);
        });

    [HttpGet("{itemId:guid}")]
    public Task<IActionResult> Get(Guid farmId, Guid itemId, CancellationToken ct) =>
        Result(async () =>
            await inventory.GetAsync(farmId, itemId, ActorId, ct) is { } item ? Ok(item) : NotFound());

    [HttpPut("{itemId:guid}")]
    public Task<IActionResult> Update(Guid farmId, Guid itemId,
        [FromBody] InventoryItemInput input, CancellationToken ct) =>
        Result(async () => Ok(await inventory.UpdateAsync(farmId, itemId, ActorId, input, ct)));

    [HttpDelete("{itemId:guid}")]
    public Task<IActionResult> Archive(Guid farmId, Guid itemId,
        [FromBody] RowVersionInput input, CancellationToken ct) =>
        Result(async () =>
        {
            await inventory.ArchiveAsync(farmId, itemId, ActorId, input.RowVersion, ct);
            return NoContent();
        });

    [HttpGet("{itemId:guid}/transactions")]
    public Task<IActionResult> History(Guid farmId, Guid itemId, CancellationToken ct) =>
        Result(async () => Ok(await inventory.HistoryAsync(farmId, itemId, ActorId, ct)));

    [HttpPost("{itemId:guid}/transactions")]
    public Task<IActionResult> Post(Guid farmId, Guid itemId,
        [FromBody] InventoryMovementInput input, CancellationToken ct) =>
        Result(async () => Ok(await inventory.PostAsync(farmId, itemId, ActorId, input, ct)));

    [HttpPost("{itemId:guid}/transactions/{transactionId:guid}/reverse")]
    public Task<IActionResult> Reverse(Guid farmId, Guid itemId, Guid transactionId,
        [FromBody] InventoryReversalInput input, CancellationToken ct) =>
        Result(async () => Ok(await inventory.ReverseAsync(
            farmId, itemId, transactionId, ActorId, input, ct)));

    [HttpGet("activities")]
    public Task<IActionResult> Activities(Guid farmId, CancellationToken ct) =>
        Result(async () => Ok(await inventory.ActivityOptionsAsync(farmId, ActorId, ct)));
}


## Repository file: src/SmartFarm.Api/Controllers/MonitoringApiController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Infrastructure.Identity;

using SmartFarm.Api.Authentication;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/farms/{farmId:guid}/crop-cycles/{cycleId:guid}/monitoring")]
public sealed class MonitoringApiController(
    UserManager<SmartFarmUser> users,
    IMonitoringService monitoring) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> Get(Guid farmId, Guid cycleId, CancellationToken ct) =>
        Result(async () => Ok(await monitoring.GetAsync(farmId, cycleId, ActorId, ct)));

    [HttpPost("activities")]
    public Task<IActionResult> Activity(Guid farmId, Guid cycleId,
        [FromBody] RecordActivityInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var activity = await monitoring.RecordActivityAsync(farmId, cycleId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, activity);
        });

    [HttpPost("observations")]
    public Task<IActionResult> Observation(Guid farmId, Guid cycleId,
        [FromBody] RecordObservationInput input, CancellationToken ct) =>
        Result(async () =>
        {
            var observation = await monitoring.RecordObservationAsync(farmId, cycleId, ActorId, input, ct);
            return StatusCode(StatusCodes.Status201Created, observation);
        });

    [HttpPost("observations/{observationId:guid}/photos")]
    [RequestSizeLimit(6 * 1024 * 1024)]
    [RequestFormLimits(MultipartBodyLengthLimit = 6 * 1024 * 1024)]
    public Task<IActionResult> Upload(Guid farmId, Guid cycleId, Guid observationId,
        [FromForm] IFormFile? photo, CancellationToken ct) =>
        Result(async () =>
        {
            if (photo is null) return BadRequest(new { message = "Select a photo." });
            await using var input = photo.OpenReadStream();
            var saved = await monitoring.AddPhotoAsync(
                farmId, cycleId, observationId, ActorId,
                input, photo.Length, photo.ContentType, ct);
            return StatusCode(StatusCodes.Status201Created, saved);
        });

    // Client chooses this ID once and retries the exact same photo safely.
    [HttpPut("observations/{observationId:guid}/photos/{photoId:guid}")]
    [RequestSizeLimit(6 * 1024 * 1024)]
    [RequestFormLimits(MultipartBodyLengthLimit = 6 * 1024 * 1024)]
    public Task<IActionResult> UploadWithId(Guid farmId, Guid cycleId, Guid observationId,
        Guid photoId, [FromForm] IFormFile? photo, CancellationToken ct) =>
        Result(async () =>
        {
            if (photo is null) return BadRequest(new { message = "Select a photo." });
            await using var input = photo.OpenReadStream();
            var saved = await monitoring.AddPhotoWithIdAsync(
                farmId, cycleId, observationId, photoId, ActorId,
                input, photo.Length, photo.ContentType, ct);
            return Ok(saved);
        });

    [HttpGet("photos/{photoId:guid}")]
    public Task<IActionResult> Photo(Guid farmId, Guid cycleId, Guid photoId, CancellationToken ct) =>
        Result(async () =>
        {
            var (stream, contentType) = await monitoring.OpenPhotoAsync(
                farmId, cycleId, photoId, ActorId, ct);
            Response.Headers["X-Content-Type-Options"] = "nosniff";
            Response.Headers.CacheControl = "private, no-store";
            return File(stream, contentType);
        });
}


## Repository file: src/SmartFarm.Api/Controllers/NotificationsController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/notifications")]
public sealed class NotificationsController(
    UserManager<SmartFarmUser> users,
    IFarmTaskService tasks) : FarmApiControllerBase(users)
{
    [HttpGet]
    public Task<IActionResult> List(CancellationToken ct) =>
        Result(async () => Ok(await tasks.NotificationsAsync(ActorId, ct)));

    [HttpPut("{notificationId:guid}/read")]
    public Task<IActionResult> Read(Guid notificationId, CancellationToken ct) =>
        Result(async () =>
        {
            await tasks.MarkNotificationReadAsync(ActorId, notificationId, ct);
            return NoContent();
        });
}


## Repository file: src/SmartFarm.Api/Controllers/StatusController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace SmartFarm.Api.Controllers;

// An anonymous, database-independent smoke test. No private environment
// information is included in the response.
[ApiController]
[AllowAnonymous]
[Route("api/status")]
public sealed class StatusController : ControllerBase
{
    [HttpGet]
    public IActionResult Get() => Ok(new { app = "SmartFarm", status = "ok" });
}


## Repository file: src/SmartFarm.Api/Controllers/SyncOperationsController.cs

using System.Security.Cryptography;
using System.Text.Json;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SmartFarm.Api.Authentication;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Security;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;
using System.Data;

namespace SmartFarm.Api.Controllers;

// A stable client operation ID makes retries safe even after a lost HTTP response.
public sealed class SyncWriteInput
{
    public Guid FarmId { get; set; }
    public Guid TargetId { get; set; }
    public string OperationType { get; set; } = "";
    public RecordActivityInput? Activity { get; set; }
    public RecordObservationInput? Observation { get; set; }
    public InventoryMovementInput? Movement { get; set; }
    public RecordHarvestInput? Harvest { get; set; }
}

public sealed record SyncWriteResult(Guid OperationId, string OperationType, JsonElement Result, bool Replayed);

[ApiController]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer)]
[Route("api/sync/operations")]
public sealed class SyncOperationsController(
    UserManager<SmartFarmUser> users,
    SmartFarmDbContext db,
    IFarmAccessService access,
    IMonitoringService monitoring,
    IInventoryService inventory,
    IHarvestService harvests) : FarmApiControllerBase(users)
{
    [HttpPost("{operationId:guid}")]
    public Task<IActionResult> Submit(Guid operationId, [FromBody] SyncWriteInput input,
        CancellationToken ct) =>
        Result(async () =>
        {
            if (operationId == Guid.Empty || input.FarmId == Guid.Empty || input.TargetId == Guid.Empty)
                return BadRequest(new { message = "Valid operation, farm and target IDs are required." });
            if (input.OperationType is not ("activity" or "observation" or "inventory" or "harvest"))
                return BadRequest(new { message = "Unsupported sync operation." });

            // Never reveal another farm's ledger or apply writes under another account.
            if (!await access.CanAsync(input.FarmId, ActorId, FarmAction.View, ct))
                return NotFound(new { message = "Resource not found." });

            var valid = input.OperationType switch
            {
                "activity" => input.Activity is not null && input.Observation is null &&
                    input.Movement is null && input.Harvest is null,
                "observation" => input.Observation is not null && input.Activity is null &&
                    input.Movement is null && input.Harvest is null,
                "inventory" => input.Movement is not null && input.Activity is null &&
                    input.Observation is null && input.Harvest is null,
                _ => input.Harvest is not null && input.Activity is null &&
                    input.Observation is null && input.Movement is null
            };
            if (!valid)
                return BadRequest(new { message = "Provide exactly one matching operation payload." });

            var hash = SHA256.HashData(JsonSerializer.SerializeToUtf8Bytes(input));
            await using var transaction = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable, ct);

            // UPDLOCK + HOLDLOCK also locks the gap when this GUID is not present.
            // Concurrent requests with the same ID cannot both execute the business write.
            var previous = await db.SyncOperations
                .FromSqlInterpolated($"""
                    SELECT * FROM dbo.SyncOperation WITH (UPDLOCK, HOLDLOCK)
                    WHERE OperationId = {operationId}
                    """)
                .AsNoTracking().SingleOrDefaultAsync(ct);
            if (previous is not null)
            {
                if (previous.UserId != ActorId || previous.FarmId != input.FarmId ||
                    previous.OperationType != input.OperationType ||
                    !CryptographicOperations.FixedTimeEquals(previous.RequestHash, hash))
                    return Conflict(new { message = "Operation ID was already used with different data." });

                var saved = JsonSerializer.Deserialize<JsonElement>(previous.ResultJson!);
                return Ok(new SyncWriteResult(operationId, input.OperationType, saved, true));
            }

            object result = input.OperationType switch
            {
                "activity" => await monitoring.RecordActivityAsync(
                    input.FarmId, input.TargetId, ActorId, input.Activity!, ct),
                "observation" => await monitoring.RecordObservationAsync(
                    input.FarmId, input.TargetId, ActorId, input.Observation!, ct),
                "inventory" => await inventory.PostAsync(
                    input.FarmId, input.TargetId, ActorId, input.Movement!, ct),
                _ => await harvests.RecordAsync(
                    input.FarmId, input.TargetId, ActorId, input.Harvest!, ct)
            };

            var resultJson = JsonSerializer.Serialize(result, new JsonSerializerOptions(JsonSerializerDefaults.Web));
            db.SyncOperations.Add(new SyncOperation
            {
                OperationId = operationId,
                FarmId = input.FarmId,
                UserId = ActorId,
                TargetEntityId = input.TargetId,
                OperationType = input.OperationType,
                RequestHash = hash,
                Status = 1,
                ResultJson = resultJson,
                ReceivedAtUtc = DateTime.UtcNow,
                CompletedAtUtc = DateTime.UtcNow
            });
            await db.SaveChangesAsync(ct);
            await transaction.CommitAsync(ct);

            return Ok(new SyncWriteResult(operationId, input.OperationType,
                JsonSerializer.Deserialize<JsonElement>(resultJson), false));
        });
}


## Repository file: src/SmartFarm.Web/Controllers/FarmsController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Farms;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class FarmsController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly IFarmManagementService _farms;
    private readonly IFarmEnrollmentService _enrollment;

    public FarmsController(
        UserManager<SmartFarmUser> users,
        IFarmManagementService farms, IFarmEnrollmentService enrollment)
    {
        _users = users;
        _farms = farms;
        _enrollment = enrollment;
    }

    private Guid Actor =>
        Guid.TryParse(_users.GetUserId(User), out var id) ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Index(CancellationToken ct) =>
        View(await IndexModel(new(), ct));

    private async Task<FarmIndexViewModel> IndexModel(FarmJoinRequestInput input, CancellationToken ct)
    {
        var farms = await _farms.ListAsync(Actor, ct);
        try { return new(farms, await _enrollment.MyRequestsAsync(Actor, ct), input); }
        catch (Exception error) when (error is System.Data.Common.DbException or HttpRequestException)
        {
            HttpContext.RequestServices.GetRequiredService<ILogger<FarmsController>>().LogError(error, "Could not load join requests");
            return new(farms, [], input, "Join request status is unavailable. Refresh this page to try again.");
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> RequestJoin(FarmJoinRequestInput input, CancellationToken ct)
    {
        if (ModelState.IsValid)
        {
            try
            {
                await _enrollment.RequestAsync(Actor, input.FarmCode, ct);
                TempData["Message"] = "Request submitted. Farm access begins after the owner approves.";
                return RedirectToAction(nameof(Index));
            }
            catch (FarmOperationException error) { ModelState.AddModelError(nameof(input.FarmCode), error.Message); }
        }
        return View(nameof(Index), await IndexModel(input, ct));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> DecideJoin(Guid id, Guid requestId, bool? approve,
        bool dashboard, CancellationToken ct)
    {
        if (!ModelState.IsValid || approve is null) return BadRequest();
        try
        {
            await _enrollment.DecideAsync(id, requestId, Actor, approve.Value, ct);
            TempData["Message"] = approve.Value ? "Join request approved." : "Join request rejected.";
        }
        catch (FarmOperationException error)
        {
            if (error.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = error.Message;
        }
        return dashboard ? RedirectToAction("Index", "Dashboard", new { farmId = id })
            : RedirectToAction(nameof(Details), new { id });
    }

    [HttpGet]
    public IActionResult Create() => View(new FarmInput());

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(FarmInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(input);
        try
        {
            var farm = await _farms.CreateAsync(Actor, input, ct);
            return RedirectToAction(nameof(Details), new { id = farm.Id });
        }
        catch (FarmOperationException e)
        {
            ModelState.AddModelError("", e.Message);
            return View(input);
        }
    }

    [HttpGet]
    public async Task<IActionResult> Details(Guid id, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(id, Actor, ct);
        if (farm is null) return NotFound();
        FarmEnrollmentPanel? enrollment = null;
        if (farm.Role == "Owner")
        {
            try { enrollment = new(id, await _enrollment.OwnerOverviewAsync(id, Actor, ct)); }
            catch (Exception error) when (error is System.Data.Common.DbException or HttpRequestException or FarmOperationException)
            {
                HttpContext.RequestServices.GetRequiredService<ILogger<FarmsController>>().LogError(error, "Could not load owner join requests");
                enrollment = new(id, null, Error: "Join requests are unavailable. Refresh this page to try again.");
            }
        }
        return View(new FarmWorkspaceViewModel(farm,
            await _farms.ListPlotsAsync(id, Actor, ct), await _farms.MembersAsync(id, Actor, ct), enrollment));
    }

    [HttpGet]
    public async Task<IActionResult> Edit(Guid id, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(id, Actor, ct);
        if (farm is null) return NotFound();
        if (farm.Role is not ("Owner" or "Manager")) return NotFound();
        return View(new FarmEditViewModel(id, new FarmInput
        {
            Name = farm.Name,
            LocationText = farm.LocationText,
            TimeZoneId = farm.TimeZoneId,
            Latitude = farm.Latitude,
            Longitude = farm.Longitude,
            RowVersion = farm.RowVersion
        }));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(Guid id, [Bind(Prefix = "Input")] FarmInput input,
        CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(new FarmEditViewModel(id, input));
        try
        {
            await _farms.UpdateAsync(id, Actor, input, ct);
            return RedirectToAction(nameof(Details), new { id });
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            Response.StatusCode = e.Error == FarmError.Conflict ? 409 : 400;
            return View(new FarmEditViewModel(id, input));
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Archive(Guid id, string rowVersion, CancellationToken ct)
    {
        try
        {
            await _farms.ArchiveAsync(id, Actor, rowVersion, ct);
            return RedirectToAction(nameof(Index));
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
            return RedirectToAction(nameof(Details), new { id });
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> CreatePlot(Guid id, PlotInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Use a valid plot name and positive area.";
            return RedirectToAction(nameof(Details), new { id });
        }
        try { await _farms.CreatePlotAsync(id, Actor, input, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpGet]
    public async Task<IActionResult> EditPlot(Guid id, Guid plotId, CancellationToken ct)
    {
        var farm = await _farms.GetAsync(id, Actor, ct);
        if (farm?.Role is not ("Owner" or "Manager")) return NotFound();
        var plot = await _farms.GetPlotAsync(id, plotId, Actor, ct);
        if (plot is null) return NotFound();
        return View(new PlotEditViewModel(id, plotId, new PlotInput
        {
            Name = plot.Name, AreaSqMeters = plot.AreaSqMeters, RowVersion = plot.RowVersion
        }));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> EditPlot(Guid id, Guid plotId, [Bind(Prefix = "Input")] PlotInput input,
        CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(new PlotEditViewModel(id, plotId, input));
        try
        {
            await _farms.UpdatePlotAsync(id, plotId, Actor, input, ct);
            return RedirectToAction(nameof(Details), new { id });
        }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            ModelState.AddModelError("", e.Message);
            Response.StatusCode = e.Error == FarmError.Conflict ? 409 : 400;
            return View(new PlotEditViewModel(id, plotId, input));
        }
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ArchivePlot(Guid id, Guid plotId, string rowVersion,
        CancellationToken ct)
    {
        try { await _farms.ArchivePlotAsync(id, plotId, Actor, rowVersion, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [AllowAnonymous, HttpPost, IgnoreAntiforgeryToken]
    public IActionResult Invite() => RetiredInvitation();

    [AllowAnonymous, AcceptVerbs("GET", "POST"), IgnoreAntiforgeryToken]
    public IActionResult AcceptInvitation() => RetiredInvitation();

    private ContentResult RetiredInvitation() => new()
    {
        StatusCode = StatusCodes.Status410Gone, ContentType = "text/plain",
        Content = "Invitation links have been retired. Register or sign in, enter the owner's farm code, and wait for approval."
    };

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ChangeRole(Guid id, Guid memberId, UpdateMemberInput input, CancellationToken ct)
    {
        try { await _farms.ChangeMemberRoleAsync(id, memberId, Actor, input.Role, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> RemoveMember(Guid id, Guid memberId, CancellationToken ct)
    {
        try { await _farms.RemoveMemberAsync(id, memberId, Actor, ct); }
        catch (FarmOperationException e)
        {
            if (e.Error == FarmError.NotFound) return NotFound();
            TempData["Error"] = e.Message;
        }
        return RedirectToAction(nameof(Details), new { id });
    }
}



## Repository file: src/SmartFarm.Web/Controllers/ReportsController.cs

using System.Text;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.Clients;
using SmartFarm.Web.ViewModels.Reports;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class ReportsController(
    UserManager<SmartFarmUser> users, IFarmReportService reports,
    IFarmManagementService farms, ICropManagementService crops) : Controller
{
    private Guid Actor => Guid.TryParse(users.GetUserId(User), out var id)
        ? id : Guid.Empty;

    [HttpGet]
    public async Task<IActionResult> Harvests(Guid farmId, DateOnly? from,
        DateOnly? to, Guid? plotId, Guid? cropTypeId, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await reports.HarvestsAsync(farmId, Actor, from, to, plotId, cropTypeId, ct);
            var plots = await farms.ListPlotsAsync(farmId, Actor, ct);
            var types = await crops.ListCropTypesAsync(farmId, Actor, ct);
            return View(new HarvestReportPage(report, plots, types));
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound) { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }

    // Read-only report download; the API checks farm membership before any data is returned.
    [HttpGet]
    public async Task<IActionResult> ExportCsv(Guid farmId, DateOnly? from,
        DateOnly? to, Guid? plotId, Guid? cropTypeId, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest();
        try
        {
            var report = await reports.HarvestsAsync(farmId, Actor, from, to, plotId, cropTypeId, ct);
            var content = HarvestCsv.Build(report);
            return File(Encoding.UTF8.GetBytes(content), "text/csv; charset=utf-8",
                $"smartfarm-harvests-{farmId:N}.csv");
        }
        catch (FarmOperationException e) when (e.Error == FarmError.NotFound) { return NotFound(); }
        catch (FarmOperationException e) when (e.Error == FarmError.InvalidInput)
        { return BadRequest(e.Message); }
    }
}


## Repository file: src/SmartFarm.Web/Controllers/DashboardController.cs

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.Authentication;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Web.ViewModels.Farms;

namespace SmartFarm.Web.Controllers;

[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application)]
public sealed class DashboardController(
    UserManager<SmartFarmUser> users, IFarmReportService reports, IFarmManagementService farms,
    IFarmEnrollmentService enrollment, ILogger<DashboardController> logger) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(Guid farmId, CancellationToken ct)
    {
        if (!Guid.TryParse(users.GetUserId(User), out var actor)) return Challenge();
        try
        {
            var farm = await farms.GetAsync(farmId, actor, ct);
            if (farm is null) return NotFound();
            FarmEnrollmentPanel? panel = null;
            if (farm.Role == "Owner")
            {
                try { panel = new(farmId, await enrollment.OwnerOverviewAsync(farmId, actor, ct), true); }
                catch (Exception error) when (error is System.Data.Common.DbException or HttpRequestException or FarmOperationException)
                {
                    logger.LogError(error, "Could not load owner join requests");
                    panel = new(farmId, null, true, "Join requests are unavailable. Refresh this page to try again.");
                }
            }
            return View(new FarmDashboardViewModel(await reports.DashboardAsync(farmId, actor, ct), panel));
        }
        catch (FarmOperationException error) when (error.Error == FarmError.NotFound)
        { return NotFound(); }
    }
}


## Repository file: src/SmartFarm.Web/Clients/HarvestCsv.cs

using System.Globalization;
using System.Text;
using SmartFarm.Core.Contracts.Reports;

namespace SmartFarm.Web.Clients;

// Export only data visible to the signed-in farm member.
public static class HarvestCsv
{
    // Escape quotes, separators and spreadsheet formulas in untrusted names/notes.
    public static string Cell(string? value)
    {
        var text = value ?? "";
        if (text.TrimStart().FirstOrDefault() is '=' or '+' or '-' or '@' ||
            text.Length > 0 && text[0] is '\t' or '\r' or '\n')
            text = "'" + text;
        return "\"" + text.Replace("\"", "\"\"") + "\"";
    }

    public static string Build(HarvestReport report)
    {
        var csv = new StringBuilder();
        csv.AppendLine("Farm date,Crop,Plot,Quantity,Unit,Status,Recorded UTC,Notes");
        foreach (var entry in report.Entries)
        {
            csv.Append(Cell(entry.FarmDate.ToString("yyyy-MM-dd"))).Append(',')
                .Append(Cell(entry.CropTypeName)).Append(',')
                .Append(Cell(entry.PlotName)).Append(',')
                .Append(entry.Quantity.ToString("0.000", CultureInfo.InvariantCulture)).Append(',')
                .Append(Cell(entry.Unit)).Append(',')
                .Append(Cell(entry.IsVoided ? "Voided" : "Valid")).Append(',')
                .Append(Cell(entry.HarvestedAtUtc.ToString("yyyy-MM-dd HH:mm:ss",
                    CultureInfo.InvariantCulture))).Append(',')
                .AppendLine(Cell(entry.Notes));
        }
        return csv.ToString();
    }
}
