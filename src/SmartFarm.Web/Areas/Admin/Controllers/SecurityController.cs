using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Services.Admin;
using SmartFarm.Web.Authentication;
using SmartFarm.Web.ViewModels.Auth;

namespace SmartFarm.Web.Areas.Admin.Controllers;

// Bootstrap only: deliberately requires the role and an authenticated session,
// but cannot expose the admin dashboard until the second factor was verified.
[Area("Admin")]
[Route("admin/security")]
[Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application, Roles = SuperAdminSecurity.Role)]
[ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
public sealed class SecurityController(
    UserManager<SmartFarmUser> users,
    SignInManager<SmartFarmUser> signIn,
    AdminAuditWriter audit) : Controller
{
    [HttpGet("setup")]
    public async Task<IActionResult> Setup()
    {
        var user = await users.GetUserAsync(User);
        if (user is null || user.DisabledAtUtc is not null) return Forbid();
        if (await users.GetTwoFactorEnabledAsync(user))
            return RedirectToAction("Index", "Dashboard", new { area = "Admin" });
        return View(await GetSetupModel(user));
    }

    private async Task<AdminMfaSetupPage> GetSetupModel(SmartFarmUser user)
    {
        var key = await users.GetAuthenticatorKeyAsync(user);
        if (string.IsNullOrWhiteSpace(key))
        {
            var result = await users.ResetAuthenticatorKeyAsync(user);
            if (!result.Succeeded) throw new InvalidOperationException("Authenticator setup failed.");
            key = await users.GetAuthenticatorKeyAsync(user);
        }
        return new AdminMfaSetupPage(key ?? "", user.Email ?? "");
    }

    [HttpPost("setup")]
    [ValidateAntiForgeryToken]
    [EnableRateLimiting(EnrollmentRateLimiting.Login)]
    public async Task<IActionResult> Setup(AdminMfaSetupInput input)
    {
        var user = await users.GetUserAsync(User);
        if (user is null || user.DisabledAtUtc is not null) return Forbid();
        if (await users.GetTwoFactorEnabledAsync(user))
            return RedirectToAction("Index", "Dashboard", new { area = "Admin" });
        if (ModelState.IsValid &&
            await users.CheckPasswordAsync(user, input.CurrentPassword) &&
            await users.VerifyTwoFactorTokenAsync(user,
                TokenOptions.DefaultAuthenticatorProvider, Normalize(input.Code)))
        {
            var result = await users.SetTwoFactorEnabledAsync(user, true);
            if (!result.Succeeded) throw new InvalidOperationException("Authenticator activation failed.");
            var codes = await users.GenerateNewTwoFactorRecoveryCodesAsync(user, 8);
            await audit.RecordAsync(user.Id, "web", "admin-mfa-enabled", "user", user.Id);
            await signIn.SignOutAsync();
            return View("RecoveryCodes", codes?.ToArray() ?? []);
        }
        ModelState.AddModelError("", "Password or authenticator code was invalid.");
        return View(await GetSetupModel(user));
    }

    private static string Normalize(string code) =>
        (code ?? "").Replace(" ", "").Replace("-", "");
}
