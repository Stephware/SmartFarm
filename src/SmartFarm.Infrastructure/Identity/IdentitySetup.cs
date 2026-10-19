using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using SmartFarm.Infrastructure.Persistence;

namespace SmartFarm.Infrastructure.Identity;

public static class IdentitySetup
{
    // The website uses cookies. Keep bearer support here temporarily until
    // Android is switched to SmartFarm.Api.
    public static IServiceCollection AddSmartFarmWebIdentity(
        this IServiceCollection services, IWebHostEnvironment environment)
    {
        services.AddIdentity<SmartFarmUser, SmartFarmRole>(ConfigureIdentity)
            .AddEntityFrameworkStores<SmartFarmDbContext>()
            .AddDefaultTokenProviders();

        services.AddAuthentication().AddBearerToken(IdentityConstants.BearerScheme, ConfigureBearer);

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
                await SecurityStampValidator.ValidatePrincipalAsync(context);
                if (context.Principal?.Identity?.IsAuthenticated != true) return;

                var users = context.HttpContext.RequestServices
                    .GetRequiredService<UserManager<SmartFarmUser>>();
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

    // The API accepts bearer tokens only: no MVC cookie scheme or login redirects.
    public static IServiceCollection AddSmartFarmApiIdentity(this IServiceCollection services)
    {
        services.AddIdentityCore<SmartFarmUser>(ConfigureIdentity)
            .AddRoles<SmartFarmRole>()
            .AddEntityFrameworkStores<SmartFarmDbContext>()
            .AddSignInManager()
            .AddDefaultTokenProviders();

        services.AddHttpContextAccessor();
        services.AddAuthentication(IdentityConstants.BearerScheme)
            .AddBearerToken(IdentityConstants.BearerScheme, ConfigureBearer);
        services.AddAuthorization();
        return services;
    }

    private static void ConfigureIdentity(IdentityOptions options)
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
        options.SignIn.RequireConfirmedEmail = false;
    }

    private static void ConfigureBearer(Microsoft.AspNetCore.Authentication.BearerToken.BearerTokenOptions options)
    {
        // ASP.NET Core Identity tokens, not JWTs.
        options.BearerTokenExpiration = TimeSpan.FromMinutes(30);
        options.RefreshTokenExpiration = TimeSpan.FromDays(7);
    }
}
