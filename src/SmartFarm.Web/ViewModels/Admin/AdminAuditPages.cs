namespace SmartFarm.Web.ViewModels.Admin;

// Read-only audit representations. Never include passwords, tokens or farm data.
public sealed record AdminAuditRow(
    Guid Id, DateTime OccurredAtUtc, string ActorName, string? ActorEmail,
    string ActorSource, string Action, string TargetType, Guid? TargetId);

public sealed record AdminAuditListPage(
    string Search, string Action, string TargetType, Guid? TargetId,
    DateOnly? From, DateOnly? To, int Page, int Total,
    int RecentCount, IReadOnlyList<AdminAuditRow> Entries, string? Error = null)
{
    public const int PageSize = 25;
    public bool HasPrevious => Page > 1;
    public bool HasNext => Page * PageSize < Total;
}

public sealed record AdminAuditDetailPage(AdminAuditRow Entry, string? Reason);

public static class AdminAuditDisplay
{
    // Only administrative actions already written by the system. Unknown future
    // actions remain visible instead of disappearing from the audit explorer.
    public static readonly (string Key, string Label)[] Actions =
    [
        ("super-admin-granted", "Super Admin access granted"),
        ("admin-mfa-enabled", "Admin MFA enabled"),
        ("user-disabled", "Account disabled"),
        ("user-restored", "Account restored"),
        ("farm-archived", "Farm archived"),
        ("farm-restored", "Farm restored"),
        ("support-status-changed", "Support case updated"),
        ("support-replied", "Support reply sent"),
        ("support-internal-note", "Private support note"),
        ("announcement-created", "Announcement draft created"),
        ("announcement-edited", "Announcement draft edited"),
        ("announcement-published", "Announcement published"),
        ("announcement-archived", "Announcement archived")
    ];

    public static readonly string[] TargetTypes = ["user", "farm", "support-ticket", "announcement"];

    public static string ActionLabel(string key) =>
        Actions.FirstOrDefault(action => action.Key == key).Label ?? key.Replace('-', ' ');

    public static bool IsKnownAction(string? action) =>
        action is not null && Actions.Any(a => a.Key == action);

    public static string TargetLabel(string type) => type switch
    {
        "user" => "User account",
        "farm" => "Farm",
        "support-ticket" => "Support case",
        "announcement" => "Announcement",
        _ => type
    };
}
