using Microsoft.AspNetCore.Identity;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using SmartFarm.Infrastructure.Email;
namespace SmartFarm.Infrastructure.Identity;
public static class AccountRecoveryRegistration
{
    public static IServiceCollection AddAccountRecovery(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddOptions<AccountRecoveryOptions>().Bind(configuration.GetSection("AccountRecovery"));
        services.AddOptions<EmailOptions>().Bind(configuration.GetSection("Email")).Validate(o => o.Valid(), "Invalid security email configuration.").ValidateOnStart();
        services.Configure<PasswordResetTokenOptions>(_ => { });
        services.Configure<IdentityOptions>(o => o.Tokens.PasswordResetTokenProvider = "AniGo.PasswordReset");
        services.Configure<SecurityStampValidatorOptions>(o => o.ValidationInterval = TimeSpan.Zero);
        services.AddSingleton(TimeProvider.System);
        services.AddSingleton<RecoveryThrottle>();
        services.AddSingleton<RecoveryEmailQueue>();
        services.AddHostedService(sp => sp.GetRequiredService<RecoveryEmailQueue>());
        services.AddSingleton<CaptureEmailSender>();
        services.AddTransient<SmtpSecurityEmailSender>();
        services.AddSingleton<ISecurityEmailSender>(sp => sp.GetRequiredService<Microsoft.Extensions.Options.IOptions<EmailOptions>>().Value.Mode switch
        {
            "Disabled" => new DisabledEmailSender(),
            "Capture" when sp.GetRequiredService<IHostEnvironment>().IsDevelopment() => sp.GetRequiredService<CaptureEmailSender>(),
            "Smtp" => sp.GetRequiredService<SmtpSecurityEmailSender>(),
            _ => throw new InvalidOperationException("Capture email is restricted to Development.")
        });
        services.AddScoped<AccountRecoveryService>();
        return services;
    }
}
