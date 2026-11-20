using System.Collections.Concurrent;
using System.Net;
using System.Net.Mail;
using System.Net.Mime;
using System.Text;
using Microsoft.Extensions.Options;

namespace SmartFarm.Infrastructure.Email;

public sealed record SecurityEmail(string To, string Kind, string Subject, string Body, string? HtmlBody = null, string? PreviewText = null);
public interface ISecurityEmailSender { Task<bool> SendAsync(SecurityEmail message, CancellationToken ct = default); }
public sealed class EmailOptions
{
    public string Mode { get; set; } = "Disabled";
    public string? Host { get; set; }
    public int Port { get; set; } = 587;
    public string? From { get; set; }
    public string? Username { get; set; }
    public string? Password { get; set; }
    public int TimeoutSeconds { get; set; } = 10;
    public bool Valid() => Mode is "Disabled" or "Capture" || Mode == "Smtp" &&
        !string.IsNullOrWhiteSpace(Host) && !Host.Any(char.IsWhiteSpace) && Port is > 0 and <= 65535 &&
        MailAddress.TryCreate(From, out var address) && address.Address == From && TimeoutSeconds is >= 1 and <= 30 &&
        (string.IsNullOrEmpty(Username) == string.IsNullOrEmpty(Password));
}
public sealed class DisabledEmailSender : ISecurityEmailSender
{
    public Task<bool> SendAsync(SecurityEmail message, CancellationToken ct = default) => Task.FromResult(false);
}
// Explicit Development/test-only fake. No public endpoint or logging exposes reset links.
public sealed class CaptureEmailSender : ISecurityEmailSender
{
    private readonly ConcurrentQueue<SecurityEmail> messages = new();
    public IReadOnlyList<SecurityEmail> Messages => messages.ToArray();
    public Task<bool> SendAsync(SecurityEmail message, CancellationToken ct = default)
    {
        ct.ThrowIfCancellationRequested(); messages.Enqueue(message);
        while (messages.Count > 100) messages.TryDequeue(out _);
        return Task.FromResult(true);
    }
}
public sealed class SmtpSecurityEmailSender(IOptions<EmailOptions> options) : ISecurityEmailSender
{
    public async Task<bool> SendAsync(SecurityEmail message, CancellationToken ct = default)
    {
        var o = options.Value;
        using var client = new SmtpClient(o.Host!, o.Port) { EnableSsl = true, UseDefaultCredentials = false };
        if (!string.IsNullOrEmpty(o.Username)) client.Credentials = new NetworkCredential(o.Username, o.Password);
        using var mail = SecurityEmailMime.Create(message, o.From!);
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(ct);
        timeout.CancelAfter(TimeSpan.FromSeconds(o.TimeoutSeconds));
        await client.SendMailAsync(mail, timeout.Token);
        return true;
    }
}

/// <summary>Builds multipart/alternative without sending; safe to inspect in tests.</summary>
public static class SecurityEmailMime
{
    public static MailMessage Create(SecurityEmail message, string from)
    {
        var mail = new MailMessage { From = new MailAddress(from, "AniGo", Encoding.UTF8),
            Subject = message.Subject, SubjectEncoding = Encoding.UTF8, BodyEncoding = Encoding.UTF8 };
        try
        {
            mail.To.Add(message.To);
            var html = message.HtmlBody ?? AniGoEmailLayout.Render(new EmailContent(message.Subject,
                message.PreviewText ?? message.Subject, null, [message.Body])).Html;
            mail.AlternateViews.Add(AlternateView.CreateAlternateViewFromString(message.Body, Encoding.UTF8, MediaTypeNames.Text.Plain));
            mail.AlternateViews.Add(AlternateView.CreateAlternateViewFromString(html, Encoding.UTF8, MediaTypeNames.Text.Html));
            return mail;
        }
        catch { mail.Dispose(); throw; }
    }
}
