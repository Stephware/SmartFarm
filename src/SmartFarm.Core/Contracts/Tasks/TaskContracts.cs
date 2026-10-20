namespace SmartFarm.Core.Contracts.Tasks;

public sealed record FarmTaskInput(
    byte TaskType,
    DateOnly DueDate,
    string? Description,
    Guid? CropCycleId,
    Guid? AssignedToUserId);

public sealed record FarmTaskUpdateInput(
    DateOnly DueDate,
    string? Description,
    Guid? AssignedToUserId,
    string RowVersion);

public sealed record FarmTaskCloseInput(byte Status, string RowVersion);

public sealed record FarmTaskDetails(
    Guid Id, Guid FarmId, Guid? CropCycleId, Guid? AssignedToUserId,
    byte TaskType, DateOnly DueDate, byte Status, string? Description,
    DateTime CreatedAtUtc, DateTime? ClosedAtUtc, string RowVersion);

public sealed record NotificationDetails(
    Guid Id, byte Type, string Title, string Message,
    Guid? FarmTaskId, Guid? WeatherAdvisoryId,
    DateTime CreatedAtUtc, DateTime? ReadAtUtc);
