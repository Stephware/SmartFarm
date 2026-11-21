using System.ComponentModel.DataAnnotations;
namespace SmartFarm.Core.Contracts.Accounts;
public sealed class ForgotPasswordInput
{
    [Required, EmailAddress, StringLength(256)] public string Email { get; set; } = "";
}
public sealed class ResetPasswordInput
{
    [Required, EmailAddress, StringLength(256)] public string Email { get; set; } = "";
    [Required, StringLength(4096)] public string Token { get; set; } = "";
    [Required, StringLength(256, MinimumLength = 10)] public string Password { get; set; } = "";
}
