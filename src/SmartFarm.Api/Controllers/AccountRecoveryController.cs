using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Core.Contracts.Accounts;
using SmartFarm.Core.Services;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Security;
namespace SmartFarm.Api.Controllers;
[ApiController, AllowAnonymous, Route("api/auth"), ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
public sealed class AccountRecoveryController(AccountRecoveryService recovery) : ControllerBase
{
    [HttpPost("forgot-password"), EnableRateLimiting(EnrollmentRateLimiting.PasswordRecovery)]
    public async Task<IActionResult> Forgot(ForgotPasswordInput input, CancellationToken ct)
    {
        await recovery.RequestAsync(input.Email, ct);
        return Ok(new { code = "recovery.requested", message = BackendMessages.Text("recovery.requested", Request.Headers.AcceptLanguage.ToString()) });
    }
    [HttpPost("reset-password"), EnableRateLimiting(EnrollmentRateLimiting.PasswordReset)]
    public async Task<IActionResult> Reset(ResetPasswordInput input, CancellationToken ct) =>
        await recovery.ResetAsync(input, ct) ? Ok(new { code = "recovery.completed" }) :
            BadRequest(new { code = "recovery.invalid", message = BackendMessages.Text("recovery.invalid", Request.Headers.AcceptLanguage.ToString()) });
}
