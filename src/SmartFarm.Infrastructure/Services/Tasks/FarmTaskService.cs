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
