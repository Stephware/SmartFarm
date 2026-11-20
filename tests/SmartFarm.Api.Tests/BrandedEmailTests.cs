using SmartFarm.Infrastructure.Email;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class BrandedEmailTests
{
    [Fact]
    public void LayoutEncodesAllDynamicFieldsAndKeepsPlainText()
    {
        var result = AniGoEmailLayout.Render(new EmailContent("Title <script>", "Preview <tag>", "<img onerror=x>",
            ["Message <unsafe> & readable"], [new("Label <x>", "Value <svg>")],
            new("Open <account>", "https://anigo.website/Account?a=1&b=2"), "Secondary <x>"));
        Assert.DoesNotContain("<script>", result.Html);
        Assert.DoesNotContain("<img onerror", result.Html);
        Assert.Contains("&lt;unsafe&gt; &amp; readable", result.Html);
        Assert.Contains("a=1&amp;b=2", result.Html);
        Assert.Contains("Value <svg>", result.Text);
        Assert.Contains("role=\"presentation\"", result.Html);
        Assert.Contains("max-width:640px", result.Html);
    }

    [Theory]
    [InlineData("javascript:alert(1)")]
    [InlineData("//evil.example/path")]
    [InlineData("http://anigo.website/path")]
    [InlineData("https://user:password@anigo.website/path")]
    public void RejectsUnsafeActionUrls(string url) => Assert.Throws<ArgumentException>(() =>
        AniGoEmailLayout.Render(new EmailContent("Title", "Preview", null, ["Body"], Action: new("Open", url))));

    [Fact]
    public void MissingOptionalValuesProduceReadableFallbackWithoutEmptySections()
    {
        var result = AniGoEmailLayout.Render(new EmailContent("Title", "Preview", "  ", ["Body"]));
        Assert.Contains("Hello there,", result.Html);
        Assert.DoesNotContain("<th", result.Html);
        Assert.DoesNotContain("class=\"email-action\"", result.Html);
        Assert.Contains("Body", result.Text);
    }

    [Theory]
    [InlineData("en", "Reset your password", "Hello")]
    [InlineData("fil", "Palitan ang iyong password", "Kumusta")]
    public void ResetPreservesSinglePlainTextActionAndDoesNotLeakTokenInPreview(string language, string heading, string greeting)
    {
        const string link = "https://anigo.website/Account/ResetPassword?email=fiction@example.test&token=fictional-token";
        var mail = SecurityEmailTemplates.PasswordReset("fiction@example.test", "Farmer <Name>", language, link, "2026-11-20 18:00 +08:00");
        Assert.Equal("password.reset", mail.Kind);
        Assert.Contains(heading, mail.HtmlBody);
        Assert.Contains(greeting, mail.Body);
        Assert.Equal(link, mail.Body.Split('\n').Single(line => line.StartsWith("https://")));
        Assert.DoesNotContain("fictional-token", mail.PreviewText);
        Assert.Contains("Farmer &lt;Name&gt;", mail.HtmlBody);
    }

    [Fact]
    public void HttpIsAllowedOnlyForExplicitDevelopmentLoopback()
    {
        const string url = "http://localhost:5080/Account/ResetPassword?token=fictional";
        Assert.Throws<ArgumentException>(() => SecurityEmailTemplates.PasswordReset("fiction@example.test", null, "en", url, "time"));
        Assert.Contains(url, SecurityEmailTemplates.PasswordReset("fiction@example.test", null, "en", url, "time", development: true).Body);
        Assert.Throws<ArgumentException>(() => SecurityEmailTemplates.PasswordReset("fiction@example.test", null, "en", "http://evil.example/path", "time", development: true));
    }

    [Fact]
    public void PasswordChangeHasRecoveryActionAndMultipartAlternative()
    {
        var message = SecurityEmailTemplates.PasswordChanged("fiction@example.test", null, "en", "https://anigo.website", "2026-11-20 18:00 +08:00");
        using var mail = SecurityEmailMime.Create(message, "noreply@anigo.website");
        Assert.Equal("AniGo", mail.From!.DisplayName);
        Assert.Equal(2, mail.AlternateViews.Count);
        Assert.Equal("text/plain", mail.AlternateViews[0].ContentType.MediaType);
        Assert.Equal("text/html", mail.AlternateViews[1].ContentType.MediaType);
        Assert.Contains("/Account/ForgotPassword", message.HtmlBody);
        Assert.Contains("password was changed", message.Body);
    }
}
