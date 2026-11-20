using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc.ModelBinding.Metadata;
namespace SmartFarm.Web.Presentation;

// MVC presentation metadata only. Validation constraints and API contracts remain unchanged.
public sealed class LocalizedValidationMetadata : IDisplayMetadataProvider, IValidationMetadataProvider
{
    private static readonly Dictionary<string, string> Labels = new() {
        ["FullName"] = "Full name", ["LocationText"] = "Farm address or area", ["TimeZoneId"] = "Time zone",
        ["AreaSqMeters"] = "Area (m²)", ["PlotId"] = "Plot", ["CropTypeId"] = "Crop type",
        ["PlannedPlantingDate"] = "Planned planting date", ["ExpectedHarvestDate"] = "Expected harvest",
        ["WateringIntervalDays"] = "Watering interval days", ["HarvestUnit"] = "Harvest unit",
        ["DueDate"] = "Due date", ["AssignedToUserId"] = "Assigned member", ["CropCycleId"] = "Crop",
        ["QuantityUsed"] = "Quantity used", ["ReorderLevel"] = "Low-stock threshold",
        ["ConfirmPassword"] = "Confirm password", ["TerminationReason"] = "Reason",
        ["TaskType"] = "Task type", ["ActivityType"] = "Activity", ["HealthStatus"] = "Crop health",
        ["IssueCategory"] = "Observed issue", ["IssueSeverity"] = "Issue severity", ["GrowthStage"] = "Growth stage"
    };
    public void CreateDisplayMetadata(DisplayMetadataProviderContext context)
    {
        if (context.Key.Name is { } name && Labels.TryGetValue(name, out var label))
            context.DisplayMetadata.DisplayName = () => UiMessages.Text(label);
    }
    public void CreateValidationMetadata(ValidationMetadataProviderContext context)
    {
        foreach (var attribute in context.ValidationMetadata.ValidatorMetadata.OfType<ValidationAttribute>())
        {
            if (attribute.ErrorMessage is not null || attribute.ErrorMessageResourceType is not null) continue;
            attribute.ErrorMessage = attribute switch {
                RequiredAttribute => "The {0} field is required.",
                EmailAddressAttribute => "The {0} field is not a valid e-mail address.",
                StringLengthAttribute { MinimumLength: > 0 } => "The field {0} must be a string with a minimum length of {2} and a maximum length of {1}.",
                StringLengthAttribute => "The field {0} must be a string with a maximum length of {1}.",
                RangeAttribute => "The field {0} must be between {1} and {2}.",
                CompareAttribute => "The {0} and {1} fields do not match.",
                _ => attribute.ErrorMessage
            };
        }
    }
}
