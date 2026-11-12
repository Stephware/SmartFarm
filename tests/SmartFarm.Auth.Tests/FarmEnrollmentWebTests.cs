using System.Net;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using System.Reflection;
using System.Security.Claims;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Contracts.Weather;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Finance;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Core.Interfaces.Reports;
using SmartFarm.Core.Interfaces.Crops;
using SmartFarm.Core.Interfaces.Tasks;
using SmartFarm.Core.Interfaces.Weather;
using SmartFarm.Core.Interfaces.Monitoring;
using SmartFarm.Core.Interfaces.Harvests;
using SmartFarm.Core.Interfaces.Inventory;
using SmartFarm.Core.Interfaces.Finance;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class FarmEnrollmentWebTests
{
    private static readonly Guid Actor = Guid.NewGuid(), FarmId = Guid.NewGuid(), RequestId = Guid.NewGuid();
    private static readonly FarmJoinRequestDetails Pending = new(RequestId, FarmId, "Green Farm", Guid.NewGuid(),
        "New Farmer", "applicant@example.test", "Pending", DateTime.UtcNow, null);

    [Theory]
    [InlineData("Owner")]
    [InlineData("Manager")]
    [InlineData("FieldWorker")]
    public async Task FarmPagesKeepResponsiveWorkspaceAndFarmSectionNavigation(string role)
    {
        using var app = App(role);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");

        Assert.Contains("class=\"app-shell\"", html);
        Assert.Matches("<aside[^>]+class=\"[^\"]*\\bapp-sidebar\\b[^\"]*\"", html);
        Assert.Contains("class=\"mobile-nav\"", html);
        Assert.Contains("aria-label=\"Mobile workspace navigation\"", html);
        Assert.Contains("aria-label=\"Farm sections\"", html);
        Assert.Contains("href=\"#main-content\"", html);
        Assert.Contains("id=\"main-content\"", html);
        Assert.Contains("aria-current=\"page\"", html);
        Assert.Contains("Farm &amp; team", html);
        Assert.Contains("data-nav-section=\"farm\"", html);
        Assert.Contains("id=\"sidebar-farm-context\"", html);
    }

    [Fact]
    public async Task FirstTimeFarmerSeesCreateAndJoinOptionsWithoutEmptyRequests()
    {
        using var app = App(enroll: (name, _) => name == "MyRequestsAsync"
            ? Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([])
            : throw new NotSupportedException(name));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Farms");
        Assert.Contains("No farms yet", html);
        Assert.Contains("Manage your own farm", html);
        Assert.Contains("Join an existing farm", html);
        Assert.Contains("Create my first farm", html);
        Assert.Contains("href=\"#join-farm\"", html);
        Assert.Contains("id=\"join-farm\"", html);
        Assert.Contains("name=\"FarmCode\"", html);
        Assert.Contains("Request to join", html);
        Assert.DoesNotContain("id=\"requests-heading\"", html);
        Assert.Contains("name=\"__RequestVerificationToken\"", html);
    }

    [Fact]
    public async Task FarmCreationKeepsRequiredFieldsAndMakesMapOptional()
    {
        using var app = App();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Farms/Create");
        Assert.Contains("Create farm &amp; continue", html);
        Assert.Contains("id=\"farm-advanced-settings\"", html);
        Assert.Contains("data-farm-location-picker", html);
        Assert.Contains("name=\"Name\"", html);
        Assert.Contains("name=\"LocationText\"", html);
        Assert.Contains("name=\"TimeZoneId\"", html);
        Assert.Contains("Asia/Manila", html);
        Assert.Contains("name=\"__RequestVerificationToken\"", html);
    }

    [Theory]
    [InlineData("Owner", true)]
    [InlineData("Manager", true)]
    [InlineData("FieldWorker", false)]
    public async Task FirstPlotGuideRespectsFarmRole(string role, bool canManage)
    {
        using var app = App(role);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        Assert.Contains("No active plots", html);
        if (canManage)
        {
            Assert.Contains("Your farm is created.", html);
            Assert.Contains("href=\"#add-plot\"", html);
            Assert.Contains("id=\"add-plot\"", html);
            Assert.Contains("CreatePlot", html);
        }
        else
        {
            Assert.DoesNotContain("Your farm is created.", html);
            Assert.DoesNotContain("id=\"add-plot\"", html);
            Assert.DoesNotContain("CreatePlot", html);
            Assert.Contains("Your farm owner or manager can add plots", html);
        }
    }

    [Fact]
    public async Task ExistingPlotsLeadDirectlyToCropPlanning()
    {
        using var app = App(plots: [new(Guid.NewGuid(), FarmId, "North field", 150m, "version")]);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        Assert.Contains("Ready to plant?", html);
        Assert.Contains("Plan a crop", html);
        Assert.DoesNotContain("Your farm is created.", html);
    }

    [Fact]
    public async Task MyFarmsShowsPendingStatusAndAcceptsCodeWithAntiforgery()
    {
        string? submitted = null;
        using var app = App(enroll: (method, args) => method switch
        {
            "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
            "RequestAsync" => CaptureCode(args),
            _ => throw new NotSupportedException(method)
        });
        Task<FarmJoinRequestDetails> CaptureCode(object?[]? args) { submitted = args![1]?.ToString(); return Task.FromResult(Pending); }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        Assert.Contains("Pending", html);
        Assert.Contains("Green Farm", html);
        Assert.Contains("owner approves", html);
        Assert.DoesNotContain("Invitation token", html);
        using var missingToken = await client.PostAsync("/Farms/RequestJoin", new FormUrlEncodedContent(new Dictionary<string,string> { ["FarmCode"] = "ABCD-1234-5678" }));
        Assert.Equal(HttpStatusCode.BadRequest, missingToken.StatusCode);
        using var posted = await client.PostAsync("/Farms/RequestJoin", Form(html, ("FarmCode", "ABCD-1234-5678")));
        Assert.Equal(HttpStatusCode.Redirect, posted.StatusCode);
        Assert.Equal("ABCD-1234-5678", submitted);
    }

    [Fact]
    public async Task OwnerDashboardShowsNamedDecisionsButWorkerDashboardDoesNotExposeApplicants()
    {
        foreach (var role in new[] { "Owner", "Manager", "FieldWorker" })
        {
            using var app = App(role);
            using var client = app.CreateClient();
            var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
            if (role == "Owner")
            {
                Assert.Contains("ABCD12345678", html);
                Assert.Contains("applicant@example.test", html);
                Assert.Contains("Approve New Farmer", html);
                Assert.Contains("Reject New Farmer", html);
            }
            else
            {
                Assert.DoesNotContain("ABCD12345678", html);
                Assert.DoesNotContain("applicant@example.test", html);
            }
        }
    }

    [Theory]
    [InlineData("Owner", true)]
    [InlineData("Manager", true)]
    [InlineData("FieldWorker", false)]
    public async Task FarmerDashboardShowsPrioritiesAndRoleAppropriateActions(string role, bool canManage)
    {
        using var app = App(role);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
        Assert.Contains("Today's priorities", html);
        Assert.Contains("Overdue tasks", html);
        Assert.Contains("Due today", html);
        Assert.Contains("Needs attention", html);
        Assert.Contains("Quick actions", html);
        Assert.True(html.IndexOf("Today's priorities", StringComparison.Ordinal) <
                    html.IndexOf("Farm at a glance", StringComparison.Ordinal));
        if (canManage)
        {
            Assert.Contains("Add a task", html);
            Assert.Contains("Add first plot", html);
        }
        else
        {
            Assert.Contains("Review tasks", html);
            Assert.DoesNotContain("Add a task", html);
            Assert.DoesNotContain("Add first plot", html);
        }
    }

    [Fact]
    public async Task DashboardDueAndFutureTasksLinkToMatchingTaskViews()
    {
        var overdue = new FarmTaskDetails(Guid.NewGuid(), FarmId, null, Actor,
            1, new DateOnly(2026, 10, 26), 0, "Check irrigation",
            DateTime.UtcNow, null, "v");
        var upcoming = overdue with { Id = Guid.NewGuid(), DueDate = new DateOnly(2026, 10, 29) };
        using var app = App(taskList: [upcoming, overdue]);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
        Assert.Contains("Assigned to you", html);
        Assert.Contains($"#task-{overdue.Id}", html);
        Assert.Contains($"#task-{upcoming.Id}", html);
        Assert.Matches(@"(?s)view=today[^""]*#task-" + overdue.Id, html);
        Assert.Matches(@"(?s)view=upcoming[^""]*#task-" + upcoming.Id, html);
    }

    [Fact]
    public async Task DashboardRecordActionUsesActiveCropMonitoringInsteadOfGenericCropList()
    {
        var cycle = new CropCycleDetails(Guid.NewGuid(), FarmId, Guid.NewGuid(), "North plot",
            Guid.NewGuid(), "Tomato", CropCycleStatus.Active,
            new DateOnly(2026, 10, 1), new DateOnly(2026, 10, 2),
            new DateOnly(2026, 11, 20), "kg", 3,
            null, null, "v");
        using var app = App(cycles: [cycle]);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
        Assert.Contains("Record fieldwork", html);
        Assert.Contains("Record activity", html);
        Assert.Contains("Record harvest", html);
        Assert.Contains("record-activity", html);
        Assert.Contains("record-harvest", html);
        Assert.Contains(cycle.Id.ToString(), html);
    }


    [Theory]
    [InlineData("Owner", true)]
    [InlineData("Manager", true)]
    [InlineData("FieldWorker", false)]
    public async Task PlotOverviewShowsCropStatusAndRoleAppropriateActions(string role, bool canManage)
    {
        var growingPlot = Guid.NewGuid();
        var emptyPlot = Guid.NewGuid();
        var planted = new CropCycleDetails(Guid.NewGuid(), FarmId, growingPlot,
            "North field", Guid.NewGuid(), "Tomato", CropCycleStatus.Active,
            new DateOnly(2026, 10, 10), new DateOnly(2026, 10, 11),
            new DateOnly(2026, 12, 15), "kg", 3, null, null, "v");
        var plots = new[]
        {
            new PlotDetails(growingPlot, FarmId, "North field", 120m, "v"),
            new PlotDetails(emptyPlot, FarmId, "South field", 100m, "v")
        };
        using var app = App(role: role, plots: plots, cycles: [planted]);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/PlotLayout?farmId={FarmId}");
        Assert.Contains("Your growing plots", html);
        Assert.Contains("Illustrative layout only", html);
        Assert.Contains("Growing", html);
        Assert.Contains("North field", html);
        Assert.Contains("South field", html);
        Assert.Contains("View crop details", html);
        Assert.Contains("plotId=" + emptyPlot, html);
        Assert.Contains("planting-plans-heading", html);
        Assert.Contains(planted.Id.ToString(), html);
        if (canManage)
        {
            Assert.Contains("Add plot", html);
            Assert.Contains("Plan a crop", html);
        }
        else
        {
            Assert.DoesNotContain(">Add plot</a>", html);
            Assert.Contains("View planting plans", html);
        }
    }

    [Fact]
    public async Task EmptyPlotOverviewGuidesOwnerButNotWorkerToAddPlot()
    {
        foreach (var role in new[] { "Owner", "FieldWorker" })
        {
            using var app = App(role);
            using var client = app.CreateClient();
            var html = await client.GetStringAsync($"/PlotLayout?farmId={FarmId}");
            Assert.Contains("No active plots yet", html);
            if (role == "Owner")
            {
                Assert.Contains("Add your first plot", html);
                Assert.Contains("add-plot", html);
            }
            else
            {
                Assert.DoesNotContain("Add your first plot", html);
                Assert.Contains("farm owner or manager can add plots", html);
            }
        }
    }

    [Theory]
    [InlineData("Owner", true)]
    [InlineData("FieldWorker", false)]
    public async Task CropPageGuidesPlotAndKeepsReadOnlyPermissions(string role, bool canManage)
    {
        var plotId = Guid.NewGuid();
        var cropTypeId = Guid.NewGuid();
        var active = new CropCycleDetails(Guid.NewGuid(), FarmId, plotId, "North field",
            cropTypeId, "Tomato", CropCycleStatus.Active,
            new DateOnly(2026, 10, 5), new DateOnly(2026, 10, 6),
            new DateOnly(2026, 12, 10), "kg", 4, null, null, "v");
        var plots = new[] { new PlotDetails(plotId, FarmId, "North field", 100m, "v") };
        var cropTypes = new[] { new CropTypeDetails(cropTypeId, "Tomato", 95, 4, "kg") };
        using var app = App(role: role, plots: plots, cycles: [active], cropTypes: cropTypes);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Crops?farmId={FarmId}&plotId={plotId}");
        Assert.Contains("Selected plot: North field", html);
        Assert.Contains("Currently growing", html);
        Assert.Contains("Plan a crop", html);
        Assert.Contains("Crop records", html);
        Assert.Contains("Open monitoring", html);
        Assert.Contains("Crop types &amp; history", html);
        Assert.Contains("crop-cycle-" + active.Id, html);
        Assert.True(html.IndexOf("id=\"ani-current-heading\"", StringComparison.Ordinal) <
                    html.IndexOf("id=\"planting-plans-heading\"", StringComparison.Ordinal));
        Assert.True(html.IndexOf("id=\"planting-plans-heading\"", StringComparison.Ordinal) <
                    html.IndexOf("id=\"crop-cycles-heading\"", StringComparison.Ordinal));
        if (canManage)
        {
            Assert.Contains("Save planting plan", html);
            Assert.Contains("Manage crop cycle", html);
        }
        else
        {
            Assert.DoesNotContain("Save planting plan", html);
            Assert.DoesNotContain("Manage crop cycle", html);
            Assert.DoesNotContain("Add a crop type", html);
        }
        Assert.Contains("Crop calendar", html);
    }

    [Fact]
    public async Task CropPlanningWithoutTypesShowsTheNextStepAndAccessibleAdvancedTools()
    {
        using var app = App(role: "Owner", plots:
            [new PlotDetails(Guid.NewGuid(), FarmId, "South field", 80m, "v")]);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Crops?farmId={FarmId}");
        Assert.Contains("Set up a crop type first", html);
        Assert.Contains("id=\"crop-advanced\"", html);
        Assert.Contains("Add crop type", html);
        Assert.DoesNotContain("Save planting plan", html);
    }

    [Fact]
    public async Task AssignedTaskFilterShowsOnlyMyOpenTasksAndPreservesCompletionView()
    {
        var assigned = new FarmTaskDetails(Guid.NewGuid(), FarmId, null, Actor,
            1, new DateOnly(2026, 11, 12), 0, "Check North field",
            DateTime.UtcNow, null, "v");
        var other = assigned with { Id = Guid.NewGuid(), AssignedToUserId = Guid.NewGuid(),
            Description = "Other worker task" };
        using var app = App("FieldWorker", taskList: [assigned, other]);
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var path = $"/Tasks?farmId={FarmId}&view=mine";
        var html = await client.GetStringAsync(path);
        Assert.Contains("Assigned to me", html);
        // The calendar intentionally links to all farm tasks, even when the
        // selected list is filtered to only this worker's assignments.
        Assert.Contains("id=\"task-" + assigned.Id + "\"", html);
        Assert.DoesNotContain("id=\"task-" + other.Id + "\"", html);
        Assert.Contains("name=\"returnView\" value=\"mine\"", html);
        using var response = await client.PostAsync(
            $"/Tasks/Close?farmId={FarmId}&taskId={assigned.Id}",
            Form(html, ("Status", "1"), ("RowVersion", assigned.RowVersion), ("returnView", "mine")));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Contains("view=mine", response.Headers.Location?.ToString());
        var after = await client.GetStringAsync(response.Headers.Location!);
        Assert.Contains("Task marked completed.", after);
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task FieldworkViewExplainsSaveThenPhotoStepsOnlyForActiveCrops(bool canRecord)
    {
        var cycle = new CropCycleDetails(Guid.NewGuid(), FarmId, Guid.NewGuid(), "North field",
            Guid.NewGuid(), "Tomato", canRecord ? CropCycleStatus.Active : CropCycleStatus.Completed,
            new DateOnly(2026, 10, 2), new DateOnly(2026, 10, 3),
            new DateOnly(2026, 12, 1), "kg", 4, null, null, "v");
        var monitoring = new MonitoringDetails(FarmId, cycle.Id, canRecord, [], []);
        using var app = App("FieldWorker", cycles: [cycle], monitoring: monitoring);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Monitoring?farmId={FarmId}&cycleId={cycle.Id}");
        Assert.Contains("Activity history", html);
        Assert.Contains("Inspections &amp; photos", html);
        Assert.Contains("Harvests", html);
        Assert.Contains("record-harvest", html);
        if (canRecord)
        {
            Assert.Contains("Save activity", html);
            Assert.Contains("Save inspection", html);
            Assert.Contains("Attach photos", html);
            Assert.Contains("Optional growth stage", html);
        }
        else
        {
            Assert.DoesNotContain("Save activity", html);
            Assert.DoesNotContain("Save inspection", html);
            Assert.Contains("Historical records remain available", html);
        }
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task HarvestViewPlacesRecordingBeforeHistoryWithoutChangingTotalsOrPermissions(bool canRecord)
    {
        var cycle = new CropCycleDetails(Guid.NewGuid(), FarmId, Guid.NewGuid(), "South field",
            Guid.NewGuid(), "Rice", canRecord ? CropCycleStatus.Active : CropCycleStatus.Completed,
            new DateOnly(2026, 10, 2), new DateOnly(2026, 10, 3),
            new DateOnly(2026, 12, 1), "kg", 5, null, null, "v");
        var entry = new HarvestEntry(Guid.NewGuid(), cycle.Id, 12.5m, "kg",
            DateTime.UtcNow, DateTime.UtcNow, Actor, "First cut",
            false, null, null, null);
        var harvest = new HarvestOverview(FarmId, cycle.Id, cycle.CropTypeName,
            cycle.PlotName, "kg", (byte)cycle.Status, canRecord, false, 12.5m, 1, 0, [entry]);
        using var app = App("FieldWorker", cycles: [cycle], harvest: harvest);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Harvests?farmId={FarmId}&cycleId={cycle.Id}");
        Assert.Contains("Confirmed harvest total", html);
        Assert.Contains("12.500", html);
        Assert.Contains("First cut", html);
        Assert.Contains("crop cycle", html);
        Assert.Contains("Saved harvest records", html);
        if (canRecord)
        {
            Assert.Contains("Save partial harvest", html);
            Assert.True(html.IndexOf("id=\"record-harvest\"", StringComparison.Ordinal) <
                        html.IndexOf("id=\"harvest-history-heading\"", StringComparison.Ordinal));
        }
        else
        {
            Assert.DoesNotContain("id=\"record-harvest\"", html);
            Assert.Contains("New harvests can only be recorded", html);
        }
    }


    [Theory]
    [InlineData("Owner", true)]
    [InlineData("FieldWorker", false)]
    public async Task InventoryLowStockAndMovementControlsRespectFarmRole(string role, bool canManage)
    {
        var itemId = Guid.NewGuid();
        var item = new InventoryItemDetails(itemId, FarmId, "Rice seed", InventoryCategory.Seeds,
            "kg", null, 2m, 5m, true, null, "version");
        using var app = App(role: role, inventory: new InventoryWorkspace([item], 1), inventoryItem: item);
        using var client = app.CreateClient();
        var overview = await client.GetStringAsync($"/Inventory?farmId={FarmId}");
        Assert.Contains("Needs restocking (1)", overview);
        Assert.Contains("Rice seed", overview);
        Assert.Contains("Low stock", overview);
        if (canManage) Assert.Contains("Add supply", overview);
        else Assert.DoesNotContain(">Add supply</a>", overview);
        var details = await client.GetStringAsync($"/Inventory/Details?farmId={FarmId}&itemId={itemId}");
        Assert.Contains("data-inventory-movement-form", details);
        Assert.Contains("data-movement-kind", details);
        Assert.Contains("data-movement-activity", details);
        Assert.Contains("No recorded crop activity is available", details);
        Assert.Contains("Current", details, StringComparison.OrdinalIgnoreCase);
        if (canManage) Assert.Contains("Supplies arrived (add stock)", details);
        else Assert.DoesNotContain("Supplies arrived (add stock)", details);
    }

    [Fact]
    public async Task WeatherFreshnessUsesLatestSavedCheckWithoutClaimingLiveConditions()
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var old = new WeatherDayDetails(today, 20m, 30m, null,
            DateTime.UtcNow.AddHours(-24), "Open-Meteo", 35m, null);
        var recent = old with { Date = today.AddDays(1), RetrievedAtUtc = DateTime.UtcNow.AddMinutes(-3) };
        using var app = App(weather: new WeatherOverview(FarmId, true, "Asia/Manila", [old, recent], []));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Weather?farmId={FarmId}");
        Assert.Contains("Last checked:", html);
        Assert.Contains("Recently saved forecast", html);
        Assert.DoesNotContain("Weather data may be out of date", html);
        Assert.Contains("not a live weather feed", html);
    }

    [Fact]
    public async Task OldSavedWeatherDisplaysAnExplicitStaleWarning()
    {
        var day = new WeatherDayDetails(DateOnly.FromDateTime(DateTime.UtcNow),
            24m, 32m, 12m, DateTime.UtcNow.AddHours(-16), "Open-Meteo", 45m, null);
        using var app = App(weather: new WeatherOverview(FarmId, true, "Asia/Manila", [day], []));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Weather?farmId={FarmId}");
        Assert.Contains("Weather data may be out of date", html);
        Assert.Contains("saved forecast", html, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task NotificationUnreadFilterAndReadActionKeepChosenView()
    {
        var unread = new NotificationDetails(Guid.NewGuid(), 1, "Check irrigation",
            "The north field is due", Guid.NewGuid(), null, DateTime.UtcNow, null);
        var read = new NotificationDetails(Guid.NewGuid(), 1, "Older weather reminder",
            "Previous forecast", null, null, DateTime.UtcNow.AddDays(-2), DateTime.UtcNow.AddDays(-1));
        using var app = App(notifications: [unread, read]);
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Notifications?view=unread");
        Assert.Contains("Check irrigation", html);
        Assert.DoesNotContain("Older weather reminder", html);
        Assert.Contains("name=\"returnView\" value=\"unread\"", html);
        using var response = await client.PostAsync($"/Notifications/Read?id={unread.Id}",
            Form(html, ("returnView", "unread")));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Contains("view=unread", response.Headers.Location?.ToString());
    }

    [Theory]
    [InlineData("Owner", true)]
    [InlineData("FieldWorker", false)]
    public async Task InviteAndApprovalControlsAppearOnlyForFarmOwner(string role, bool owner)
    {
        using var app = App(role);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        Assert.Contains("Farm team", html);
        if (owner)
        {
            Assert.Contains("Invite &amp; approve team members", html);
            Assert.Contains("Share farm joining code", html);
            Assert.Contains("Pending requests", html);
            Assert.Contains("Approve New Farmer", html);
            Assert.Contains("Request history", html);
            Assert.Contains("data-confirm=\"Reject the join request", html);
        }
        else
        {
            Assert.DoesNotContain("Invite &amp; approve team members", html);
            Assert.DoesNotContain("Approve New Farmer", html);
        }
    }

    [Fact]
    public async Task HarvestReportShowsUnitSeparatedSummaryBeforeRecentRecordsAndAdvancedCharts()
    {
        var plot = Guid.NewGuid();
        var cycle = Guid.NewGuid();
        var today = new DateOnly(2026, 11, 12);
        var report = new HarvestReport(FarmId, "Green Farm", "Asia/Manila",
            null, null, null, null, 3, 1,
            [new HarvestUnitTotal("kg", 24.5m, 2), new HarvestUnitTotal("pieces", 3m, 1)],
            [], [],
            [new HarvestReportEntry(Guid.NewGuid(), cycle, plot, "North field",
                Guid.NewGuid(), "Tomato", 24.5m, "kg", DateTime.UtcNow, today, false, null)]);
        using var app = App(harvestReport: report);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Reports/Harvests?farmId={FarmId}");
        Assert.Contains("Harvests at a glance", html);
        Assert.Contains("24.500 kg", html);
        Assert.Contains("3.000 pieces", html);
        Assert.Contains("Recent harvests", html);
        Assert.Contains("Filters &amp; downloads", html);
        Assert.Contains("Export records CSV", html);
        Assert.Contains("id=\"report-advanced\"", html);
        Assert.Contains("different units", html);
        Assert.True(html.IndexOf("Harvests at a glance", StringComparison.Ordinal) <
                    html.IndexOf("id=\"report-history-heading\"", StringComparison.Ordinal));
        Assert.True(html.IndexOf("id=\"report-history-heading\"", StringComparison.Ordinal) <
                    html.IndexOf("id=\"report-advanced\"", StringComparison.Ordinal));
    }

    [Fact]
    public async Task FilteredHarvestReportKeepsDateAndPlotSelectionAndClearAction()
    {
        var plotId = Guid.NewGuid();
        var from = new DateOnly(2026, 10, 1);
        var to = new DateOnly(2026, 10, 31);
        var report = new HarvestReport(FarmId, "Green Farm", "Asia/Manila",
            from, to, plotId, null, 0, 0, [], [], [], []);
        using var app = App(harvestReport: report,
            plots: [new PlotDetails(plotId, FarmId, "North field", 130m, "v")]);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync(
            $"/Reports/Harvests?farmId={FarmId}&from=2026-10-01&to=2026-10-31&plotId={plotId}");
        Assert.Contains("Filters applied", html);
        Assert.Contains("No recorded harvests for this view", html);
        Assert.Contains("See all harvests", html);
        Assert.Contains("value=\"2026-10-01\"", html);
        Assert.Contains("value=\"2026-10-31\"", html);
        Assert.Contains("plotId=" + plotId, html);
        Assert.Matches("(?s)class=\"ani-report-filter-details\"[^>]*open", html);
        Assert.Contains("ExportCycleCsv", html);
    }

    [Theory]
    [InlineData("Owner", true)]
    [InlineData("Manager", true)]
    [InlineData("FieldWorker", false)]
    public async Task FarmFinanceShowsManualCashButOnlyManagersCanAddEntries(string role, bool canManage)
    {
        var entry = new FarmFinanceEntryDetails(Guid.NewGuid(), FarmId, null,
            new DateOnly(2026, 10, 20), "Income", "Crop sale", 1000m, null,
            DateTime.UtcNow, false, null);
        var overview = new FarmFinanceOverview(FarmId, 300m, 1000m, 700m, [entry]);
        var insights = new FarmFinanceInsights(FarmId, new DateOnly(2026, 10, 1),
            new DateOnly(2026, 10, 31), 1000m, 300m, 700m, 2, [], [], []);
        using var app = App(role, financeOverview: overview, financeInsights: insights);
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/FarmFinance?farmId={FarmId}&from=2026-10-01&to=2026-10-31");
        Assert.Contains("₱1,000.00", html);
        Assert.Contains("₱300.00", html);
        Assert.Contains("₱700.00", html);
        Assert.Contains("A harvest is not automatically a sale", html);
        Assert.Contains("Crop sale", html);
        Assert.Contains("Date filters applied", html);
        Assert.Contains("finance-advanced", html);
        Assert.True(html.IndexOf("id=\"finance-history-heading\"", StringComparison.Ordinal) <
                    html.IndexOf("id=\"finance-advanced\"", StringComparison.Ordinal));
        if (canManage)
        {
            Assert.Contains("id=\"add-finance-entry\"", html);
            Assert.Contains("Save cash entry", html);
            Assert.True(html.IndexOf("id=\"add-finance-entry\"", StringComparison.Ordinal) <
                        html.IndexOf("id=\"finance-history-heading\"", StringComparison.Ordinal));
        }
        else
        {
            Assert.DoesNotContain("id=\"add-finance-entry\"", html);
            Assert.DoesNotContain("Save cash entry", html);
        }
    }

    [Theory]
    [InlineData("Owner", true)]
    [InlineData("Manager", true)]
    [InlineData("FieldWorker", false)]
    public async Task FarmSettingsStayRestrictedAndMakeExactMapOptional(string role, bool canEdit)
    {
        using var app = App(role);
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync($"/Farms/Edit?id={FarmId}");
        if (!canEdit)
        {
            Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
            return;
        }
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("Exact farm location on the map", html);
        Assert.Contains("ani-farm-map-options", html);
        Assert.Contains("data-farm-location-picker", html);
        Assert.Contains("name=\"Input.TimeZoneId\"", html);
        Assert.Contains("Save farm settings", html);
        Assert.Contains("name=\"Input.RowVersion\"", html);
    }

    [Fact]
    public async Task DashboardShowsOnlySavedFarmInformationAndKeepsQuickLinks()
    {
        using var app = App();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
        Assert.Contains("Today's work", html);
        Assert.Contains("Plots &amp; crops", html);
        Assert.Contains("Farm weather", html);
        Assert.Contains("aria-labelledby=\"weather-heading\"", html);
        Assert.Contains("Open tasks", html);
        Assert.Contains("No plots yet", html);
        Assert.Contains("Set the farm's map pin", html);
    }

    [Fact]
    public async Task DecisionUsesRouteRequestAndOwnerActorAndRequiresAntiforgery()
    {
        object?[]? decision = null;
        using var app = App(enroll: (name, args) => name switch
        {
            "OwnerOverviewAsync" => Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending])),
            "DecideAsync" => Capture(args),
            _ => throw new NotSupportedException(name)
        });
        Task Capture(object?[]? args) { decision = args; return Task.CompletedTask; }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Dashboard?farmId={FarmId}");
        var path = $"/Farms/DecideJoin?id={FarmId}&requestId={RequestId}&dashboard=true";
        using var missing = await client.PostAsync(path, new FormUrlEncodedContent(new Dictionary<string,string> { ["approve"] = "true" }));
        Assert.Equal(HttpStatusCode.BadRequest, missing.StatusCode);
        using var missingDecision = await client.PostAsync(path, Form(html));
        Assert.Equal(HttpStatusCode.BadRequest, missingDecision.StatusCode);
        Assert.Null(decision);
        using var response = await client.PostAsync(path, Form(html, ("approve", "true"), ("UserId", Guid.NewGuid().ToString()), ("Role", "Owner")));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal(FarmId, decision![0]); Assert.Equal(RequestId, decision[1]);
        Assert.Equal(Actor, decision[2]); Assert.Equal(true, decision[3]);
    }

    [Fact]
    public async Task RetiredInvitationLinksCannotGrantMembership()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync("/Farms/AcceptInvitation?token=old-link");
        Assert.Equal(HttpStatusCode.Gone, response.StatusCode);
        Assert.Contains("farm code", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task RegistrationAsksOnlyForNameEmailAndPassword()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Account/Register");
        foreach (var field in new[] { "FullName", "Email", "Password" })
            Assert.Contains($"name=\"{field}\"", html);
        foreach (var field in new[] { "JoinFarm", "FarmCode", "ConfirmPassword" })
            Assert.DoesNotContain($"name=\"{field}\"", html);
        var ids = Regex.Matches(html, "\\bid=\"([^\"]+)\"").Select(match => match.Groups[1].Value).ToList();
        Assert.Equal(ids.Count, ids.Distinct().Count());
    }

    [Fact]
    public async Task InvalidRegistrationKeepsEmailAndLinksItsErrorWithoutEchoingPassword()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Account/Register");
        using var response = await client.PostAsync("/Account/Register", Form(html,
            ("FullName", "New Farmer"), ("Email", "not-an-email"), ("Password", "StrongPassword123")));
        html = await response.Content.ReadAsStringAsync();
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Contains("href=\"#Email\"", html);
        Assert.Contains("not-an-email", html);
        Assert.DoesNotContain("value=\"StrongPassword123\"", html);
    }

    [Fact]
    public async Task SignedInLoginGoesToMyFarms()
    {
        using var app = App();
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        using var response = await client.GetAsync("/Account/Login");
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/Farms", response.Headers.Location?.OriginalString);
    }

    [Fact]
    public async Task PendingRequestCanBeCanceledWithAntiforgeryAndAuthenticatedActor()
    {
        object?[]? canceled = null;
        using var app = App(enroll: (name, args) => name switch
        {
            "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
            "CancelAsync" => Capture(args),
            _ => throw new NotSupportedException(name)
        });
        Task Capture(object?[]? args) { canceled = args; return Task.CompletedTask; }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        Assert.Contains("Cancel request", html);
        var path = $"/Farms/CancelJoin?farmId={FarmId}&requestId={RequestId}";
        using var missing = await client.PostAsync(path, new FormUrlEncodedContent([]));
        Assert.Equal(HttpStatusCode.BadRequest, missing.StatusCode);
        Assert.Null(canceled);
        using var response = await client.PostAsync(path, Form(html, ("actor", Guid.NewGuid().ToString())));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/Farms", response.Headers.Location?.OriginalString);
        Assert.Equal(FarmId, canceled![0]); Assert.Equal(RequestId, canceled[1]); Assert.Equal(Actor, canceled[2]);
    }

    [Fact]
    public async Task OwnerCanRegenerateCodeWithAntiforgeryAndCurrentCode()
    {
        object?[]? regenerated = null;
        using var app = App(enroll: (name, args) => name switch
        {
            "OwnerOverviewAsync" => Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending])),
            "RegenerateCodeAsync" => Capture(args),
            _ => throw new NotSupportedException(name)
        });
        Task<string> Capture(object?[]? args) { regenerated = args; return Task.FromResult("NEWC12345678"); }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        Assert.Contains("Copy code", html);
        Assert.Contains("Regenerate code", html);
        var path = $"/Farms/RegenerateCode?id={FarmId}";
        using var missing = await client.PostAsync(path, new FormUrlEncodedContent(new Dictionary<string,string> { ["currentCode"] = "ABCD12345678" }));
        Assert.Equal(HttpStatusCode.BadRequest, missing.StatusCode);
        Assert.Null(regenerated);
        using var response = await client.PostAsync(path, Form(html, ("currentCode", "ABCD12345678")));
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal(FarmId, regenerated![0]); Assert.Equal(Actor, regenerated[1]); Assert.Equal("ABCD12345678", regenerated[2]);
    }

    [Fact]
    public async Task StaleCodeRegenerationShowsConflictAndCurrentOwnerPanel()
    {
        using var app = App(enroll: (name, _) => name switch
        {
            "OwnerOverviewAsync" => Task.FromResult(new FarmEnrollmentOverview("NEWC12345678", [])),
            "RegenerateCodeAsync" => throw new FarmOperationException(FarmError.Conflict, "Farm code changed. Reload before trying again."),
            _ => throw new NotSupportedException(name)
        });
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        using var response = await client.PostAsync($"/Farms/RegenerateCode?id={FarmId}", Form(html, ("currentCode", "ABCD12345678")));
        Assert.Equal(HttpStatusCode.Conflict, response.StatusCode);
        html = await response.Content.ReadAsStringAsync();
        Assert.Contains("NEWC12345678", html);
        Assert.Contains("Farm code changed", html);
    }

    [Fact]
    public async Task MyFarmsShowsMultipleRolesAndTerminalRequestHistoryWithoutCancelControls()
    {
        var requests = new[] { Pending with { Status = "Approved" }, Pending with { Id = Guid.NewGuid(), Status = "Rejected" }, Pending with { Id = Guid.NewGuid(), Status = "Canceled" } };
        using var app = App(farms: [new(FarmId, "Green Farm", "Location", "Owner", "Asia/Manila"),
                new(Guid.NewGuid(), "Second Farm", "Location", "Manager", "Asia/Manila")],
            enroll: (name, _) => name == "MyRequestsAsync"
                ? Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>(requests) : throw new NotSupportedException(name));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync("/Farms");
        Assert.Contains("Create farm", html); Assert.Contains("Join farm", html);
        Assert.Contains("Owner", html); Assert.Contains("Manager", html); Assert.Contains("Second Farm", html);
        foreach (var status in new[] { "Approved", "Rejected", "Canceled" }) Assert.Contains(status, html);
        Assert.DoesNotContain("Cancel request", html);
        Assert.Contains("valid farm code", html);
    }

    [Fact]
    public async Task OwnerPanelSeparatesPendingDecisionsFromTerminalHistory()
    {
        using var app = App(enroll: (name, _) => name == "OwnerOverviewAsync"
            ? Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending],
                [Pending with { Id = Guid.NewGuid(), FullName = "Former Applicant", Status = "Canceled" }]))
            : throw new NotSupportedException(name));
        using var client = app.CreateClient();
        var html = await client.GetStringAsync($"/Farms/Details?id={FarmId}");
        Assert.Contains("Approve New Farmer", html);
        Assert.Contains("Former Applicant", html); Assert.Contains("Canceled", html);
        Assert.DoesNotContain("Approve Former Applicant", html);
    }

    [Theory]
    [InlineData("/Farms/CancelJoin", "CancelAsync")]
    [InlineData("/Farms/RegenerateCode", "RegenerateCodeAsync")]
    public async Task EnrollmentDenialsReturnNotFound(string action, string operation)
    {
        using var app = App(enroll: (name, _) => name == operation
            ? throw new FarmOperationException(FarmError.NotFound, "Not found")
            : name == "MyRequestsAsync" ? Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending])
            : throw new NotSupportedException(name));
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        using var response = await client.PostAsync($"{action}?id={FarmId}&farmId={FarmId}&requestId={RequestId}",
            Form(html, ("currentCode", "ABCD12345678")));
        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task RegistrationRejectsMissingAntiforgery()
    {
        using var app = new WebApplicationFactory<Program>();
        using var client = app.CreateClient();
        using var response = await client.PostAsync("/Account/Register", new FormUrlEncodedContent(new Dictionary<string,string>
            { ["FullName"] = "Test Farmer", ["Email"] = "farmer@example.test", ["Password"] = "StrongPassword123" }));
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task OwnerDecisionsShareEnrollmentWriteBudgetAndReturnRetryAfter()
    {
        var decisions = 0;
        using var app = App(enroll: (name, _) => name switch
        {
            "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
            "DecideAsync" => Decide(),
            _ => throw new NotSupportedException(name)
        });
        Task Decide() { decisions++; return Task.CompletedTask; }
        using var client = app.CreateClient(new() { AllowAutoRedirect = false });
        var html = await client.GetStringAsync("/Farms");
        for (var attempt = 0; attempt < 9; attempt++)
        {
            using var response = await client.PostAsync("/Farms/RequestJoin", Form(html));
            Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        }
        var path = $"/Farms/DecideJoin?id={FarmId}&requestId={RequestId}";
        using (var allowed = await client.PostAsync(path, Form(html, ("approve", "true"))))
            Assert.Equal(HttpStatusCode.Redirect, allowed.StatusCode);
        using var rejected = await client.PostAsync("/Farms/RequestJoin", Form(html));
        Assert.Equal(HttpStatusCode.TooManyRequests, rejected.StatusCode);
        Assert.True(rejected.Headers.RetryAfter?.Delta > TimeSpan.Zero);
        Assert.Contains("Too many attempts", await rejected.Content.ReadAsStringAsync());
        foreach (var action in new[] { path, $"/Farms/CancelJoin?farmId={FarmId}&requestId={RequestId}", $"/Farms/RegenerateCode?id={FarmId}" })
        {
            using var response = await client.PostAsync(action, Form(html, ("approve", "true"), ("CurrentCode", "ABCD12345678")));
            Assert.Equal(HttpStatusCode.TooManyRequests, response.StatusCode);
            Assert.True(response.Headers.RetryAfter?.Delta > TimeSpan.Zero);
        }
        Assert.Equal(1, decisions);
        using var read = await client.GetAsync("/Farms");
        Assert.Equal(HttpStatusCode.OK, read.StatusCode);
    }

    private static FormUrlEncodedContent Form(string html, params (string Key, string Value)[] values)
    {
        var fields = values.ToDictionary(v => v.Key, v => v.Value);
        fields["__RequestVerificationToken"] = WebUtility.HtmlDecode(Regex.Match(html,
            "name=\"__RequestVerificationToken\"[^>]*value=\"([^\"]+)\"").Groups[1].Value);
        return new(fields);
    }

    private static WebApplicationFactory<Program> App(string role = "Owner", Func<string, object?[]?, object>? enroll = null, IReadOnlyList<FarmSummary>? farms = null, IReadOnlyList<PlotDetails>? plots = null,
        IReadOnlyList<CropCycleDetails>? cycles = null, IReadOnlyList<FarmTaskDetails>? taskList = null,
        IReadOnlyList<CropTypeDetails>? cropTypes = null,
        MonitoringDetails? monitoring = null, HarvestOverview? harvest = null,
        InventoryWorkspace? inventory = null, InventoryItemDetails? inventoryItem = null,
        WeatherOverview? weather = null, IReadOnlyList<NotificationDetails>? notifications = null,
        HarvestReport? harvestReport = null, FarmFinanceOverview? financeOverview = null,
        FarmFinanceInsights? financeInsights = null) =>
        new WebApplicationFactory<Program>().WithWebHostBuilder(builder => builder.ConfigureTestServices(services =>
        {
            services.AddSingleton<IAuthenticationService, SignedIn>();
            services.AddScoped(_ => Stub<IFarmManagementService>((name, _) => name switch
            {
                "ListAsync" => Task.FromResult<IReadOnlyList<FarmSummary>>(farms ?? []),
                "GetAsync" => Task.FromResult<FarmDetails?>(new(FarmId, "Green Farm", "Location", "Asia/Manila", null, null, role, "version")),
                "ListPlotsAsync" => Task.FromResult<IReadOnlyList<PlotDetails>>(plots ?? []),
                "MembersAsync" => Task.FromResult<IReadOnlyList<FarmMemberDetails>>([]),
                _ => throw new NotSupportedException(name)
            }));
            services.AddScoped(_ => Stub<IFarmReportService>((name, _) => name switch
            {
                "DashboardAsync" => Task.FromResult(
                    new FarmDashboard(FarmId, "Green Farm", new DateOnly(2026,10,27),
                        0,0,0,0,0,0,0,0,[],[])),
                "HarvestsAsync" => Task.FromResult(harvestReport ?? new HarvestReport(
                    FarmId, "Green Farm", "Asia/Manila",
                    null, null, null, null, 0, 0, [], [], [], [])),
                _ => throw new NotSupportedException(name)
            }));
            // The dashboard reads crop, task and saved-weather summaries as well as the report.
            services.AddScoped(_ => Stub<ICropManagementService>((name, args) => name switch
            {
                "ListCyclesAsync" => Task.FromResult<IReadOnlyList<CropCycleDetails>>(cycles ?? []),
                "ListCropTypesAsync" => Task.FromResult<IReadOnlyList<CropTypeDetails>>(cropTypes ?? []),
                "GetCycleAsync" => Task.FromResult<CropCycleDetails?>(
                    cycles?.FirstOrDefault(c => c.Id == (Guid)args![1]!)),
                _ => throw new NotSupportedException(name)
            }));
            services.AddScoped(_ => Stub<IFarmTaskService>((name, args) => name switch
            {
                "ListAsync" => Task.FromResult<IReadOnlyList<FarmTaskDetails>>(taskList ?? []),
                "NotificationsAsync" => Task.FromResult<IReadOnlyList<NotificationDetails>>(notifications ?? []),
                "MarkNotificationReadAsync" => Task.CompletedTask,
                "CloseAsync" => Task.FromResult((taskList ?? [])
                    .First(t => t.Id == (Guid)args![1]!) with { Status = 1 }),
                _ => throw new NotSupportedException(name)
            }));
            if (monitoring is not null)
            {
                services.AddScoped(_ => Stub<IMonitoringService>((name, _) => name switch
                {
                    "GetAsync" => Task.FromResult(monitoring),
                    _ => throw new NotSupportedException(name)
                }));
            }
            if (harvest is not null)
            {
                services.AddScoped(_ => Stub<IHarvestService>((name, _) => name switch
                {
                    "GetAsync" => Task.FromResult(harvest),
                    _ => throw new NotSupportedException(name)
                }));
            }
            if (financeOverview is not null || financeInsights is not null)
            {
                services.AddScoped(_ => Stub<IFarmFinanceService>((name, _) => name switch
                {
                    "ListAsync" => Task.FromResult(financeOverview ??
                        new FarmFinanceOverview(FarmId, 0m, 0m, 0m, [])),
                    "InsightsAsync" => Task.FromResult(financeInsights ??
                        new FarmFinanceInsights(FarmId, null, null, 0m, 0m, 0m, 0, [], [], [])),
                    _ => throw new NotSupportedException(name)
                }));
            }
            services.AddScoped(_ => Stub<IFarmWeatherService>((name, _) => name == "GetAsync"
                ? Task.FromResult(weather ?? new WeatherOverview(FarmId, false, "Asia/Manila", [], []))
                : throw new NotSupportedException(name)));
            if (inventory is not null || inventoryItem is not null)
            {
                services.AddScoped(_ => Stub<IInventoryService>((name, _) => name switch
                {
                    "ListAsync" => Task.FromResult(inventory ?? new InventoryWorkspace([], 0)),
                    "GetAsync" => Task.FromResult<InventoryItemDetails?>(inventoryItem),
                    "HistoryAsync" => Task.FromResult<IReadOnlyList<InventoryTransactionDetails>>([]),
                    "ActivityOptionsAsync" => Task.FromResult<IReadOnlyList<InventoryActivityOption>>([]),
                    _ => throw new NotSupportedException(name)
                }));
            }
            services.AddScoped(_ => Stub<IFarmEnrollmentService>(enroll ?? ((name, _) => name switch
            {
                "OwnerOverviewAsync" => role == "Owner" ? Task.FromResult(new FarmEnrollmentOverview("ABCD12345678", [Pending])) : throw new InvalidOperationException("Non-owner must not request applicants"),
                "MyRequestsAsync" => Task.FromResult<IReadOnlyList<FarmJoinRequestDetails>>([Pending]),
                _ => throw new NotSupportedException(name)
            })));
        }));

    private static T Stub<T>(Func<string, object?[]?, object> call) where T : class
    {
        var proxy = DispatchProxy.Create<T, Proxy>(); ((Proxy)(object)proxy).Call = call; return proxy;
    }
    public class Proxy : DispatchProxy
    {
        public Func<string, object?[]?, object> Call { get; set; } = null!;
        protected override object? Invoke(MethodInfo? method, object?[]? args) => Call(method!.Name, args);
    }
    private sealed class SignedIn : IAuthenticationService
    {
        public Task<AuthenticateResult> AuthenticateAsync(HttpContext context, string? scheme) => Task.FromResult(AuthenticateResult.Success(new AuthenticationTicket(
            new ClaimsPrincipal(new ClaimsIdentity([new Claim(ClaimTypes.NameIdentifier, Actor.ToString())], IdentityConstants.ApplicationScheme)), IdentityConstants.ApplicationScheme)));
        public Task ChallengeAsync(HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task ForbidAsync(HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task SignInAsync(HttpContext context, string? scheme, ClaimsPrincipal principal, AuthenticationProperties? properties) => Task.CompletedTask;
        public Task SignOutAsync(HttpContext context, string? scheme, AuthenticationProperties? properties) => Task.CompletedTask;
    }
}
