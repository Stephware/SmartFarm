using System.Net;
using System.Text;

namespace SmartFarm.Infrastructure.Email;

public sealed record EmailDetail(string Label, string? Value);
public sealed record EmailAction(string Label, string Url);
public sealed record EmailContent(string Heading, string Preview, string? RecipientName,
    IReadOnlyList<string> Paragraphs, IReadOnlyList<EmailDetail>? Details = null,
    EmailAction? Action = null, string? Secondary = null, string Language = "en", bool AllowLoopbackHttp = false);
public sealed record RenderedEmail(string Html, string Text, string PreviewText);

/// <summary>One email-safe layout. All content is text; callers cannot inject HTML fragments.</summary>
public static class AniGoEmailLayout
{
    public const string Website = "https://anigo.website";
    private static string Encode(string? text) => WebUtility.HtmlEncode(text ?? "");

    public static RenderedEmail Render(EmailContent content)
    {
        var filipino = content.Language == "fil";
        var name = string.IsNullOrWhiteSpace(content.RecipientName) ? (filipino ? "kaibigan" : "there") : content.RecipientName.Trim();
        var greeting = filipino ? $"Kumusta {name}," : $"Hello {name},";
        if (content.Action is { } action && (!Uri.TryCreate(action.Url, UriKind.Absolute, out var uri)
            || !(uri.Scheme == "https" || content.AllowLoopbackHttp && uri.Scheme == "http" && uri.IsLoopback) || uri.UserInfo.Length != 0 || action.Url.Any(char.IsControl)))
            throw new ArgumentException("Email actions require an absolute HTTPS URL without credentials.");
        var text = new StringBuilder().AppendLine(content.Heading).AppendLine().AppendLine(greeting).AppendLine();
        var paragraphs = new StringBuilder();
        foreach (var paragraph in content.Paragraphs.Where(p => !string.IsNullOrWhiteSpace(p)))
        {
            paragraphs.Append($"<p style=\"margin:0 0 18px;line-height:1.65;overflow-wrap:break-word;word-wrap:break-word\">{Encode(paragraph).Replace("\n", "<br>")}</p>");
            text.AppendLine(paragraph).AppendLine();
        }
        var details = new StringBuilder();
        foreach (var row in content.Details ?? [])
        {
            var value = string.IsNullOrWhiteSpace(row.Value) ? (filipino ? "Hindi tinukoy" : "Not provided") : row.Value;
            details.Append($"<tr><th scope=\"row\" align=\"left\" valign=\"top\" width=\"35%\" style=\"padding:14px 16px;border-bottom:1px solid #e1e7de;font-size:14px;font-weight:400;color:#535f55;word-wrap:break-word\">{Encode(row.Label)}</th><td valign=\"top\" style=\"padding:14px 16px;border-bottom:1px solid #e1e7de;font-size:14px;font-weight:600;overflow-wrap:anywhere;word-break:break-word\">{Encode(value)}</td></tr>");
            text.AppendLine($"{row.Label}: {value}");
        }
        var panel = details.Length == 0 ? "" : $"<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:12px 0 28px;background:#f8f6f1;border-collapse:collapse;table-layout:fixed;color:#26352d\">{details}</table>";
        var button = "";
        if (content.Action is { } primary)
        {
            // Table cell background survives clients that ignore anchor backgrounds/radii.
            button = $"<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:8px 0 26px;max-width:100%\"><tr><td bgcolor=\"#134D30\" style=\"border-radius:8px;text-align:center;mso-padding-alt:16px 24px\"><a class=\"email-action\" href=\"{Encode(primary.Url)}\" style=\"display:inline-block;padding:16px 24px;font-size:16px;font-weight:700;line-height:1.4;color:#ffffff;text-decoration:none;overflow-wrap:anywhere\">{Encode(primary.Label)}</a></td></tr></table>";
            text.AppendLine().AppendLine(primary.Label).AppendLine(primary.Url);
        }
        var secondary = string.IsNullOrWhiteSpace(content.Secondary) ? "" : $"<p style=\"margin:0;font-size:14px;line-height:1.65;color:#535f55\">{Encode(content.Secondary)}</p>";
        if (secondary.Length > 0) text.AppendLine().AppendLine(content.Secondary);
        var automated = filipino ? "Awtomatikong email ito mula sa AniGo. Huwag tumugon sa email na ito." : "This is an automated email from AniGo. Please do not reply.";
        text.AppendLine().AppendLine("AniGo — Smart Farm and Crop Monitoring Management System")
            .AppendLine(automated).AppendLine($"AniGo · {Website}");
        var html = $$$"""
<!doctype html>
<html lang="{{{(filipino ? "fil" : "en")}}}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light dark"><title>{{{Encode(content.Heading)}}}</title>
<style>@media only screen and (max-width:480px){.email-outer{padding:16px 8px!important}.email-pad{padding:26px 20px!important}.email-title{font-size:26px!important}}@media(prefers-color-scheme:dark){.email-card,.email-body{background:#ffffff!important;color:#26352d!important}.email-header{background:#134D30!important;color:#ffffff!important}}</style></head>
<body style="margin:0;padding:0;background:#f8f6f1;font-family:Arial,Helvetica,sans-serif;color:#26352d;-webkit-text-size-adjust:100%">
<div style="display:none;max-height:0;overflow:hidden;mso-hide:all;font-size:1px;line-height:1px;color:#f8f6f1">{{{Encode(content.Preview)}}}</div>
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" bgcolor="#f8f6f1"><tr><td class="email-outer" align="center" style="padding:36px 16px">
<!--[if mso]><table role="presentation" width="640" cellpadding="0" cellspacing="0"><tr><td><![endif]-->
<table class="email-card" role="presentation" width="100%" cellpadding="0" cellspacing="0" bgcolor="#ffffff" style="max-width:640px;background:#ffffff;border:1px solid #e1e7de;border-radius:16px;overflow:hidden">
<tr><td class="email-header email-pad" bgcolor="#134D30" style="padding:30px 36px;background:#134D30;border-bottom:4px solid #CBA14E">
<table role="presentation" cellpadding="0" cellspacing="0"><tr><td valign="middle" style="padding-right:14px"><img src="https://anigo.website/images/branding/anigo-email-mark.png" width="52" height="52" alt="AniGo farm mark" style="display:block;width:52px;height:52px;border:0;background:#ffffff;border-radius:8px"></td><td valign="middle" style="color:#ffffff"><p style="margin:0;font-size:28px;line-height:1.2;font-weight:700;color:#ffffff">AniGo</p><p style="margin:6px 0 0;font-size:13px;line-height:1.5;color:#ffffff">Smarter farming, made simpler.</p></td></tr></table>
</td></tr><tr><td class="email-body email-pad" style="padding:34px 36px 36px;background:#ffffff;color:#26352d;font-size:16px;line-height:1.65;word-wrap:break-word;overflow-wrap:anywhere">
<h1 class="email-title" style="margin:0 0 22px;font-size:30px;line-height:1.25;font-weight:700;color:#134D30">{{{Encode(content.Heading)}}}</h1>
<p style="margin:0 0 18px;line-height:1.65">{{{Encode(greeting)}}}</p>{{{paragraphs}}}{{{panel}}}{{{button}}}{{{secondary}}}
</td></tr><tr><td class="email-pad" style="padding:24px 36px;border-top:1px solid #e1e7de;background:#ffffff;color:#535f55;font-size:12px;line-height:1.7">
<p style="margin:0 0 8px;font-weight:700;color:#134D30">AniGo — Smart Farm and Crop Monitoring Management System</p>
<p style="margin:0 0 12px">{{{Encode(automated)}}}</p><p style="margin:0">© {{{DateTime.UtcNow.Year}}} AniGo · <a href="https://anigo.website" style="color:#134D30;text-decoration:underline">anigo.website</a></p>
</td></tr></table><!--[if mso]></td></tr></table><![endif]-->
</td></tr></table></body></html>
""";
        return new(html, text.ToString(), content.Preview);
    }
}
