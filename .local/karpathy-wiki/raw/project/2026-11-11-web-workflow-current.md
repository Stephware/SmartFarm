# SmartFarm web-workflow-current source capture

> Source: local repository at ac624be7e5e721f00d990d01f720c682f1977930
> Collected: 2026-11-11
> Published: Unknown

Source evidence only. No build, deployment, SQL or device success is implied.

## Review instructions and local history

Documentation-only synchronization authorized by user on 2026-11-11. Unignore Karpathy Wiki, Superpowers docs and AGENTS.md permanently; preserve secrets/generated/private runtime exclusions. Do not change application/schema/business code, commit, push, merge or create PRs. Future workflow: Karpathy → Superpowers → Ponytail → Impeccable (for UI/UX) → Testing → Knowledge Update. Follow actual installed skills, use only relevant workflows, preserve security, validation, error handling, accessibility, correctness and essential tests.

```text
ac624be Merge Pine and Gold frontend polish
69a2c28 Merge Pine and Gold weather and reports
7a0716b Merge Pine and Gold inventory and harvest UI
1a12e8f Merge Pine and Gold fieldwork UI
dcfbc34 Merge Pine and Gold crop illustrations
0a2ed4f Merge Pine and Gold dashboard redesign
f96bf33 Merge Harvest Gold + Pine UI foundation
cfcabb9 Merge pull request #78 from Stephware/feature/phase-d3-cycle-export
a8e7c94 Merge pull request #77 from Stephware/feature/phase-d2-finance-insights
a5e592d Merge pull request #76 from Stephware/feature/phase-d1-yield-comparisons
9d7a903 Merge pull request #75 from Stephware/feature/phase-c2-activity-stock
c4e7f61 Merge pull request #74 from Stephware/feature/phase-c1-crop-health
1f6440b Merge pull request #73 from Stephware/feature/phase-b-daily-work
3016c31 Merge pull request #72 from Stephware/feature/phase-a-stability
a11f936 Merge pull request #71 from Stephware/feature/ui-overhaul-phase-8
a266d11 Merge pull request #70 from Stephware/feature/ui-overhaul-phase-7
fcdbe8a Merge pull request #69 from Stephware/feature/ui-overhaul-phase-6
8619bde Merge pull request #68 from Stephware/feature/ui-overhaul-phase-5
f7541ef Merge weather freshness and cache fixes
d5a28b7 Merge Android session and error fixes
ed9a513 Merge pull request #65 from Stephware/feature/ui-overhaul-phase-4
9a345b7 Merge pull request #64 from Stephware/feature/ui-overhaul-phase-3
6bcb9d1 Merge pull request #63 from Stephware/fix/ui-phase-2-smoke-copy
22fed90 Merge pull request #62 from Stephware/feature/ui-overhaul-phase-2
34710c7 Merge pull request #61 from Stephware/feature/ui-overhaul-phase-1
076d0e1 Merge full system verification checks
d5a57c7 Merge offline sync reliability fixes
a24c113 Merge Android photo viewing improvements
2476fb0 Merge image verification and quality fixes
51c9d27 Merge pull request #55 from Stephware/feature/enable-hangfire-worker
8e6a776 Merge pull request #54 from Stephware/feature/free-farm-location-picker
4553261 Merge PR #53: Phase 3 plot layout and finance
6ddcfdf Merge PR #52: Phase 2 farm improvements
13f2c42 Merge pull request #51 from Stephware/feat/phase1-farm-usability
aa9de5f Merge PR #50: improve farm enrollment
10cb863 Merge PR #49: readable API codes
de548ef Add crop agenda, shared photo storage, and AniGo branding
53d4b38 Replace invalid JPEG photo test fixture
5d69ea1 Fix report route and database health tests
d4f7ab2 Merge pull request #45 from Stephware/chore/ignore-local-docs
59c5f46 Merge pull request #44 from Stephware/feature/farmer-registration-approval
0ce304a Merge pull request #43 from Stephware/ui/consistent-responsive-layouts
d4508e0 Merge pull request #42 from Stephware/ui/navigation-practical-inputs
910656d Merge pull request #41 from Stephware/ui/android-drafts-offline-feedback
460f3e6 Merge pull request #40 from Stephware/ui/safe-actions
f0d7e6e Merge pull request #39 from Stephware/ui/forms-recovery-accessibility
0bbe3c2 Merge pull request #38 from Stephware/feature/mvc-shared-services
2c41323 Merge pull request #37 from Stephware/feature/android-glide-compression-cache
```

## `src/SmartFarm.Web/Views/Shared/_Layout.cshtml`

````text
@{
    var signedIn = User.Identity?.IsAuthenticated == true;
    var currentController = ViewContext.RouteData.Values["controller"]?.ToString();
    var currentAction = ViewContext.RouteData.Values["action"]?.ToString();
}
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="theme-color" content="#153D2D" />
    <title>@ViewData["Title"] - AniGo</title>
    <link rel="stylesheet" href="~/css/site.css" asp-append-version="true" />
    <link rel="stylesheet" href="~/css/dashboard.css" asp-append-version="true" />
    <link rel="stylesheet" href="~/css/crop-art.css" asp-append-version="true" />
    <link rel="stylesheet" href="~/css/fieldwork.css" asp-append-version="true" />
    <link rel="stylesheet" href="~/css/harvest-inventory.css" asp-append-version="true" />
    <link rel="stylesheet" href="~/css/insights.css" asp-append-version="true" />
    <link rel="stylesheet" href="~/css/access-polish.css" asp-append-version="true" />
    <script src="~/js/site.js" asp-append-version="true" defer></script>
    <script src="~/js/access-polish.js" asp-append-version="true" defer></script>
</head>
<body class="@(signedIn ? "is-authenticated" : "is-guest")">
    <a class="skip-link" href="#main-content">Skip to content</a>
    @if (signedIn)
    {
        <div class="app-shell">
            <aside class="app-sidebar" aria-label="Workspace navigation">
                <a class="brand sidebar-brand" asp-controller="Farms" asp-action="Index">
                    <span class="brand-badge" aria-hidden="true">A</span>
                    <span>AniGo<small>Farm management</small></span>
                </a>
                <p class="sidebar-caption">WORKSPACE</p>
                <nav class="sidebar-links" aria-label="Account and farm selection">
                    <a asp-controller="Farms" asp-action="Index" data-nav-icon="farms"
                       aria-current="@(currentController == "Farms" && currentAction == "Index" ? "page" : null)">My farms</a>
                    <a asp-controller="Notifications" asp-action="Index" data-nav-icon="notifications"
                       aria-current="@(currentController == "Notifications" ? "page" : null)">Notifications</a>
                    <a asp-controller="Account" asp-action="Profile" data-nav-icon="account"
                       aria-current="@(currentController == "Account" && currentAction == "Profile" ? "page" : null)">My account</a>
                </nav>
                @* The farm links come from _FarmNavigation, not duplicated route definitions. *@
                <div id="sidebar-farm-group" class="sidebar-farm-group" hidden>
                    <p class="sidebar-caption">FARM MANAGEMENT</p>
                    <nav id="sidebar-farm-links" class="sidebar-links" aria-label="Current farm sections"></nav>
                </div>
                <div class="sidebar-bottom">
                    <a class="sf-farm-switch" asp-controller="Farms" asp-action="Index">
                        <span aria-hidden="true">↔</span> Switch farm
                    </a>
                    <form method="post" asp-controller="Account" asp-action="Logout" class="logout-form">
                        <button type="submit" class="sidebar-logout">Log out</button>
                    </form>
                </div>
            </aside>
            <div class="app-body">
                <header class="site-header app-topbar">
                    <div class="app-topbar-title">
                        <a class="mobile-brand" asp-controller="Farms" asp-action="Index">
                            <span class="brand-badge" aria-hidden="true">A</span>AniGo
                        </a>
                        <span class="topbar-label">AniGo / Farm workspace</span>
                        <strong>@(ViewData["Title"] ?? "Overview")</strong>
                    </div>
                    <nav class="app-topbar-actions" aria-label="Quick account links">
                        <a asp-controller="Notifications" asp-action="Index" data-nav-icon="notifications">Notifications</a>
                        <a asp-controller="Account" asp-action="Profile" data-nav-icon="account">Account</a>
                    </nav>
                    <details class="mobile-nav" id="mobile-workspace-menu">
                        <summary>Menu</summary>
                        <nav aria-label="Mobile workspace navigation">
                            <div id="mobile-farm-links" class="mobile-farm-links" hidden></div>
                            <a asp-controller="Farms" asp-action="Index">My farms</a>
                            <a asp-controller="Notifications" asp-action="Index">Notifications</a>
                            <a asp-controller="Account" asp-action="Profile">My account</a>
                            <form method="post" asp-controller="Account" asp-action="Logout" class="logout-form">
                                <button type="submit" class="link-button">Log out</button>
                            </form>
                        </nav>
                    </details>
                </header>
                <main class="page app-page" id="main-content" tabindex="-1">
                    <partial name="_ValidationSummary" />
                    @RenderBody()
                </main>
            </div>
            <nav class="mobile-workspace-bar" id="mobile-workspace-bar" aria-label="Farm quick navigation" hidden>
                <div id="mobile-workspace-links" class="mobile-workspace-links"></div>
                <button type="button" class="mobile-more-button" id="mobile-more-button" aria-label="More farm sections">More</button>
            </nav>
        </div>
    }
    else
    {
        <header class="site-header guest-topbar">
            <a class="brand" asp-controller="Home" asp-action="Index">
                <span class="brand-badge" aria-hidden="true">A</span>AniGo
            </a>
            <nav aria-label="Main navigation">
                <a asp-controller="Account" asp-action="Login">Log in</a>
                <a class="button guest-nav-cta" asp-controller="Account" asp-action="Register">Create account</a>
            </nav>
        </header>
        <main class="page guest-page" id="main-content" tabindex="-1">
            <partial name="_ValidationSummary" />
            @RenderBody()
        </main>
    }
</body>
</html>
````

## `src/SmartFarm.Web/Views/Shared/_FarmNavigation.cshtml`

````text
@model Guid
@{ var current = ViewContext.RouteData.Values["controller"]?.ToString(); }
<nav class="farm-navigation" aria-label="Farm sections">
    <a asp-controller="Farms" asp-action="Details" asp-route-id="@Model" aria-current="@(current == "Farms" ? "page" : null)">Farm overview</a>
    <a asp-controller="Dashboard" asp-action="Index" asp-route-farmId="@Model" aria-current="@(current == "Dashboard" ? "page" : null)">Dashboard</a>
    <a asp-controller="Tasks" asp-action="Index" asp-route-farmId="@Model" aria-current="@(current == "Tasks" ? "page" : null)">Tasks</a>
    <a asp-controller="Crops" asp-action="Index" asp-route-farmId="@Model" aria-current="@(current is "Crops" or "Monitoring" or "Harvests" ? "page" : null)">Crops</a>
    <a asp-controller="PlotLayout" asp-action="Index" asp-route-farmId="@Model" aria-current="@(current == "PlotLayout" ? "page" : null)">Plot layout</a>
    <a asp-controller="FarmFinance" asp-action="Index" asp-route-farmId="@Model" aria-current="@(current == "FarmFinance" ? "page" : null)">Finances</a>
    <a asp-controller="Inventory" asp-action="Index" asp-route-farmId="@Model" aria-current="@(current == "Inventory" ? "page" : null)">Inventory</a>
    <a asp-controller="Weather" asp-action="Index" asp-route-farmId="@Model" aria-current="@(current == "Weather" ? "page" : null)">Weather</a>
    <a asp-controller="Reports" asp-action="Harvests" asp-route-farmId="@Model" aria-current="@(current == "Reports" ? "page" : null)">Reports</a>
</nav>
````

## `src/SmartFarm.Web/Controllers/AccountController.cs`

````text
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
        if (!result.Succeeded)
        {
            ModelState.AddModelError(string.Empty, "Invalid email or password, or account temporarily unavailable.");
            return View(input);
        }

        return Url.IsLocalUrl(returnUrl) ? LocalRedirect(returnUrl!) : RedirectToAction("Index", "Farms");
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
````

## `src/SmartFarm.Web/Views/Account/Register.cshtml`

````text
@model SmartFarm.Web.ViewModels.Auth.RegisterInput
@{
    ViewData["Title"] = "Register";
}
<div class="auth-layout">
    <aside class="auth-intro">
        <p class="auth-kicker">GROW WITH ANIGO</p>
        <h2>A simpler way to manage your farm.</h2>
        <p>Set up your own farm or request to join an existing farm using its owner's code.</p>
        <div class="ani-auth-illustration" aria-hidden="true">
            <img src="@Url.Content("~/images/crops/sprout.svg")" alt="" width="120" height="120" />
            <img src="@Url.Content("~/images/crops/corn.svg")" alt="" width="120" height="120" />
            <img src="@Url.Content("~/images/crops/rice.svg")" alt="" width="120" height="120" />
        </div>
        <div class="auth-intro-foot">One account can access multiple farms.</div>
    </aside>
    <section class="panel auth-card" aria-labelledby="register-heading">
        <div class="auth-heading">
            <p class="auth-kicker">GET STARTED</p>
            <h1 id="register-heading">Create your account</h1>
            <p>Set up your profile to start managing or joining farms.</p>
        </div>
        <form method="post" asp-action="Register" asp-controller="Account" class="auth-form">
            <div class="form-field">
                <label asp-for="FullName">Full name</label>
                <input asp-for="FullName" autocomplete="name" required />
                <span asp-validation-for="FullName" class="error"></span>
            </div>
            <div class="form-field">
                <label asp-for="Email"></label>
                <input asp-for="Email" type="email" autocomplete="email" inputmode="email"
                       autocapitalize="none" spellcheck="false" required />
                <span asp-validation-for="Email" class="error"></span>
            </div>
            <div class="form-field">
                <label asp-for="Password"></label>
                <div class="ani-password-field">
                    <input asp-for="Password" type="password" autocomplete="new-password"
                           aria-describedby="password-guidance" required minlength="10" />
                    <button type="button" class="ani-password-toggle" data-password-toggle
                            aria-controls="Password" aria-pressed="false">Show</button>
                </div>
                <span asp-validation-for="Password" class="error"></span>
                <p class="hint" id="password-guidance">Use at least 10 characters, including uppercase, lowercase, and a number.</p>
            </div>
            <button class="auth-submit" type="submit">Create account</button>
        </form>
        <p class="ani-auth-fineprint">After registration, sign in to create a farm or request to join an existing one.</p>
        <p class="auth-switch">Already have an account? <a asp-controller="Account" asp-action="Login">Log in</a>.</p>
    </section>
</div>
````

## `src/SmartFarm.Api/Models/Auth/RegisterInput.cs`

````text
using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Api.Models.Auth;

public sealed class RegisterInput : IValidatableObject
{
    [Required, StringLength(120, MinimumLength = 2)]
    public string FullName { get; set; } = string.Empty;

    [Required, EmailAddress, StringLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required, StringLength(128, MinimumLength = 10), DataType(DataType.Password)]
    public string Password { get; set; } = string.Empty;

    [DataType(DataType.Password)]
    public string? ConfirmPassword { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        if (ConfirmPassword is not null && ConfirmPassword != Password)
            yield return new ValidationResult("The password and confirmation password do not match.",
                [nameof(ConfirmPassword)]);
    }
}
````

## `src/SmartFarm.Api/Controllers/AuthController.cs`

````text
using Microsoft.AspNetCore.Authentication;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Api.Models.Auth;

using SmartFarm.Api.Authentication;
using Microsoft.AspNetCore.RateLimiting;
using SmartFarm.Infrastructure.Security;

namespace SmartFarm.Api.Controllers;

[ApiController]
[Route("api/auth")]
public sealed class AuthController(
    UserManager<SmartFarmUser> users,
    SignInManager<SmartFarmUser> signIn,
    AccountRegistrationService registration,
    IOptionsMonitor<BearerTokenOptions> tokenOptions) : ControllerBase
{
    [AllowAnonymous, HttpPost("register")]
    [EnableRateLimiting(EnrollmentRateLimiting.Registration)]
    public async Task<IActionResult> Register([FromBody] RegisterInput input)
    {
        var result = await registration.RegisterAsync(input.FullName, input.Email, input.Password);
        if (!result.Succeeded)
            return BadRequest(new { errors = result.Errors.Select(error => error.Description) });

        return StatusCode(StatusCodes.Status201Created, new { message = "Account created. Please sign in." });
    }

    [AllowAnonymous, HttpPost("login")]
    [EnableRateLimiting(EnrollmentRateLimiting.Login)]
    public async Task<IActionResult> Login([FromBody] LoginInput input)
    {
        var user = await users.FindByEmailAsync(input.Email.Trim());
        if (user is null || user.DisabledAtUtc is not null)
            return Unauthorized(new { message = "Invalid email or password." });

        var result = await signIn.CheckPasswordSignInAsync(user, input.Password, lockoutOnFailure: true);
        if (!result.Succeeded)
            return Unauthorized(new { message = "Invalid email or password, or account temporarily unavailable." });

        var principal = await signIn.CreateUserPrincipalAsync(user);
        await HttpContext.SignInAsync(IdentityConstants.BearerScheme, principal);
        // The bearer handler writes tokenType/accessToken/expiresIn/refreshToken.
        return new EmptyResult();
    }

    [AllowAnonymous, HttpPost("refresh")]
    public async Task<IActionResult> Refresh([FromBody] RefreshTokenInput input)
    {
        AuthenticationTicket? ticket;
        try
        {
            ticket = tokenOptions.Get(IdentityConstants.BearerScheme)
                .RefreshTokenProtector.Unprotect(input.RefreshToken);
        }
        catch (Exception exception) when (exception is FormatException or ArgumentException or CryptographicException)
        {
            return Unauthorized();
        }

        if (ticket?.Properties?.ExpiresUtc is not { } expiration ||
            expiration <= DateTimeOffset.UtcNow ||
            await signIn.ValidateSecurityStampAsync(ticket.Principal) is not { } user ||
            user.DisabledAtUtc is not null)
            return Unauthorized();

        var principal = await signIn.CreateUserPrincipalAsync(user);
        await HttpContext.SignInAsync(IdentityConstants.BearerScheme, principal);
        return new EmptyResult();
    }

    [Authorize(AuthenticationSchemes = SmartFarmAuthSchemes.Bearer), HttpGet("me")]
    public async Task<IActionResult> Me()
    {
        var user = await users.GetUserAsync(User);
        if (user is null || user.DisabledAtUtc is not null) return Unauthorized();
        return Ok(new { id = user.Id, fullName = user.FullName, email = user.Email });
    }
}
````

## `src/SmartFarm.Web/Presentation/CropArtwork.cs`

````text
namespace SmartFarm.Web.Presentation;

/// <summary>Identifies a crop icon by its existing display name. No status or growth is inferred.</summary>
public static class CropArtwork
{
    public static string FileName(string? cropName)
    {
        var name = cropName?.Trim().ToLowerInvariant() ?? "";
        if (name.Contains("tomato") || name.Contains("kamatis")) return "tomato.svg";
        if (name.Contains("eggplant") || name.Contains("talong") || name.Contains("aubergine")) return "eggplant.svg";
        if (name.Contains("rice") || name.Contains("palay") || name.Contains("paddy")) return "rice.svg";
        if (name.Contains("corn") || name.Contains("maize") || name.Contains("mais")) return "corn.svg";
        return "sprout.svg";
    }
}
````

## `.github/workflows/verify-smartfarm.yml`

````text
name: Verify SmartFarm

on:
  pull_request:
    branches: [dev, main]
  push:
    branches: [dev]
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: smartfarm-verify-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  dotnet:
    name: API, MVC, Worker and schema tests
    runs-on: ubuntu-latest
    timeout-minutes: 25
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-dotnet@v4
        with:
          dotnet-version: 10.0.x
      - name: Restore
        run: dotnet restore SmartFarm.slnx
      - name: Build
        run: dotnet build SmartFarm.slnx --configuration Release --no-restore
      - name: Test
        run: dotnet test SmartFarm.slnx --configuration Release --no-build --no-restore
      # SQL-backed workflow tests require a separately initialized disposable
      # database and are intentionally not run against developer/production data.

  android:
    name: Android build and Retrofit/Room unit tests
    runs-on: ubuntu-latest
    timeout-minutes: 35
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: gradle
          cache-dependency-path: mobile/SmartFarm.Android/**/*.gradle.kts
      # ubuntu-latest already provides Android command-line tools. Installing
      # obsolete SDK "tools" through setup-android fails on recent runners.
      - name: Install Android SDK 36
        run: |
          test -n "${ANDROID_HOME:-}"
          SDKMANAGER="$(find "$ANDROID_HOME/cmdline-tools" -path '*/bin/sdkmanager' -print -quit)"
          test -n "$SDKMANAGER"
          "$SDKMANAGER" "platforms;android-36" "build-tools;36.0.0"
      - name: Build Android, compile device tests and run unit tests
        working-directory: mobile/SmartFarm.Android
        run: bash ./gradlew --no-daemon :app:assembleDebug :app:assembleAndroidTest :app:testDebugUnitTest
      # Instrumentation tests are compiled in CI, but need an emulator to execute.
````

## `scripts/verify-phase-a.sh`

````text
#!/usr/bin/env bash
# Run from any working directory: bash scripts/verify-phase-a.sh [--sql] [--device] [--skip-android]
# --sql requires a disposable, initialized SMARTFARM_TEST_CONNECTION.
# --device runs connected instrumentation tests on an already authorized Android emulator/device.
set -euo pipefail
cd "$(dirname "$0")/.."

run_sql=false
run_device=false
skip_android=false
for arg in "$@"; do
    case "$arg" in
        --sql) run_sql=true ;;
        --device) run_device=true ;;
        --skip-android) skip_android=true ;;
        -h|--help)
            echo "Usage: bash scripts/verify-phase-a.sh [--sql] [--device] [--skip-android]"
            echo "By default SQL integration and connected Android tests are NOT run."
            exit 0 ;;
        *) echo "Unknown argument: $arg" >&2; exit 2 ;;
    esac
done
if "$run_device" && "$skip_android"; then
    echo "--device and --skip-android cannot be combined." >&2; exit 2
fi
command -v dotnet >/dev/null || { echo "Missing .NET SDK." >&2; exit 2; }
command -v python3 >/dev/null || { echo "Missing Python 3." >&2; exit 2; }

echo "== SmartFarm Phase A: compilation and regression tests =="
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx -c Release --no-restore
dotnet test SmartFarm.slnx -c Release --no-build
python3 -m unittest discover -s database/verification -v
python3 -m unittest discover -s scripts/tests -v

if "$run_sql"; then
    if [[ -z "${SMARTFARM_TEST_CONNECTION:-}" ]]; then
        echo "SQL integration NOT run: set SMARTFARM_TEST_CONNECTION to a disposable initialized SQL Server database." >&2
        exit 2
    fi
    echo "== Running SQL-backed API farm-to-harvest regression =="
    # The test enforces disposable database name prefixes before making any changes.
    SMARTFARM_REQUIRE_SQL_TESTS=1 dotnet test tests/SmartFarm.Api.Tests/SmartFarm.Api.Tests.csproj \
        -c Release --no-build --filter 'FullyQualifiedName~BusinessWorkflowSqlTests'
else
    echo "NOT VERIFIED: SQL-backed end-to-end integration. Rerun with --sql and a disposable test database."
fi

if "$skip_android"; then
    echo "NOT VERIFIED: Android compilation and tests (--skip-android)."
else
    command -v java >/dev/null || { echo "Missing Java/Android SDK JDK." >&2; exit 2; }
    echo "== Android compilation, unit tests and instrumented-test compilation =="
    (cd mobile/SmartFarm.Android && bash ./gradlew --no-daemon \
        :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin)
    if "$run_device"; then
        command -v adb >/dev/null || { echo "adb is required for --device." >&2; exit 2; }
        if ! adb devices | awk 'NR>1 && $2=="device" { found=1 } END { exit !found }'; then
            echo "No authorized Android emulator/device connected. Device tests NOT run." >&2
            exit 2
        fi
        echo "== Running real connected Android instrumentation tests =="
        (cd mobile/SmartFarm.Android && bash ./gradlew --no-daemon :app:connectedDebugAndroidTest)
    else
        echo "NOT VERIFIED: Android on-device instrumentation execution. Rerun with --device."
    fi
fi

echo "PASS: requested Phase A checks completed."
echo "Reminder: SQL restoration, private-photo recovery, and interactive browser acceptance require separate real-environment verification."
````

## `scripts/verify-phase6.sh`

````text
#!/usr/bin/env bash
# Strict, opt-in local end-to-end verification. Never points SQL writes at SmartFarmDB.
# Example:
#   SMARTFARM_API_URL=http://localhost:5081 SMARTFARM_WEB_URL=http://localhost:5080 \
#   SMARTFARM_TEST_CONNECTION='Server=...;Database=SmartFarmEnrollmentTest_verify;...;' \
#   bash scripts/verify-phase6.sh
# The disposable test database must already contain the current SmartFarm schema.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { printf 'ERROR: %s\n' "$1" >&2; exit 1; }
command -v curl >/dev/null || fail "curl is required."
command -v dotnet >/dev/null || fail "Install the .NET 10 SDK."
: "${SMARTFARM_API_URL:?Set SMARTFARM_API_URL to the running development API.}"
: "${SMARTFARM_WEB_URL:?Set SMARTFARM_WEB_URL to the running development MVC website.}"
: "${SMARTFARM_TEST_CONNECTION:?Set SMARTFARM_TEST_CONNECTION to a disposable SQL Server database.}"

# Both SQL suites enforce the same catalog-prefix allowlist in C#.
# The prefix check here provides fast feedback before any tests run.
case "${SMARTFARM_TEST_CONNECTION}" in
  *SmartFarmEnrollmentTest_*|*SmartFarmEnrollmentBackend_*) ;;
  *) fail "Use a disposable SmartFarmEnrollmentTest_* or SmartFarmEnrollmentBackend_* database, never SmartFarmDB." ;;
esac
export SMARTFARM_ENROLLMENT_TEST_CONNECTION="${SMARTFARM_ENROLLMENT_TEST_CONNECTION:-$SMARTFARM_TEST_CONNECTION}"
case "${SMARTFARM_ENROLLMENT_TEST_CONNECTION}" in
  *SmartFarmEnrollmentTest_*|*SmartFarmEnrollmentBackend_*) ;;
  *) fail "Enrollment tests also require a disposable test database." ;;
esac

check_200() {
  local label="$1" url="$2" code
  code="$(curl --silent --show-error --max-time 15 --output /dev/null --write-out '%{http_code}' "$url")" ||
    fail "$label is unreachable: $url"
  [[ "$code" == 200 ]] || fail "$label returned HTTP $code: $url"
  printf 'PASS: %s (HTTP 200)\n' "$label"
}

check_200 "API status" "${SMARTFARM_API_URL%/}/api/status"
check_200 "API and SQL tables" "${SMARTFARM_API_URL%/}/health/database"
check_200 "MVC home page" "${SMARTFARM_WEB_URL%/}/"
check_200 "MVC and SQL tables" "${SMARTFARM_WEB_URL%/}/health/database"

printf '\n[1/2] .NET API, MVC, SQL workflow and worker tests\n'
dotnet test SmartFarm.slnx --configuration Release
printf '\n[2/2] Android build and Retrofit/Room unit tests\n'
(cd mobile/SmartFarm.Android &&
  bash ./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest)

if [[ "${RUN_ANDROID_DEVICE_TESTS:-0}" == 1 ]]; then
  printf '\n[Device] Room, image and UI instrumentation tests\n'
  (cd mobile/SmartFarm.Android &&
    bash ./gradlew --no-daemon :app:connectedDebugAndroidTest)
else
  printf '\nAndroid emulator tests were not run (set RUN_ANDROID_DEVICE_TESTS=1).\n'
fi

printf '\nPASS: Live endpoints, SQL-backed workflows, backend tests and Android build checks.\n'
printf 'Verify live Hangfire execution in its existing dashboard/logs; these tests check worker scheduling and rules, not a running worker process.\n'
````
