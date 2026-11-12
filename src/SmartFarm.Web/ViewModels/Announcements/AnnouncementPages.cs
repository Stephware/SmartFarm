using System.ComponentModel.DataAnnotations;
using SmartFarm.Core.Entities;

namespace SmartFarm.Web.ViewModels.Announcements;

public static class AnnouncementRules
{
    public static readonly string[] Categories = ["General", "Maintenance", "Update"];
    public static bool IsVisible(PlatformAnnouncement item, DateTime nowUtc) =>
        item.State == "Published" && item.PublishedAtUtc is DateTime published &&
        published <= nowUtc && (item.ExpiresAtUtc is null || item.ExpiresAtUtc > nowUtc);

    public static bool ValidExpiry(DateTime? expiry, DateTime nowUtc) =>
        expiry is null || expiry.Value > nowUtc;

    public static bool ValidRowVersion(string? value, out byte[] bytes)
    {
        try { bytes = Convert.FromBase64String(value ?? ""); }
        catch (FormatException) { bytes = []; }
        return bytes.Length == 8;
    }
}

public sealed class AnnouncementInput
{
    [Required, StringLength(140, MinimumLength = 5)]
    public string Title { get; set; } = "";

    [Required, StringLength(2000, MinimumLength = 15)]
    public string Body { get; set; } = "";

    [Required, RegularExpression("General|Maintenance|Update")]
    public string Category { get; set; } = "General";

    // Datetime input is explicitly labelled UTC. Do not convert from server local time.
    public DateTime? ExpiresAtUtc { get; set; }
    public string? RowVersion { get; set; }
}

public sealed record AnnouncementRow(Guid Id, string Title, string Body,
    string Category, string State, DateTime CreatedAtUtc,
    DateTime? PublishedAtUtc, DateTime? ExpiresAtUtc, string RowVersion);

public sealed record AdminAnnouncementIndexPage(string Status, int Page,
    int Total, int DraftCount, int PublishedCount, IReadOnlyList<AnnouncementRow> Announcements)
{
    public const int PageSize = 20;
    public bool HasPrevious => Page > 1;
    public bool HasNext => Page * PageSize < Total;
}

public sealed record AnnouncementEditorPage(Guid? Id, AnnouncementInput Input)
{
    public bool IsEdit => Id.HasValue;
}

public sealed record PublicAnnouncementIndexPage(int Page, int Total,
    IReadOnlyList<AnnouncementRow> Announcements)
{
    public const int PageSize = 15;
    public bool HasPrevious => Page > 1;
    public bool HasNext => Page * PageSize < Total;
}

public static class AnnouncementMapper
{
    public static AnnouncementRow ToRow(PlatformAnnouncement announcement) =>
        new(announcement.Id, announcement.Title, announcement.Body,
            announcement.Category, announcement.State, announcement.CreatedAtUtc,
            announcement.PublishedAtUtc, announcement.ExpiresAtUtc,
            Convert.ToBase64String(announcement.RowVersion));

    public static AnnouncementInput ToInput(PlatformAnnouncement announcement) =>
        new()
        {
            Title = announcement.Title,
            Body = announcement.Body,
            Category = announcement.Category,
            ExpiresAtUtc = announcement.ExpiresAtUtc,
            RowVersion = Convert.ToBase64String(announcement.RowVersion)
        };
}
