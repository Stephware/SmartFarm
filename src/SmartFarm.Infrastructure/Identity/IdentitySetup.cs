using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Identity;

public static class IdentitySetup
{
    public static IServiceCollection AddSmartFarmIdentity(
        this IServiceCollection services, IWebHostEnvironment environment)
    {
        services.AddIdentity<SmartFarmUser, SmartFarmRole>(options =>
            {
                options.User.RequireUniqueEmail = true;
                options.Password.RequiredLength = 10;
                options.Password.RequireDigit = true;
                options.Password.RequireLowercase = true;
                options.Password.RequireUppercase = true;
                options.Password.RequireNonAlphanumeric = false;
                options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(10);
                options.Lockout.MaxFailedAccessAttempts = 5;
                options.Lockout.AllowedForNewUsers = true;
                // No mail delivery/confirmation flow yet: do not pretend it is active.
                options.SignIn.RequireConfirmedEmail = false;
            })
            .AddEntityFrameworkStores<SmartFarmDbContext>()
            .AddDefaultTokenProviders();

        // Identity registers application cookies; add a separate token scheme for Android.
        // These tokens are ASP.NET Core Identity bearer tokens, not JWTs.
        services.AddAuthentication()
            .AddBearerToken(IdentityConstants.BearerScheme, options =>
            {
                options.BearerTokenExpiration = TimeSpan.FromMinutes(30);
                options.RefreshTokenExpiration = TimeSpan.FromDays(7);
            });
        services.ConfigureApplicationCookie(options =>
        {
            options.LoginPath = "/Account/Login";
            options.AccessDeniedPath = "/Account/AccessDenied";
            options.Cookie.Name = "SmartFarm.Auth";
            options.Cookie.HttpOnly = true;
            options.Cookie.SameSite = SameSiteMode.Lax;
            options.Cookie.SecurePolicy = environment.IsDevelopment()
                ? CookieSecurePolicy.SameAsRequest : CookieSecurePolicy.Always;
            options.SlidingExpiration = true;
            options.ExpireTimeSpan = TimeSpan.FromHours(8);
            options.Events.OnValidatePrincipal = async context =>
            {
                // Preserve the framework's security-stamp validation.
                await SecurityStampValidator.ValidatePrincipalAsync(context);
                if (context.Principal?.Identity?.IsAuthenticated != true) return;

                var users = context.HttpContext.RequestServices.GetRequiredService<UserManager<SmartFarmUser>>();
                var user = await users.GetUserAsync(context.Principal);
                if (user is null || user.DisabledAtUtc is not null)
                {
                    context.RejectPrincipal();
                    await context.HttpContext.SignOutAsync(IdentityConstants.ApplicationScheme);
                }
            };
        });
        services.AddAuthorization();
        return services;
    }
}
