using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Api.Models.Auth;

public sealed class RefreshTokenInput
{
    [Required]
    public string RefreshToken { get; set; } = string.Empty;
}
