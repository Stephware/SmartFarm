using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Api.Models.Auth;

public sealed class LoginInput
{
    [Required, EmailAddress, StringLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required, DataType(DataType.Password)]
    public string Password { get; set; } = string.Empty;

    public bool RememberMe { get; set; }
}
