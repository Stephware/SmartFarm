using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
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
        if (User.Identity?.IsAuthenticated == true) return RedirectToAction("Index", "Home");
        ViewData["ReturnUrl"] = returnUrl;
        return View(new LoginInput());
    }

    [AllowAnonymous, HttpPost, ValidateAntiForgeryToken]
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
        var result = await _registration.RegisterAsync(input.FullName, input.Email, input.Password, input.JoinFarm, input.FarmCode);
        if (!result.Succeeded)
        {
            foreach (var error in result.Errors)
                ModelState.AddModelError(error.Code == "FarmCode" ? nameof(input.FarmCode) : string.Empty, error.Description);
            return View(input);
        }

        TempData["Message"] = input.JoinFarm
            ? "Account created. Sign in to check your farm request. Access begins after the owner approves."
            : "Account created. Please sign in to create your farm.";
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

