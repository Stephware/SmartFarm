using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Api.Models.Auth;

public sealed class RegisterInput : IValidatableObject
{
    [Required, StringLength(120, MinimumLength = 2)]
    public string FullName { get; set; } = string.Empty;

    [Required, EmailAddress, StringLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required, StringLength(128, MinimumLength = 10), DataType(DataType.Password)]
    public string Password { get; set; } = string.Empty;

    [DataType(DataType.Password)]
    public string? ConfirmPassword { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        if (ConfirmPassword is not null && ConfirmPassword != Password)
            yield return new ValidationResult("The password and confirmation password do not match.",
                [nameof(ConfirmPassword)]);
    }
}
