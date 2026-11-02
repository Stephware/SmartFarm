using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using SmartFarm.Web.ViewModels.Monitoring;
using SmartFarm.Web.ViewModels.Reports;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class PhaseOneInsightsTests
{
    [Fact]
    public void TimelineCombinesCropMilestonesTasksInspectionsPhotosAndPartialHarvests()
    {
        var farmId = Guid.NewGuid();
        var cycleId = Guid.NewGuid();
        var cycle = new CropCycleDetails(cycleId, farmId, Guid.NewGuid(), "East Plot",
            Guid.NewGuid(), "Tomato", CropCycleStatus.Active, new DateOnly(2026, 10, 1),
            new DateOnly(2026, 10, 2), new DateOnly(2026, 11, 20), "kg", 3,
            null, null, "version");
        var photo = new PhotoDetails(Guid.NewGuid(), Guid.NewGuid(), "image/jpeg", 200,
            new DateTime(2026, 10, 4, 20, 0, 0, DateTimeKind.Utc));
        var monitoring = new MonitoringDetails(farmId, cycleId, true,
            [new ActivityDetails(Guid.NewGuid(), 1,
                new DateTime(2026, 10, 3, 20, 0, 0, DateTimeKind.Utc),
                new DateTime(2026, 10, 3, 20, 0, 0, DateTimeKind.Utc), Guid.NewGuid(), "Watered")],
            [new ObservationDetails(Guid.NewGuid(), 2, "Flowering",
                new DateTime(2026, 10, 4, 10, 0, 0, DateTimeKind.Utc),
                new DateTime(2026, 10, 4, 10, 0, 0, DateTimeKind.Utc),
                Guid.NewGuid(), "Yellow leaves", [photo])]);
        var harvest = new HarvestOverview(farmId, cycleId, "Tomato", "East Plot",
            "kg", 1, true, true, 3, 1, 1,
            [new HarvestEntry(Guid.NewGuid(), cycleId, 3, "kg",
                new DateTime(2026, 10, 8, 1, 0, 0, DateTimeKind.Utc),
                new DateTime(2026, 10, 8, 1, 0, 0, DateTimeKind.Utc), Guid.NewGuid(),
                "Partial", false, null, null, null),
             new HarvestEntry(Guid.NewGuid(), cycleId, 1, "kg",
                new DateTime(2026, 10, 7, 1, 0, 0, DateTimeKind.Utc),
                new DateTime(2026, 10, 7, 1, 0, 0, DateTimeKind.Utc), Guid.NewGuid(),
                null, true, DateTime.UtcNow, Guid.NewGuid(), "Mistake")]);
        var tasks = new[]
        {
            new FarmTaskDetails(Guid.NewGuid(), farmId, cycleId, null, 2,
                new DateOnly(2026, 10, 5), 0, "Check soil", DateTime.UtcNow, null, "v"),
            new FarmTaskDetails(Guid.NewGuid(), farmId, Guid.NewGuid(), null, 2,
                new DateOnly(2026, 10, 6), 0, null, DateTime.UtcNow, null, "v")
        };

        var timeline = CropTimelineBuilder.Build(cycle, monitoring, harvest, tasks, "Asia/Manila");
        Assert.Equal("Tomato", timeline.Cycle.CropTypeName);
        Assert.Equal(9, timeline.TotalCount);
        Assert.Equal(new DateOnly(2026, 10, 4),
            Assert.Single(timeline.Entries.Where(t => t.Title == "Watering")).FarmDate);
        Assert.Contains(timeline.Entries, t => t.PhotoId == photo.Id);
        Assert.Contains(timeline.Entries, t => t.Title == "Voided harvest");
        Assert.Contains(timeline.Entries, t => t.Title == "Watering · Pending");
        Assert.Contains(timeline.Entries, t => t.Title == "Expected harvest");
        Assert.DoesNotContain(timeline.Entries, t => t.Detail == "Unrelated");
        Assert.Equal(timeline.Entries.OrderByDescending(e => e.FarmDate)
            .Select(e => e.FarmDate), timeline.Entries.Select(e => e.FarmDate));
    }

    [Fact]
    public void DashboardSupportsOptionalActionableItemsWithoutChangingExistingConstructor()
    {
        var dashboard = new FarmDashboard(Guid.NewGuid(), "East Farm",
            new DateOnly(2026, 10, 30), 2, 1, 2, 1, 4, 1, 1, 1, [], []);
        Assert.Empty(dashboard.AttentionItems);

        var updated = dashboard with
        {
            AttentionItems = [new DashboardAttentionItem("task", Guid.NewGuid(),
                "Watering overdue", "Review the task", new DateOnly(2026, 10, 29))]
        };
        Assert.Single(updated.AttentionItems);
        Assert.Equal(4, updated.OpenTasks);
        Assert.Empty(dashboard.AttentionItems);
    }

    [Fact]
    public void HarvestChartsKeepUnitsSeparateAndNeverUseVoidedData()
    {
        var plotId = Guid.NewGuid();
        var report = new HarvestReport(Guid.NewGuid(), "Demo", "Asia/Manila",
            null, null, null, null, 3, 1,
            [new HarvestUnitTotal("kg", 30, 2), new HarvestUnitTotal("pieces", 5, 1)],
            [new HarvestCropPlotTotal(Guid.NewGuid(), "Tomato", plotId, "East Plot", "kg", 30, 2),
             new HarvestCropPlotTotal(Guid.NewGuid(), "Corn", Guid.NewGuid(), "West Plot", "pieces", 5, 1)],
            [new HarvestMonthlyTotal("2026-10", "kg", 10, 1),
             new HarvestMonthlyTotal("2026-11", "kg", 20, 1),
             new HarvestMonthlyTotal("2026-11", "pieces", 5, 1)],
            []);
        var plots = new[] { new PlotDetails(plotId, report.FarmId, "East Plot", 10m, "v") };

        var monthly = HarvestAnalyticsBuilder.Monthly(report);
        Assert.Equal(2, monthly.Count);
        var kilograms = Assert.Single(monthly.Where(m => m.Unit == "kg"));
        Assert.Equal(2, kilograms.Bars.Count);
        Assert.Equal(100, kilograms.Bars[0].WidthPercent);
        Assert.Equal(50, kilograms.Bars[1].WidthPercent);
        var cropPlot = HarvestAnalyticsBuilder.CropAndPlot(report, plots);
        Assert.Equal(2, cropPlot.Count);
        Assert.Equal(3, Assert.Single(cropPlot.Single(c => c.Unit == "kg").Bars)
            .YieldPerSquareMeter);
        Assert.Null(Assert.Single(cropPlot.Single(c => c.Unit == "pieces").Bars)
            .YieldPerSquareMeter);
    }
}
