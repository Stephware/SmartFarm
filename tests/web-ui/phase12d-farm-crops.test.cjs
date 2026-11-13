"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = name => fs.readFileSync(path.join(root, name), "utf8");
const farm = read("Views/Farms/Details.cshtml");
const plots = read("Views/PlotLayout/Index.cshtml");
const crops = read("Views/Crops/Index.cshtml");
const editPlot = read("Views/Farms/EditPlot.cshtml");
const editPlan = read("Views/Crops/EditPlan.cshtml");
const style = read("Styles/anigo-farm-12d.css");

test("12D uses scoped Pine/Gold design without rewriting farm access", () => {
    assert.ok(read("Styles/anigo-tailwind.css").includes('@import "./anigo-farm-12d.css"'));
    for (const token of ["--ui-border", "--ui-surface", "--ui-primary", "--ui-text-secondary"])
        assert.ok(style.includes(token), token);
    for (const name of ["ani12d-farm-workspace", "ani12d-crop-workspace", "ani12d-plot-workspace"])
        assert.ok(style.includes(name), name);
    assert.match(style, /max-width:760px/);
    assert.match(style, /max-width:420px/);
    assert.match(style, /prefers-reduced-motion:reduce/);
});
test("Farm management puts plot and team workflows before destructive actions", () => {
    assert.ok(farm.includes('aria-label="Farm management sections"'));
    assert.ok(farm.includes('class="ani12d-farm-summary"'));
    assert.ok(farm.includes('id="add-plot"'));
    assert.ok(farm.includes('asp-action="CreatePlot"'));
    assert.ok(farm.includes('asp-action="EditPlot"'));
    assert.ok(farm.includes('asp-action="ArchivePlot"'));
    assert.ok(farm.includes('asp-fragment="@(canManage ? "planting-plans-heading" : "ani-current-heading")"'));
    assert.ok(farm.includes('asp-action="ChangeRole"'));
    assert.ok(farm.includes('asp-action="RemoveMember"'));
    assert.ok(farm.includes('@if (isOwner)'));
    assert.ok(farm.indexOf('class="ani12d-danger-options"') > farm.indexOf('id="members-heading"'));
    assert.ok(farm.includes('data-confirm="Archive @Model.Farm.Name?'));
});
test("Plot directory presents actual saved values without claiming geographic accuracy", () => {
    assert.ok(plots.includes('class="plot-layout-grid sf-plot-grid plot-board ani-fields-board ani12d-plot-directory"'));
    assert.ok(plots.includes("Plot directory, not a map."));
    assert.ok(plots.includes("@plot.AreaSqMeters.ToString"));
    assert.ok(plots.includes("aria-label=\"@plot.Name,"));
    assert.ok(plots.includes("asp-route-plotId=\"@plot.PlotId\""));
    assert.ok(plots.includes("CropArtwork.FileName(plot.CropName)"));
    assert.ok(plots.includes('canManage ? "planting-plans-heading" : "ani-current-heading"'));
    assert.doesNotMatch(plots, /actual field position or health status/);
});
test("Crop planning never implicitly selects an arbitrary plot or type", () => {
    assert.ok(crops.includes('option value="" disabled selected="@(Model.PlanPlotId is null)"'));
    assert.ok(crops.includes('option value="" disabled selected="@(Model.Plan.CropTypeId == Guid.Empty)"'));
    assert.ok(crops.includes('asp-route-plotId="@plot.Id"') || crops.includes('asp-route-plotId="@Model.Farm.Id"') || crops.includes('name="plotId"'));
    assert.ok(crops.includes('asp-for="Plan.PlannedPlantingDate"') || crops.includes('TextBox("Plan.PlannedPlantingDate"'));
    assert.ok(crops.includes('asp-for="Plan.ExpectedHarvestDate"'));
    assert.ok(crops.includes("Only one crop may be actively growing") ||
        crops.includes("only one crop may be actively growing"));
    assert.ok(crops.includes("crop-cycle-@cycle.Id"));
    assert.ok(crops.includes("Transition.RowVersion"));
    assert.ok(crops.includes('data-confirm="Cancel:'));
});
test("Calendar is accessible without discovering an extra hidden schedule", () => {
    assert.ok(crops.includes('sf-crop-calendar ani12d-calendar"'));
    assert.ok(crops.includes('class="crop-agenda-details ani12d-agenda-details" open'));
    assert.ok(crops.includes('asp-fragment="crop-calendar-heading"'));
    assert.ok(crops.includes('id="agenda-date-@day.Date.ToString("yyyy-MM-dd")"'));
    assert.ok(crops.includes("projected, not completed work"));
    assert.ok(crops.includes("No scheduled crop activities this month"));
    assert.ok(style.includes(".ani12d-calendar-month { display: none; }"));
});
test("Editing is explicit and preserves concurrency and safe cancel routes", () => {
    assert.ok(editPlot.includes('asp-for="Input.RowVersion"'));
    assert.ok(editPlan.includes('asp-for="Input.RowVersion"'));
    assert.ok(editPlot.includes("ani12d-form-actions"));
    assert.ok(editPlan.includes("ani12d-form-actions"));
    assert.ok(editPlan.includes('asp-route-cycleId="@Model.Cycle.Id"'));
    assert.ok(editPlan.includes("asp-validation-summary"));
    assert.ok(editPlot.includes("asp-validation-summary"));
});
