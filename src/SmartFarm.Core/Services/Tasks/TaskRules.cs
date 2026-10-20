using SmartFarm.Core.Contracts.Tasks;

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

    // Deterministic keys keep recurring jobs safe to rerun.
    public static string ScheduleKey(Guid cropId, byte taskType, DateOnly due) =>
        $"cycle:{cropId:N}:type:{taskType}:date:{due:yyyyMMdd}";
}
