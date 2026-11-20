using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Web.ViewModels.Auth;

/// <summary>Presentation-only reset form. The API contract remains ResetPasswordInput.</summary>
public sealed class ResetPasswordForm
{
    [Required, EmailAddress, StringLength(256)]
    public string Email { get; set; } = "";

    [Required, StringLength(4096)]
    public string Token { get; set; } = "";

    [Required, StringLength(256, MinimumLength = 10)]
    public string Password { get; set; } = "";

    [Required, Compare(nameof(Password))]
    public string ConfirmPassword { get; set; } = "";
}
