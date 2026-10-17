using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Auth;

namespace SmartFarm.Web.Controllers;

public sealed class AccountController(
    UserManager<SmartFarmUser> users,
    SignInManager<SmartFarmUser> signIn,
    AccountRegistrationService registration) : Controller
{
    [AllowAnonymous, HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        if (User.Identity?.IsAuthenticated == true) return RedirectToAction("Index", "Home");
        ViewData["ReturnUrl"] = returnUrl;
        return View(new LoginInput());
    }

    [AllowAnonymous, HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Login(LoginInput input, string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        if (!ModelState.IsValid) return View(input);

        var user = await users.FindByEmailAsync(input.Email.Trim());
        if (user is null || user.DisabledAtUtc is not null)
        {
            ModelState.AddModelError(string.Empty, "Invalid email or password.");
            return View(input);
        }
        var result = await signIn.PasswordSignInAsync(user, input.Password, input.RememberMe, lockoutOnFailure: true);
        if (!result.Succeeded)
        {
            ModelState.AddModelError(string.Empty, "Invalid email or password, or account temporarily unavailable.");
            return View(input);
        }

        return Url.IsLocalUrl(returnUrl) ? LocalRedirect(returnUrl!) : RedirectToAction("Index", "Home");
    }

    [AllowAnonymous, HttpGet]
    public IActionResult Register() => View(new RegisterInput());

    [AllowAnonymous, HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Register(RegisterInput input)
    {
        if (!ModelState.IsValid) return View(input);
        var result = await registration.RegisterAsync(input.FullName, input.Email, input.Password);
        if (!result.Succeeded)
        {
            foreach (var error in result.Errors)
                ModelState.AddModelError(string.Empty, error.Description);
            return View(input);
        }

        TempData["Message"] = "Account created. Please sign in.";
        return RedirectToAction(nameof(Login));
    }

    [Authorize(AuthenticationSchemes = IdentityConstants.ApplicationScheme), HttpGet]
    public async Task<IActionResult> Profile()
    {
        var user = await users.GetUserAsync(User);
        if (user is null || user.DisabledAtUtc is not null) return Forbid();
        return View(user);
    }

    [Authorize(AuthenticationSchemes = IdentityConstants.ApplicationScheme), HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        await signIn.SignOutAsync();
        return RedirectToAction("Index", "Home");
    }

    [AllowAnonymous, HttpGet]
    public IActionResult AccessDenied() => View();
}

