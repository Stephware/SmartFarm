using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Core.Contracts.Accounts;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Web.Presentation;
using SmartFarm.Web.ViewModels.Auth;

namespace SmartFarm.Web.Controllers;

/// <summary>
/// Browser-facing pages for the existing Identity recovery service.
/// JSON clients continue to use the separate /api/auth recovery endpoints.
/// </summary>
[AllowAnonymous, ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
public sealed class AccountRecoveryController(AccountRecoveryService recovery) : Controller
{
    [HttpGet("/Account/ForgotPassword")]
    public IActionResult ForgotPassword() => View("ForgotPassword", new ForgotPasswordInput());

    [HttpGet("/Account/ResetPassword")]
    public IActionResult ResetPassword([FromQuery] string? email, [FromQuery] string? token)
    {
        Response.Headers.ReferrerPolicy = "no-referrer";
        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(token))
            return View("RecoveryInvalid");
        return View("ResetPassword", new ResetPasswordForm { Email = email, Token = token });
    }

    [HttpGet("/Account/ForgotPassword/Sent")]
    public IActionResult Sent() => View("ForgotPasswordSent");

    [HttpGet("/Account/ResetPassword/Complete")]
    public IActionResult Complete() => View("ResetPasswordComplete");

    [HttpPost("/Account/ForgotPassword"), ValidateAntiForgeryToken,
     EnableRateLimiting(EnrollmentRateLimiting.PasswordRecovery)]
    public async Task<IActionResult> Forgot(ForgotPasswordInput input, CancellationToken ct)
    {
        if (!ModelState.IsValid) return View("ForgotPassword", input);
        await recovery.RequestAsync(input.Email, ct);
        // Never disclose whether this email has an AniGo account or SMTP is enabled.
        return RedirectToAction(nameof(Sent));
    }

    [HttpPost("/Account/ResetPassword"), ValidateAntiForgeryToken,
     EnableRateLimiting(EnrollmentRateLimiting.PasswordReset)]
    public async Task<IActionResult> Reset(ResetPasswordForm input, CancellationToken ct)
    {
        Response.Headers.ReferrerPolicy = "no-referrer";
        if (!ModelState.IsValid) return View("ResetPassword", input);
        var valid = await recovery.ResetAsync(new ResetPasswordInput
        {
            Email = input.Email,
            Token = input.Token,
            Password = input.Password
        }, ct);
        if (!valid)
        {
            ModelState.AddModelError(string.Empty,
                UiMessages.Text("The reset link is invalid, expired or has already been used. Request a new link."));
            return View("ResetPassword", input);
        }
        // The redirect removes both password fields and the token from the address bar.
        return RedirectToAction(nameof(Complete));
    }
}
