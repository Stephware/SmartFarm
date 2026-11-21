"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const web = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = p => fs.readFileSync(path.join(web, p), "utf8");
test("Phase 2 page layout rules remain scoped to the authenticated workspace", () => {
  const entry = read("Styles/anigo-tailwind.css");
  const src = read("Styles/anigo-web-phase2.css");
  const bundle = read("wwwroot/css/anigo-tailwind.css");
  assert.match(entry, /@import "\.\/anigo-web-phase2\.css"/);
  assert.match(src, /body\.is-authenticated \.ani-p2-page/);
  assert.match(bundle, /ani-p2-report-tools/);
  for (const page of ["Farms/Index", "Dashboard/Index", "Crops/Index",
    "Inventory/Index", "Reports/Harvests", "Farms/Details",
    "Notifications/Index", "Home/Help", "Account/Profile"]) {
    assert.match(read("Views/" + page + ".cshtml"), /ani-p2-page/, page);
  }
  assert.match(read("Areas/Admin/Views/Dashboard/Index.cshtml"), /ani-p2-admin/);
});
test("Harvest results appear ahead of optional date filters without removing exports", () => {
  const report = read("Views/Reports/Harvests.cshtml");
  assert.ok(report.indexOf('id="harvest-unit-totals-heading"') <
    report.indexOf('id="report-filters"'));
  assert.ok(report.indexOf('id="report-history-heading"') <
    report.indexOf('id="report-filters"'));
  assert.match(report, /<details class="ani-p2-report-tools"/);
  for (const endpoint of ["ExportCsv", "ExportCycleCsv"])
    assert.ok(report.includes('asp-action="' + endpoint + '"'));
  assert.match(report, /name="from"/);
  assert.match(report, /name="to"/);
});
test("Farm and account actions remain available", () => {
  const farm = read("Views/Farms/Details.cshtml");
  assert.match(farm, /href="#add-plot"/);
  assert.match(farm, /asp-action="CreatePlot"/);
  assert.match(farm, /partial name="_AniGuide"/);
  assert.match(read("Views/Shared/_AniGuide.cshtml"), /data-ani-replay/);
  assert.match(farm, /join-requests-heading/);
  const notes = read("Views/Notifications/Index.cshtml");
  assert.match(notes, /asp-action="Read"/);
  assert.match(notes, /name="returnView"/);
  const help = read("Views/Home/Help.cshtml");
  assert.match(help, /Needs review/);
  const profile = read("Views/Account/Profile.cshtml");
  assert.match(profile, /data-color-mode-toggle/);
  assert.match(profile, /Your account information \(read-only\)/);
});
