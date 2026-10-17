using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Web.ViewModels.Auth;

public sealed class RefreshTokenInput
{
    [Required]
    public string RefreshToken { get; set; } = string.Empty;
}

