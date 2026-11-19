using Microsoft.AspNetCore.Antiforgery;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Core.Contracts.Accounts;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Security;
namespace SmartFarm.Web.Controllers;
// Backend form contracts only; frontend owns Razor presentation.
[AllowAnonymous, ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
public sealed class AccountRecoveryController(AccountRecoveryService recovery, IAntiforgery antiforgery) : Controller
{
    [HttpGet("/Account/ForgotPassword")]
    public IActionResult ForgotContract() => Contract("recovery.forgot_form", null, null);
    [HttpGet("/Account/ResetPassword")]
    public IActionResult ResetContract([FromQuery] string? email, [FromQuery] string? token) => Contract("recovery.reset_form", email, token);
    private IActionResult Contract(string code, string? email, string? token)
    {
        Response.Headers["Referrer-Policy"] = "no-referrer";
        return Ok(new { code, email, token, antiforgeryToken = antiforgery.GetAndStoreTokens(HttpContext).RequestToken });
    }
    [HttpPost("/Account/ForgotPassword"), ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.PasswordRecovery)]
    public async Task<IActionResult> Forgot(ForgotPasswordInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest(new { code = "validation.invalid" });
        await recovery.RequestAsync(input.Email, ct);
        return Ok(new { code = "recovery.requested" });
    }
    [HttpPost("/Account/ResetPassword"), ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.PasswordReset)]
    public async Task<IActionResult> Reset(ResetPasswordInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid) return BadRequest(new { code = "validation.invalid" });
        return await recovery.ResetAsync(input, ct) ? Ok(new { code = "recovery.completed" }) : BadRequest(new { code = "recovery.invalid" });
    }
}
