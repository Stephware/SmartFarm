using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.DataProtection;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.WebUtilities;
using Microsoft.Extensions.Caching.Memory;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using SmartFarm.Core.Contracts.Accounts;
using SmartFarm.Core.Services;
using SmartFarm.Infrastructure.Email;

namespace SmartFarm.Infrastructure.Identity;

public sealed class AccountRecoveryOptions
{
    public string? ApplicationOrigin { get; set; }
    public bool ValidOrigin(bool development) => Uri.TryCreate(ApplicationOrigin, UriKind.Absolute, out var uri) &&
        (uri.Scheme == "https" || development && uri.Scheme == "http" && uri.IsLoopback) &&
        uri.UserInfo == "" && uri.AbsolutePath == "/" && uri.Query == "" && uri.Fragment == "";
}
public sealed class PasswordResetTokenOptions : DataProtectionTokenProviderOptions
{
    public PasswordResetTokenOptions() { Name = "AniGo.PasswordReset"; TokenLifespan = TimeSpan.FromHours(1); }
}
public sealed class PasswordResetTokenProvider(IDataProtectionProvider protection, IOptions<PasswordResetTokenOptions> options,
    ILogger<DataProtectorTokenProvider<SmartFarmUser>> logger)
    : DataProtectorTokenProvider<SmartFarmUser>(protection, Microsoft.Extensions.Options.Options.Create<DataProtectionTokenProviderOptions>(options.Value), logger);

public sealed class RecoveryThrottle(TimeProvider clock)
{
    private readonly object gate = new();
    private readonly Dictionary<string, DateTimeOffset> sent = new();
    private static string Key(string email) => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(email.ToUpperInvariant())));
    public void Release(string email) { lock (gate) sent.Remove(Key(email)); }
    public bool TryAcquire(string email)
    {
        var key = Key(email);
        lock (gate)
        {
            var now = clock.GetUtcNow();
            foreach (var stale in sent.Where(p => p.Value <= now).Select(p => p.Key).ToList()) sent.Remove(stale);
            if (sent.ContainsKey(key) || sent.Count >= 10000) return false;
            sent[key] = now.AddMinutes(10); return true;
        }
    }
}

public sealed class AccountRecoveryService(UserManager<SmartFarmUser> users, UserPreferencesService preferences,
    ISecurityEmailSender sender, IOptions<AccountRecoveryOptions> options, IHostEnvironment environment,
    RecoveryThrottle throttle, RecoveryEmailQueue queue, TimeProvider clock, ILogger<AccountRecoveryService> logger)
{
    public Task RequestAsync(string email, CancellationToken ct = default)
    {
        ct.ThrowIfCancellationRequested();
        email = email.Trim();
        if (throttle.TryAcquire(email) && !queue.TryRequest(email))
        {
            throttle.Release(email);
            logger.LogWarning("Security email queue is full.");
        }
        return Task.CompletedTask;
    }

    internal async Task SendRequestedAsync(string email, CancellationToken ct)
    {
        if (!options.Value.ValidOrigin(environment.IsDevelopment()))
        { logger.LogWarning("Password recovery is unavailable: configure a valid ApplicationOrigin."); return; }
        var user = await users.FindByEmailAsync(email);
        if (user is null || user.DisabledAtUtc is not null || !await users.HasPasswordAsync(user)) return;
        var language = await preferences.LanguageAsync(user.Id, ct);
        var token = WebEncoders.Base64UrlEncode(Encoding.UTF8.GetBytes(await users.GeneratePasswordResetTokenAsync(user)));
        var link = options.Value.ApplicationOrigin!.TrimEnd('/') + "/Account/ResetPassword?email=" + Uri.EscapeDataString(user.Email!) + "&token=" + token;
        var body = language == "fil"
            ? $"Hiniling ang pagpapalit ng iyong password. Magagamit ang link sa loob ng isang oras. Kung hindi ikaw ang humiling nito, huwag pansinin ang email.\n{link}\nOras sa Pilipinas: {LocalTime()}"
            : $"A password reset was requested. This link expires in one hour. If you did not request it, ignore this email.\n{link}\nPhilippine time: {LocalTime()}";
        await DeliverAsync(new(user.Email!, "password.reset", language == "fil" ? "AniGo: Palitan ang password" : "AniGo: Reset your password", body), ct);
    }

    public async Task<bool> ResetAsync(ResetPasswordInput input, CancellationToken ct = default)
    {
        var user = await users.FindByEmailAsync(input.Email.Trim());
        if (user is null || user.DisabledAtUtc is not null) return false;
        string token;
        try { token = new UTF8Encoding(false, true).GetString(WebEncoders.Base64UrlDecode(input.Token)); }
        catch (Exception e) when (e is FormatException or DecoderFallbackException) { return false; }
        var result = await users.ResetPasswordAsync(user, token, input.Password);
        if (!result.Succeeded) return false;
        // ResetPasswordAsync rotates Identity's security stamp; cookies/access/refresh validate it.
        var language = await preferences.LanguageAsync(user.Id, ct);
        if (!queue.TryConfirmation(new(user.Email!, "password.changed", language == "fil" ? "AniGo: Napalitan ang password" : "AniGo: Password changed",
            language == "fil" ? $"Napalitan ang iyong password. Kung hindi ikaw ito, bawiin agad ang iyong account. Oras sa Pilipinas: {LocalTime()}"
                : $"Your password was changed. If this was not you, recover your account immediately. Philippine time: {LocalTime()}")))
            logger.LogWarning("Security confirmation email queue is full.");
        return true;
    }
    private string LocalTime() => FarmTime.Local(clock.GetUtcNow().UtcDateTime, FarmTime.DefaultZone).ToString("yyyy-MM-dd HH:mm zzz", CultureInfo.InvariantCulture);
    internal async Task DeliverAsync(SecurityEmail message, CancellationToken ct)
    {
        try
        {
            if (!await sender.SendAsync(message, ct)) logger.LogWarning("Security email delivery unavailable ({Kind}).", message.Kind);
        }
        catch (OperationCanceledException) when (ct.IsCancellationRequested) { throw; }
        catch (Exception error)
        { logger.LogWarning("Security email delivery failed ({Kind}, {FailureType}).", message.Kind, error.GetType().Name); }
    }
}
