using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Tasks;

namespace SmartFarm.Web.ViewModels.Monitoring;

public sealed record CropTimelineEntry(
    DateOnly FarmDate, TimeOnly? FarmTime, string Category,
    string Title, string? Detail = null, Guid? PhotoId = null);

public sealed record CropTimelinePage(CropCycleDetails Cycle,
    IReadOnlyList<CropTimelineEntry> Entries, int TotalCount, string TimeZoneId);

public static class CropTimelineBuilder
{
    public static CropTimelinePage Build(CropCycleDetails cycle, MonitoringDetails monitoring,
        HarvestOverview harvests, IReadOnlyList<FarmTaskDetails> tasks, string timeZoneId)
    {
        TimeZoneInfo zone;
        try { zone = TimeZoneInfo.FindSystemTimeZoneById(timeZoneId); }
        catch (Exception ex) when (ex is TimeZoneNotFoundException or InvalidTimeZoneException)
        { zone = TimeZoneInfo.Utc; }

        var events = new List<CropTimelineEntry>();
        void Date(DateOnly date, string category, string title, string? detail = null) =>
            events.Add(new(date, null, category, title, detail));

        void Instant(DateTime utc, string category, string title,
            string? detail = null, Guid? photoId = null)
        {
            var local = TimeZoneInfo.ConvertTimeFromUtc(
                DateTime.SpecifyKind(utc, DateTimeKind.Utc), zone);
            events.Add(new(DateOnly.FromDateTime(local), TimeOnly.FromDateTime(local),
                category, title, detail, photoId));
        }

        Date(cycle.PlannedPlantingDate, "Plan", "Planned planting");
        if (cycle.PlantedDate is { } planted) Date(planted, "Crop", "Planted");
        if (cycle.ExpectedHarvestDate is { } expected)
            Date(expected, "Plan", "Expected harvest");
        if (cycle.CompletedAtUtc is { } completed)
            Instant(completed, "Crop", cycle.StatusName, cycle.TerminationReason);

        foreach (var task in tasks.Where(t => t.CropCycleId == cycle.Id))
            Date(task.DueDate, "Task", task.TaskTypeName + " · " + task.StatusName,
                task.Description);

        foreach (var activity in monitoring.Activities)
            Instant(activity.OccurredAtUtc, "Activity", activity.ActivityTypeName,
                activity.Notes);

        foreach (var observation in monitoring.Observations)
        {
            Instant(observation.ObservedAtUtc, "Inspection", observation.HealthStatusName,
                string.Join(" · ", new[] { observation.GrowthStage, observation.Notes }
                    .Where(s => !string.IsNullOrWhiteSpace(s))));
            foreach (var photo in observation.Photos)
                Instant(photo.UploadedAtUtc, "Photo", "Inspection photo", null, photo.Id);
        }

        foreach (var harvest in harvests.Entries)
            Instant(harvest.HarvestedAtUtc, "Harvest",
                harvest.IsVoided ? "Voided harvest" : "Harvest recorded",
                $"{harvest.Quantity:N3} {harvest.Unit}" +
                (string.IsNullOrWhiteSpace(harvest.Notes) ? "" : $" · {harvest.Notes}") +
                (harvest.IsVoided ? $" · {harvest.VoidReason}" : ""));

        var ordered = events.OrderByDescending(e => e.FarmDate)
            .ThenByDescending(e => e.FarmTime).ThenBy(e => e.Category).ToList();
        return new CropTimelinePage(cycle, ordered.Take(100).ToList(),
            ordered.Count, timeZoneId);
    }
}
