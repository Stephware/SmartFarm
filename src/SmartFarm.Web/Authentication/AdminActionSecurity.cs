namespace SmartFarm.Web.Authentication;

// The password is checked only in memory by Identity and is never written to
// AuditLog, TempData, URLs, logs, or the database as plaintext.
public static class AdminActionSecurity
{
    public static bool ValidPasswordInput(string? password) =>
        password?.Length is >= 1 and <= 1024;
}
