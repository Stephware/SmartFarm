using System.Text.Json.Serialization;
using SmartFarm.Core.Contracts;
using SmartFarm.Core.Enums;

namespace SmartFarm.Core.Contracts.Tasks;

public sealed record FarmTaskInput(
    [property: JsonConverter(typeof(TaskTypeJsonConverter))] byte TaskType,
    DateOnly DueDate,
    string? Description,
    Guid? CropCycleId,
    Guid? AssignedToUserId);

public sealed record FarmTaskUpdateInput(
    DateOnly DueDate,
    string? Description,
    Guid? AssignedToUserId,
    string RowVersion);

public sealed record FarmTaskCloseInput(
    [property: JsonConverter(typeof(TaskCloseStatusJsonConverter))] byte Status, string RowVersion);

public sealed record FarmTaskDetails(
    Guid Id, Guid FarmId, Guid? CropCycleId, Guid? AssignedToUserId,
    byte TaskType, DateOnly DueDate, byte Status, string? Description,
    DateTime CreatedAtUtc, DateTime? ClosedAtUtc, string RowVersion)
{
    public string TaskTypeName => FarmCodeLabels.Task(TaskType);
    public string StatusName => FarmCodeLabels.TaskStatus(Status);
}

public sealed record NotificationDetails(
    Guid Id, byte Type, string Title, string Message,
    Guid? FarmTaskId, Guid? WeatherAdvisoryId,
    DateTime CreatedAtUtc, DateTime? ReadAtUtc)
{
    public string TypeName => FarmCodeLabels.Notification(Type);
}
