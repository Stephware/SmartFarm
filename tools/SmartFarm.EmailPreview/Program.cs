using SmartFarm.Infrastructure.Email;

// No configuration, DI container, SMTP connection, database or live tokens are loaded.
if (Environment.GetEnvironmentVariable("DOTNET_ENVIRONMENT") != "Development")
    throw new InvalidOperationException("Email previews require DOTNET_ENVIRONMENT=Development.");
if (args.Length != 1 || !Path.IsPathFullyQualified(args[0]) || Directory.Exists(args[0]) || File.Exists(args[0]))
    throw new ArgumentException("Provide a new absolute output directory.");
var output = args[0]; Directory.CreateDirectory(output);
var logo = Path.Combine(Directory.GetCurrentDirectory(), "src/SmartFarm.Web/wwwroot/images/branding/anigo-email-mark.png");
var localLogo = File.Exists(logo);
if (localLogo) File.Copy(logo, Path.Combine(output, "anigo-email-mark.png"));
foreach (var language in new[] { "en", "fil" })
{
    Write("password-reset-" + language, SecurityEmailTemplates.PasswordReset("fiction@example.test", "Maya Santos", language,
        "https://anigo.website/Account/ResetPassword?email=fiction%40example.test&token=preview-only-not-a-live-token", "2026-11-20 18:00 +08:00"));
    Write("password-changed-" + language, SecurityEmailTemplates.PasswordChanged("fiction@example.test", "Maya Santos", language,
        "https://anigo.website", "2026-11-20 18:00 +08:00"));
}
var longContent = AniGoEmailLayout.Render(new EmailContent("A fictional account security update",
    "Fictional preview for layout checks only.", string.Join(" ", Enumerable.Repeat("Maya Santos", 12)),
    [string.Join(" ", Enumerable.Repeat("This is fictional preview content, not an actual farm notification.", 18))],
    [new("Farm (fictional)", "Luntian Demonstration Farm"), new("Optional detail", null), new("Long reference", new string('A', 240))],
    new("Review fictional account", "https://anigo.website/Account/Login"), "Preview only — no email was sent."));
Write("long-and-missing", new("fiction@example.test", "preview", "Fictional preview", longContent.Text, longContent.Html));
var minimal = AniGoEmailLayout.Render(new EmailContent("A fictional account update", "Preview only.", null, ["A short message with no details or action."]));
Write("minimal", new("fiction@example.test", "preview", "Fictional preview", minimal.Text, minimal.Html));
File.WriteAllText(Path.Combine(output, "index.html"), "<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>AniGo email previews</title><body style=\"font:16px/1.6 Arial,sans-serif;max-width:700px;margin:40px auto;padding:0 20px;background:#f8f6f1;color:#26352d\"><h1>AniGo email previews</h1><p>Fictional development data. No emails were sent. Sample reset tokens are inert; use your real account's recovery flow instead.</p><ul>" + string.Join("", new[] { "password-reset-en", "password-reset-fil", "password-changed-en", "password-changed-fil", "long-and-missing", "minimal" }.Select(name => $"<li><a href=\"{name}.html\">{name}</a> · <a href=\"{name}.txt\">Plain text</a></li>")) + "</ul></body></html>");
Console.WriteLine("Created six fictional HTML/plain-text previews and index. No email sent.");
void Write(string name, SecurityEmail message)
{
    File.WriteAllText(Path.Combine(output, name + ".html"), localLogo ? message.HtmlBody!.Replace("https://anigo.website/images/branding/anigo-email-mark.png", "./anigo-email-mark.png") : message.HtmlBody);
    File.WriteAllText(Path.Combine(output, name + ".txt"), message.Body);
}
