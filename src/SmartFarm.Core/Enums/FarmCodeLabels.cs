namespace SmartFarm.Core.Enums;

// Central definitions for API labels, UI labels and supported input codes.
// Unknown persisted codes remain visible as "Unknown" rather than being
// silently presented as a valid status.
public sealed record FarmCodeOption(byte Code, string Name);

public static class FarmCodeLabels
{
    public static string Activity(byte code) => (FarmActivityType)code switch
    {
        FarmActivityType.Watering => "Watering",
        FarmActivityType.Fertilizing => "Fertilizing",
        FarmActivityType.Pesticide => "Pesticide",
        FarmActivityType.Weeding => "Weeding",
        FarmActivityType.Other => "Other",
        _ => "Unknown"
    };

    public static string Health(byte code) => (CropHealthStatus)code switch
    {
        CropHealthStatus.NotAssessed => "Not assessed",
        CropHealthStatus.Healthy => "Healthy",
        CropHealthStatus.NeedsAttention => "Needs attention",
        CropHealthStatus.Critical => "Critical",
        _ => "Unknown"
    };

    public static string Task(byte code) => (FarmTaskType)code switch
    {
        FarmTaskType.Planting => "Planting",
        FarmTaskType.Watering => "Watering",
        FarmTaskType.Fertilizing => "Fertilizing",
        FarmTaskType.Inspection => "Inspection",
        FarmTaskType.Harvest => "Harvest",
        FarmTaskType.Other => "Other",
        _ => "Unknown"
    };

    public static string TaskStatus(byte code) => (FarmTaskStatus)code switch
    {
        FarmTaskStatus.Pending => "Pending",
        FarmTaskStatus.Completed => "Completed",
        FarmTaskStatus.Skipped => "Skipped",
        FarmTaskStatus.Cancelled => "Cancelled",
        _ => "Unknown"
    };

    public static string MemberRole(byte code) => (FarmMemberRole)code switch
    {
        FarmMemberRole.Manager => "Manager",
        FarmMemberRole.FieldWorker => "Field Worker",
        _ => "Unknown"
    };

    public static string Notification(byte code) => (FarmNotificationType)code switch
    {
        FarmNotificationType.TaskReminder => "Task reminder",
        FarmNotificationType.WeatherAdvisory => "Weather advisory",
        _ => "Unknown"
    };

    public static string Advisory(byte code) => (WeatherAdvisoryType)code switch
    {
        WeatherAdvisoryType.HeavyRain => "Heavy rain",
        WeatherAdvisoryType.StrongWind => "Strong wind",
        WeatherAdvisoryType.HighTemperature => "High temperature",
        _ => "Unknown"
    };

    public static string Severity(byte code) => (WeatherAdvisorySeverity)code switch
    {
        WeatherAdvisorySeverity.Low => "Low",
        WeatherAdvisorySeverity.Warning => "Warning",
        WeatherAdvisorySeverity.Severe => "Severe",
        _ => "Unknown"
    };

    public static string AdvisoryStatus(byte code) => (WeatherAdvisoryStatus)code switch
    {
        WeatherAdvisoryStatus.Active => "Active",
        WeatherAdvisoryStatus.Resolved => "Resolved",
        _ => "Unknown"
    };

    public static string Category(InventoryCategory category) => category switch
    {
        InventoryCategory.Seeds => "Seeds",
        InventoryCategory.Fertilizers => "Fertilizers",
        InventoryCategory.Pesticides => "Pesticides",
        InventoryCategory.Other => "Other",
        _ => "Unknown"
    };

    public static string Transaction(StockTransactionType type) => type switch
    {
        StockTransactionType.Opening => "Opening",
        StockTransactionType.Receipt => "Receipt",
        StockTransactionType.Consumption => "Consumption",
        StockTransactionType.Adjustment => "Adjustment",
        StockTransactionType.Reversal => "Reversal",
        _ => "Unknown"
    };

    public static string CropStatus(CropCycleStatus status) =>
        Enum.IsDefined(status) ? status.ToString() : "Unknown";

    private static IReadOnlyList<FarmCodeOption> Options<T>(Func<byte, string> name)
        where T : struct, Enum =>
        Enum.GetValues<T>().Select(v => new FarmCodeOption(Convert.ToByte(v), name(Convert.ToByte(v)))).ToArray();

    public static IReadOnlyList<FarmCodeOption> Activities => Options<FarmActivityType>(Activity);
    public static IReadOnlyList<FarmCodeOption> HealthStatuses => Options<CropHealthStatus>(Health);
    public static IReadOnlyList<FarmCodeOption> TaskTypes => Options<FarmTaskType>(Task);
    public static IReadOnlyList<FarmCodeOption> TaskStatuses => Options<FarmTaskStatus>(TaskStatus);
    public static IReadOnlyList<FarmCodeOption> TaskCloseStatuses =>
        TaskStatuses.Where(item => item.Code != (byte)FarmTaskStatus.Pending).ToArray();
    public static IReadOnlyList<FarmCodeOption> MemberRoles => Options<FarmMemberRole>(MemberRole);
    public static IReadOnlyList<FarmCodeOption> NotificationTypes => Options<FarmNotificationType>(Notification);
    public static IReadOnlyList<FarmCodeOption> WeatherAdvisories => Options<WeatherAdvisoryType>(Advisory);
    public static IReadOnlyList<FarmCodeOption> WeatherSeverities => Options<WeatherAdvisorySeverity>(Severity);
    public static IReadOnlyList<FarmCodeOption> WeatherStatuses => Options<WeatherAdvisoryStatus>(AdvisoryStatus);
    public static IReadOnlyList<FarmCodeOption> InventoryCategories =>
        Options<InventoryCategory>(code => Category((InventoryCategory)code));
    public static IReadOnlyList<FarmCodeOption> InventoryMovementTypes =>
        Options<StockTransactionType>(code => Transaction((StockTransactionType)code))
            .Where(item => item.Code >= (byte)StockTransactionType.Receipt &&
                item.Code <= (byte)StockTransactionType.Adjustment).ToArray();
    public static IReadOnlyList<FarmCodeOption> CropStatuses =>
        Options<CropCycleStatus>(code => CropStatus((CropCycleStatus)code));
}
