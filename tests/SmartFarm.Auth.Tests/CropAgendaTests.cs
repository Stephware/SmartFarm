using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Enums;
using SmartFarm.Web.ViewModels.Crops;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class CropAgendaTests
{
    private static CropCycleDetails Cycle(CropCycleStatus status, DateOnly planted,
        DateOnly? expected, short? watering = null) =>
        new(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(), "North Plot",
            Guid.NewGuid(), "Tomato", status, new DateOnly(2026, 10, 28),
            status == CropCycleStatus.Planned ? null : planted, expected, "kg",
            watering, null, null, "version");

    [Fact]
    public void AgendaShowsPlannedPlantingAndExpectedHarvestBeforePlanting()
    {
        var cycle = Cycle(CropCycleStatus.Planned, new DateOnly(2026, 11, 2),
            new DateOnly(2026, 11, 28));
        var calendar = CropAgendaBuilder.Build([cycle], new DateOnly(2026, 11, 15));

        Assert.Equal(new DateOnly(2026, 11, 1), calendar.FirstDay);
        Assert.Equal(new DateOnly(2026, 10, 1), calendar.PreviousMonth);
        Assert.Equal(new DateOnly(2026, 12, 1), calendar.NextMonth);
        Assert.Contains(calendar.Days.SelectMany(d => d.Entries),
            e => e.Kind == "Expected harvest" && e.Date == new DateOnly(2026, 11, 28));
        Assert.DoesNotContain(calendar.Days.SelectMany(d => d.Entries),
            e => e.Kind == "Planned planting");
        Assert.Contains(CropAgendaBuilder.Build([cycle], new DateOnly(2026, 10, 1))
            .Days.SelectMany(d => d.Entries),
            e => e.Kind == "Planned planting" && e.Date == new DateOnly(2026, 10, 28));
    }

    [Fact]
    public void AgendaReusesWateringScheduleAndOmitsEndedCrops()
    {
        var active = Cycle(CropCycleStatus.Active, new DateOnly(2026, 11, 1),
            new DateOnly(2026, 11, 21), watering: 5);
        var completed = Cycle(CropCycleStatus.Completed, new DateOnly(2026, 11, 1),
            new DateOnly(2026, 11, 25), watering: 5);

        var calendar = CropAgendaBuilder.Build([active, completed], new DateOnly(2026, 11, 1));
        var events = calendar.Days.SelectMany(d => d.Entries).ToArray();
        Assert.Equal(1, events.Count(e => e.Kind == "Planted"));
        Assert.Contains(events, e => e.Kind == "Watering" &&
            e.Date == new DateOnly(2026, 11, 6));
        Assert.Contains(events, e => e.Kind == "Expected harvest" &&
            e.Date == new DateOnly(2026, 11, 21));
        Assert.DoesNotContain(events, e => e.Date == new DateOnly(2026, 11, 25));
        Assert.Equal(events.Length, calendar.Days.Sum(d => d.Entries.Count));
    }

    [Fact]
    public void AgendaOmitsFutureBoundaryPlantingWithoutOverflowing()
    {
        var cycle = Cycle(CropCycleStatus.Active, DateOnly.MaxValue, null, watering: 1);

        var calendar = CropAgendaBuilder.Build([cycle], new DateOnly(2026, 10, 1));

        Assert.Empty(calendar.Days);
    }

    [Fact]
    public void MonthSelectionUsesFarmTimeZoneAndRejectsBadInput()
    {
        var utc = new DateTime(2026, 10, 31, 17, 0, 0, DateTimeKind.Utc);
        Assert.Equal(new DateOnly(2026, 11, 1),
            CropAgendaBuilder.SelectMonth(null, "Asia/Manila", utc));
        Assert.Equal(new DateOnly(2026, 12, 1),
            CropAgendaBuilder.SelectMonth("2026-12", "Asia/Manila", utc));
        Assert.Equal(new DateOnly(2026, 11, 1),
            CropAgendaBuilder.SelectMonth("not-a-month", "Asia/Manila", utc));
        Assert.Equal(new DateOnly(2026, 10, 1),
            CropAgendaBuilder.SelectMonth(null, "Invalid/FarmZone", utc));
    }
}
