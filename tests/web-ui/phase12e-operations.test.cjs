"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.join(__dirname, "../../src/SmartFarm.Web");
const read = file => fs.readFileSync(path.join(root, file), "utf8");
const tasks = read("Views/Tasks/Index.cshtml");
const monitoring = read("Views/Monitoring/Index.cshtml");
const harvests = read("Views/Harvests/Index.cshtml");
const inventory = read("Views/Inventory/Index.cshtml");
const item = read("Views/Inventory/Details.cshtml");
const itemEdit = read("Views/Inventory/Edit.cshtml");
const weather = read("Views/Weather/Index.cshtml");
const report = read("Views/Reports/Harvests.cshtml");
const css = read("Styles/anigo-operations-12e.css");

test("Phase 12E scopes a responsive theme to operational screens", () => {
    assert.ok(read("Styles/anigo-tailwind.css").includes('@import "./anigo-operations-12e.css"'));
    for (const name of ["ani12e-tasks", "ani12e-monitoring", "ani12e-harvests",
        "ani12e-inventory", "ani12e-stock-detail", "ani12e-weather", "ani12e-reports"]) {
        assert.ok(css.includes("." + name), "missing " + name);
    }
    for (const rule of ["--ui-border", "--ui-surface", "--ui-primary",
        "max-width:760px", "max-width:420px", "prefers-reduced-motion:reduce", ":focus-visible"]) {
        assert.ok(css.includes(rule), rule);
    }
    assert.ok(css.includes("min-height: 44px"));
    assert.ok(css.includes(".ani12e-hero .page-hero > div:first-child { min-width: 0; flex: 1 1 auto; }"));
});

test("Tasks preserve assignment, completion and calendar navigation", () => {
    assert.ok(tasks.includes("ani12e-task-workspace"));
    assert.ok(tasks.includes("ani12e-task-list"));
    assert.ok(tasks.includes("ani12e-mobile-agenda"));
    assert.ok(tasks.includes("monthTasks"));
    assert.ok(tasks.includes("asp-fragment=\"task-calendar-heading\""));
    assert.ok(tasks.includes('asp-route-view="all"'));
    assert.ok(tasks.includes('name="RowVersion"'));
    assert.ok(tasks.includes('name="returnView"'));
    assert.ok(tasks.includes('task.AssignedToUserId == actorId'));
    assert.ok(tasks.includes('assigned == actorId ? "You"'));
    assert.ok(tasks.includes('?? "Farm member"'));
    assert.ok(css.includes(".ani12e-task-calendar .reference-calendar { display: none; }"));
});

test("Monitoring protects private photos and retains save-then-upload flow", () => {
    assert.ok(monitoring.includes("ani12e-recording-grid"));
    assert.ok(monitoring.includes("ani12e-photo-grid"));
    assert.ok(monitoring.includes("Attach to saved inspection"));
    assert.ok(monitoring.includes("@if (monitoring.CanRecord)"));
    assert.ok(monitoring.includes('asp-action="Activity"'));
    assert.ok(monitoring.includes('asp-action="Observation"'));
    assert.ok(monitoring.includes('asp-action="Upload"'));
    assert.ok(monitoring.includes('asp-action="Photo"'));
    assert.ok(monitoring.includes('asp-route-photoId="@entry.Photo.Id"'));
    assert.ok(monitoring.includes("private images"));
});

test("Harvests preserve partial totals and valid/voided status", () => {
    assert.ok(harvests.includes("ani12e-harvests"));
    assert.ok(harvests.includes("ani12e-history-panel"));
    assert.ok(harvests.includes("@overview.TotalQuantity"));
    assert.ok(harvests.includes("@overview.VoidedHarvestCount"));
    assert.ok(harvests.includes("@if (overview.CanRecord)"));
    assert.ok(harvests.includes('asp-action="Record"'));
    assert.ok(harvests.includes('asp-action="Void"'));
    assert.ok(harvests.includes("offline entries appear as pending"));
});

test("Inventory keeps stock history, thresholds and explicit activity linkage", () => {
    assert.ok(inventory.includes("ani12e-inventory-table"));
    assert.ok(inventory.includes("ani12e-search-form"));
    assert.ok(inventory.includes('asp-route-filter="low"'));
    assert.ok(inventory.includes("@item.QuantityOnHand"));
    assert.ok(inventory.includes("item.IsLowStock"));
    assert.ok(inventory.includes("@if (canManage)"));
    assert.ok(item.includes("ani12e-movement-form"));
    assert.ok(item.includes('data-inventory-movement-form'));
    assert.ok(item.includes('name="CropActivityId"'));
    assert.ok(item.includes('name="RowVersion"'));
    assert.ok(item.includes('@if (canManage) { <span>+ Supplies arrived</span> }'));
    assert.ok(item.includes('asp-action="Reverse"'));
    assert.ok(itemEdit.includes("ani12e-form-actions"));
    assert.ok(itemEdit.includes('asp-for="Input.RowVersion"'));
});

test("Weather never represents stored forecast as live conditions", () => {
    assert.ok(weather.includes("ani12e-weather-safety"));
    assert.ok(weather.includes("Saved forecast, not live conditions."));
    assert.ok(weather.includes("Saved forecast may be outdated"));
    assert.ok(weather.includes("lastChecked"));
    assert.ok(weather.includes("TimeZoneId"));
    assert.ok(weather.includes('role="alert"'));
    assert.ok(weather.includes("ani12e-forecast-grid"));
    assert.ok(weather.includes("ani12e-weather-advisories"));
});

test("Reports retain unit-separated totals, filters and exports", () => {
    assert.ok(report.includes("ani12e-reports"));
    assert.ok(report.includes("ani12e-report-filter-form"));
    assert.ok(report.includes("ani12e-unit-totals"));
    assert.ok(report.includes("ani12e-report-results"));
    assert.ok(report.includes("role=\"status\""));
    assert.ok(report.includes("Filter results or download CSV"));
    assert.ok(report.includes('class="ani-report-filter-details ani12e-report-filters" open'));
    assert.ok(report.includes('asp-action="ExportCsv"'));
    assert.ok(report.includes('asp-action="ExportCycleCsv"'));
    assert.ok(report.includes('asp-route-plotId="@report.PlotId"'));
    assert.ok(report.includes('asp-route-cropTypeId="@report.CropTypeId"'));
    assert.ok(report.includes("Each unit has its own total"));
    assert.ok(report.includes("Voided harvests are excluded"));
});
