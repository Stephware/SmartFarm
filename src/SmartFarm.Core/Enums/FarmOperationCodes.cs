namespace SmartFarm.Core.Enums;

// Existing SQL values are fixed. Keep their numeric values for older Web,
// Android and offline clients while exposing readable API names separately.
public enum FarmActivityType : byte
{
    Watering = 1,
    Fertilizing = 2,
    Pesticide = 3,
    Weeding = 4,
    Other = 5
}

public enum CropHealthStatus : byte
{
    NotAssessed = 0,
    Healthy = 1,
    NeedsAttention = 2,
    Critical = 3
}

public enum FarmTaskType : byte
{
    Planting = 1,
    Watering = 2,
    Fertilizing = 3,
    Inspection = 4,
    Harvest = 5,
    Other = 6
}

public enum FarmTaskStatus : byte
{
    Pending = 0,
    Completed = 1,
    Skipped = 2,
    Cancelled = 3
}

public enum FarmMemberRole : byte
{
    Manager = 1,
    FieldWorker = 2
}

public enum FarmNotificationType : byte
{
    TaskReminder = 1,
    WeatherAdvisory = 2
}

public enum WeatherAdvisoryType : byte
{
    HeavyRain = 1,
    StrongWind = 2,
    HighTemperature = 3
}

public enum WeatherAdvisorySeverity : byte
{
    Low = 1,
    Warning = 2,
    Severe = 3
}

public enum WeatherAdvisoryStatus : byte
{
    Active = 0,
    Resolved = 1
}
