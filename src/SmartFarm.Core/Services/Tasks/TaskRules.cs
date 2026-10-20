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

    // Deterministic keys keep recurring jobs safe to rerun.
    public static string ScheduleKey(Guid cropId, byte taskType, DateOnly due) =>
        $"cycle:{cropId:N}:type:{taskType}:date:{due:yyyyMMdd}";
}
