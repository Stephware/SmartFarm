using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using SmartFarm.Web.Authentication;

namespace SmartFarm.Web.Authentication;

public static class SuperAdminSecurity
{
    public const string Role = "SuperAdmin";
    public const string Policy = "SuperAdminMfa";

    public static bool HasVerifiedMfa(ClaimsPrincipal principal) =>
        principal.Identity?.IsAuthenticated == true &&
        principal.IsInRole(Role) &&
        principal.HasClaim("amr", "mfa");

    public static IServiceCollection AddSuperAdminSecurity(this IServiceCollection services)
    {
        services.AddAuthorization(options =>
            options.AddPolicy(Policy, new AuthorizationPolicyBuilder(
                    IdentityConstants.ApplicationScheme)
                .RequireAssertion(context => HasVerifiedMfa(context.User))
                .Build()));
        return services;
    }
}
