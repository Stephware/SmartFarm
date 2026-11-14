using SmartFarm.Core.Entities;

namespace SmartFarm.Web.ViewModels.Announcements;

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

public static class AnnouncementMapper
{
    public static AnnouncementRow ToRow(PlatformAnnouncement announcement) =>
        new(announcement.Id, announcement.Title, announcement.Body,
            announcement.Category, announcement.State, announcement.CreatedAtUtc,
            announcement.PublishedAtUtc, announcement.ExpiresAtUtc,
            Convert.ToBase64String(announcement.RowVersion));

}
