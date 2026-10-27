using System.Globalization;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Entities;
using SmartFarm.Core.Enums;
using SmartFarm.Core.Services.Tasks;

namespace SmartFarm.Web.ViewModels.Crops;

public sealed record CropAgendaEntry(DateOnly Date, string Kind, string CropName, string PlotName);
public sealed record CropAgendaDay(DateOnly Date, IReadOnlyList<CropAgendaEntry> Entries);
public sealed record CropAgendaMonth(DateOnly FirstDay, DateOnly PreviousMonth,
    DateOnly NextMonth, IReadOnlyList<CropAgendaDay> Days);

public static class CropAgendaBuilder
{
    // Navigation is scoped to reasonable farm-planning dates, with no database changes.
    public static DateOnly SelectMonth(string? month, string timeZoneId, DateTime utcNow)
    {
        var current = TaskRules.FarmDate(timeZoneId, utcNow);
        var fallback = new DateOnly(current.Year, current.Month, 1);
        if (!DateOnly.TryParseExact(month is null ? "" : month + "-01", "yyyy-MM-dd",
                CultureInfo.InvariantCulture, DateTimeStyles.None, out var selected) ||
            selected.Year is < 2000 or > 2100)
            return fallback;
        return selected;
    }

    public static CropAgendaMonth Build(IReadOnlyList<CropCycleDetails> cycles, DateOnly month)
    {
        var first = new DateOnly(month.Year, month.Month, 1);
        var last = first.AddMonths(1).AddDays(-1);
        var entries = new List<CropAgendaEntry>();

        foreach (var cycle in cycles)
        {
            if (cycle.Status is not (CropCycleStatus.Planned or CropCycleStatus.Active))
                continue;

            var schedule = new CropCycle
            {
                Id = cycle.Id, Status = (byte)cycle.Status,
                PlannedPlantingDate = cycle.PlannedPlantingDate,
                PlantedDate = cycle.PlantedDate,
                ExpectedHarvestDate = cycle.ExpectedHarvestDate,
                WateringIntervalDays = cycle.WateringIntervalDays
            };

            foreach (var (type, due) in TaskRules.ScheduledTasks(schedule, first, last))
            {
                if (due < first || due > last) continue;
                var kind = type switch
                {
                    1 => "Planned planting",
                    2 => "Watering",
                    5 => "Expected harvest",
                    _ => null
                };
                if (kind is not null)
                    entries.Add(new CropAgendaEntry(due, kind, cycle.CropTypeName, cycle.PlotName));
            }

            // Planned crops also have an expected harvest even before planting begins.
            if (cycle.Status == CropCycleStatus.Planned &&
                cycle.ExpectedHarvestDate is { } plannedHarvest &&
                plannedHarvest >= first && plannedHarvest <= last)
                entries.Add(new CropAgendaEntry(plannedHarvest, "Expected harvest",
                    cycle.CropTypeName, cycle.PlotName));

            // Show the recorded planting date for already active crops.
            if (cycle.Status == CropCycleStatus.Active &&
                cycle.PlantedDate is { } planted && planted >= first && planted <= last)
                entries.Add(new CropAgendaEntry(planted, "Planted",
                    cycle.CropTypeName, cycle.PlotName));
        }

        var days = entries.GroupBy(e => e.Date).OrderBy(g => g.Key)
            .Select(g => new CropAgendaDay(g.Key, g.OrderBy(e => e.Kind)
                .ThenBy(e => e.PlotName).ThenBy(e => e.CropName).ToList()))
            .ToList();
        return new CropAgendaMonth(first, first.AddMonths(-1), first.AddMonths(1), days);
    }
}
