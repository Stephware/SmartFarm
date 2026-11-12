using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Web.ViewModels.Auth;

public sealed class AdminMfaInput
{
    [Required, StringLength(12, MinimumLength = 6)]
    public string Code { get; set; } = "";
    public string? ReturnUrl { get; set; }
}

public sealed class AdminRecoveryInput
{
    [Required, StringLength(32, MinimumLength = 8)]
    public string Code { get; set; } = "";
    public string? ReturnUrl { get; set; }
}

public sealed class AdminMfaSetupInput
{
    [Required]
    public string CurrentPassword { get; set; } = "";
    [Required, StringLength(12, MinimumLength = 6)]
    public string Code { get; set; } = "";
}

public sealed record AdminMfaSetupPage(string AuthenticatorKey, string Email);
