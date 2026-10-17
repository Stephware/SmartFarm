using Microsoft.AspNetCore.Authentication;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Authentication.BearerToken;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Options;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Web.ViewModels.Auth;

namespace SmartFarm.Web.Api.Controllers;

[ApiController]
[Route("api/auth")]
public sealed class AuthController(
    UserManager<SmartFarmUser> users,
    SignInManager<SmartFarmUser> signIn,
    AccountRegistrationService registration,
    IOptionsMonitor<BearerTokenOptions> tokenOptions) : ControllerBase
{
    [AllowAnonymous, HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterInput input)
    {
        var result = await registration.RegisterAsync(input.FullName, input.Email, input.Password);
        if (!result.Succeeded)
            return BadRequest(new { errors = result.Errors.Select(error => error.Description) });

        return StatusCode(StatusCodes.Status201Created, new { message = "Account created. Please sign in." });
    }

    [AllowAnonymous, HttpPost("login")]
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

    [Authorize(AuthenticationSchemes = IdentityConstants.BearerScheme), HttpGet("me")]
    public async Task<IActionResult> Me()
    {
        var user = await users.GetUserAsync(User);
        if (user is null || user.DisabledAtUtc is not null) return Unauthorized();
        return Ok(new { id = user.Id, fullName = user.FullName, email = user.Email });
    }
}

