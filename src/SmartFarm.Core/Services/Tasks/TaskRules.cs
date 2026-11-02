using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Services.Tasks;

public static class TaskRules
{
    public static bool IsValidType(byte type) => Enum.IsDefined((FarmTaskType)type);
    public static bool IsValidCloseStatus(byte status) =>
        status >= (byte)FarmTaskStatus.Completed && status <= (byte)FarmTaskStatus.Cancelled;

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

    public static byte? ForActivity(byte activityType)
    {
        if (activityType == (byte)FarmActivityType.Watering)
            return (byte)FarmTaskType.Watering;
        if (activityType == (byte)FarmActivityType.Fertilizing)
            return (byte)FarmTaskType.Fertilizing;
        return null;
    }

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
            for (var dueDay = planted.DayNumber + next * interval;
                dueDay <= lastDay.DayNumber; dueDay += interval)
            {
                if (dueDay >= today.DayNumber) yield return (2, DateOnly.FromDayNumber(dueDay));
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
