using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Web.ViewModels.Auth;

public sealed class RegisterInput : IValidatableObject
{
    public bool JoinFarm { get; set; } = true;

    [StringLength(32)]
    public string? FarmCode { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        if (JoinFarm && string.IsNullOrWhiteSpace(FarmCode))
            yield return new ValidationResult("Enter the farm code supplied by the owner.", [nameof(FarmCode)]);
    }

    [Required, StringLength(120, MinimumLength = 2)]
    public string FullName { get; set; } = string.Empty;

    [Required, EmailAddress, StringLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required, StringLength(128, MinimumLength = 10), DataType(DataType.Password)]
    public string Password { get; set; } = string.Empty;

    [Required, Compare(nameof(Password)), DataType(DataType.Password)]
    public string ConfirmPassword { get; set; } = string.Empty;
}

