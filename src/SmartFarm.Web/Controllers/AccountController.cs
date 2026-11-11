using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Security;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Auth;

using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Controllers;

public sealed class AccountController : Controller
{
    private readonly UserManager<SmartFarmUser> _users;
    private readonly SignInManager<SmartFarmUser> _signIn;
    private readonly AccountRegistrationService _registration;

    public AccountController(
        UserManager<SmartFarmUser> users,
        SignInManager<SmartFarmUser> signIn,
        AccountRegistrationService registration)
    {
        _users = users;
        _signIn = signIn;
        _registration = registration;
    }

    [AllowAnonymous, HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        if (User.Identity?.IsAuthenticated == true) return RedirectToAction("Index", "Farms");
        ViewData["ReturnUrl"] = returnUrl;
        return View(new LoginInput());
    }

    [AllowAnonymous, HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Login)]
    public async Task<IActionResult> Login(LoginInput input, string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        if (!ModelState.IsValid) return View(input);

        var user = await _users.FindByEmailAsync(input.Email.Trim());
        if (user is null || user.DisabledAtUtc is not null)
        {
            ModelState.AddModelError(string.Empty, "Invalid email or password.");
            return View(input);
        }
        var result = await _signIn.PasswordSignInAsync(user, input.Password, input.RememberMe, lockoutOnFailure: true);
        if (result.RequiresTwoFactor)
            return RedirectToAction(nameof(TwoFactor), new { returnUrl });
        if (!result.Succeeded)
        {
            ModelState.AddModelError(string.Empty, "Invalid email or password, or account temporarily unavailable.");
            return View(input);
        }

        return await AfterSignIn(user, returnUrl);
    }

    // The two-factor user is stored in Identity's short-lived, protected 2FA cookie.
    [AllowAnonymous, HttpGet]
    [ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
    public async Task<IActionResult> TwoFactor(string? returnUrl = null)
    {
        if (await _signIn.GetTwoFactorAuthenticationUserAsync() is null)
            return RedirectToAction(nameof(Login));
        return View(new AdminMfaInput { ReturnUrl = returnUrl });
    }

    [AllowAnonymous, HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Login)]
    public async Task<IActionResult> TwoFactor(AdminMfaInput input)
    {
        var user = await _signIn.GetTwoFactorAuthenticationUserAsync();
        if (user is null || user.DisabledAtUtc is not null) return RedirectToAction(nameof(Login));
        if (!ModelState.IsValid) return View(input);
        var code = input.Code.Replace(" ", "").Replace("-", "");
        var result = await _signIn.TwoFactorAuthenticatorSignInAsync(code, isPersistent: false,
            rememberClient: false);
        if (result.IsLockedOut)
        {
            ModelState.AddModelError("", "Account temporarily locked. Try again later.");
            return View(input);
        }
        if (!result.Succeeded)
        {
            ModelState.AddModelError("", "Invalid authenticator code.");
            return View(input);
        }
        return await AfterSignIn(user, input.ReturnUrl);
    }

    [AllowAnonymous, HttpGet]
    [ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
    public async Task<IActionResult> RecoveryCode(string? returnUrl = null)
    {
        if (await _signIn.GetTwoFactorAuthenticationUserAsync() is null)
            return RedirectToAction(nameof(Login));
        return View(new AdminRecoveryInput { ReturnUrl = returnUrl });
    }

    [AllowAnonymous, HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Login)]
    public async Task<IActionResult> RecoveryCode(AdminRecoveryInput input)
    {
        var user = await _signIn.GetTwoFactorAuthenticationUserAsync();
        if (user is null || user.DisabledAtUtc is not null) return RedirectToAction(nameof(Login));
        if (!ModelState.IsValid) return View(input);
        var result = await _signIn.TwoFactorRecoveryCodeSignInAsync(
            input.Code.Replace(" ", "").Trim());
        if (!result.Succeeded)
        {
            ModelState.AddModelError("", result.IsLockedOut
                ? "Account temporarily locked. Try again later."
                : "Invalid or already used recovery code.");
            return View(input);
        }
        return await AfterSignIn(user, input.ReturnUrl);
    }

    private async Task<IActionResult> AfterSignIn(SmartFarmUser user, string? returnUrl)
    {
        if (await _users.IsInRoleAsync(user, SuperAdminSecurity.Role))
        {
            if (!await _users.GetTwoFactorEnabledAsync(user))
                return RedirectToAction("Setup", "Security", new { area = "Admin" });
            return LocalRedirect("/admin");
        }
        return Url.IsLocalUrl(returnUrl) ? LocalRedirect(returnUrl!) :
            RedirectToAction("Index", "Farms");
    }

    [AllowAnonymous, HttpGet]
    public IActionResult Register() => View(new RegisterInput());

    [AllowAnonymous, HttpPost, ValidateAntiForgeryToken, EnableRateLimiting(EnrollmentRateLimiting.Registration)]
    public async Task<IActionResult> Register(RegisterInput input)
    {
        if (!ModelState.IsValid) return View(input);
        var result = await _registration.RegisterAsync(input.FullName, input.Email, input.Password);
        if (!result.Succeeded)
        {
            foreach (var error in result.Errors)
                ModelState.AddModelError(string.Empty, error.Description);
            return View(input);
        }

        TempData["Message"] = "Account created. Sign in to create a farm or request access to an existing farm.";
        return RedirectToAction(nameof(Login));
    }

    [Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application), HttpGet]
    public async Task<IActionResult> Profile()
    {
        var user = await _users.GetUserAsync(User);
        if (user is null || user.DisabledAtUtc is not null) return Forbid();
        return View(user);
    }

    [Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Application), HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        await _signIn.SignOutAsync();
        return RedirectToAction("Index", "Home");
    }

    [AllowAnonymous, HttpGet]
    public IActionResult AccessDenied() => View();
}

