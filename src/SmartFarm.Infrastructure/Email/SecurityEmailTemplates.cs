namespace SmartFarm.Infrastructure.Email;

/// <summary>Only existing security events. New email triggers require a separate business decision.</summary>
public static class SecurityEmailTemplates
{
    public static SecurityEmail PasswordReset(string to, string? name, string language, string resetUrl, string localTime, bool development = false)
    {
        var fil = language == "fil";
        var title = fil ? "Palitan ang iyong password" : "Reset your password";
        var content = new EmailContent(title,
            fil ? "Hiniling ang pagpapalit ng iyong AniGo password." : "A password reset was requested for your AniGo account.", name,
            [fil ? "Nakatanggap kami ng kahilingang palitan ang password ng iyong AniGo account. Gamitin ang button sa ibaba upang pumili ng bagong password."
                : "We received a request to reset your AniGo account password. Use the button below to choose a new password."],
            [new(fil ? "Bisa ng link" : "Link validity", fil ? "Isang oras" : "One hour"),
             new(fil ? "Oras sa Pilipinas" : "Philippine time", localTime)],
            new(fil ? "Palitan ang password" : "Reset password", resetUrl),
            fil ? "Kung hindi ikaw ang humiling nito, huwag pansinin ang email. Hindi magbabago ang iyong password hangga't hindi ka pumipili ng bago."
                : "If you did not request this, ignore this email. Your password will stay the same unless you choose a new one.", language, development);
        return Message(to, "password.reset", title, content);
    }

    public static SecurityEmail PasswordChanged(string to, string? name, string language, string origin, string localTime, bool development = false)
    {
        var fil = language == "fil";
        var title = fil ? "Napalitan ang iyong password" : "Your password was changed";
        return Message(to, "password.changed", title, new EmailContent(title,
            fil ? "Abiso tungkol sa seguridad ng iyong AniGo account." : "A security update for your AniGo account.", name,
            [fil ? "Napalitan ang iyong AniGo password. Abiso ito upang malaman mong naitala ang pagbabago."
                : "Your AniGo password was changed. This email confirms that the change was recorded."],
            [new(fil ? "Oras sa Pilipinas" : "Philippine time", localTime)],
            new(fil ? "Bawiin ang aking account" : "Recover my account", origin.TrimEnd('/') + "/Account/ForgotPassword"),
            fil ? "Kung ikaw ang nagpalit, wala ka nang kailangang gawin. Kung hindi ikaw ito, bawiin agad ang iyong account gamit ang button sa itaas."
                : "If you made this change, no further action is needed. If this was not you, recover your account immediately using the button above.", language, development));
    }

    private static SecurityEmail Message(string to, string kind, string title, EmailContent content)
    {
        var rendered = AniGoEmailLayout.Render(content);
        return new(to, kind, "AniGo — " + title, rendered.Text, rendered.Html, rendered.PreviewText);
    }
}
