"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const web = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = f => fs.readFileSync(path.join(web, f), "utf8");
const farm = read("Views/Dashboard/Index.cshtml");
const admin = read("Areas/Admin/Views/Dashboard/Index.cshtml");
const rules = read("Presentation/DashboardRoleSummary.cs");
const css = read("Styles/anigo-dashboard-12c.css");

test("Phase 12C uses one scoped design and real farm-derived dashboard summaries", () => {
    assert.match(read("Styles/anigo-tailwind.css"), /@import "\.\/anigo-dashboard-12c\.css"/);
    assert.match(farm, /DashboardRoleSummary\.Build\(Model\.FarmRole, Model\.Tasks, Model\.Cycles, Model\.ActorId, farm\.FarmToday\)/);
    assert.match(rules, /AssignedToUserId == actorId/);
    assert.match(rules, /Status == 0/);
    assert.match(rules, /CropCycleStatus\.Active/);
    assert.match(farm, /ani12c-role-owner/);
    assert.match(farm, /ani12c-role-manager/);
    assert.match(farm, /ani12c-role-worker/);
    assert.match(css, /var\(--ui-surface\)/);
    assert.match(css, /max-width:360px/);
});
test("Phase 12C always chooses a crop explicitly when multiple are active", () => {
    assert.match(farm, /activeCycle = activeCycles\.Count == 1 \? activeCycles\[0\] : null/);
    assert.match(farm, /activeCycles\.Count > 1/);
    assert.match(farm, /id="ani12c-crop-picker"/);
    assert.match(farm, /asp-route-cycleId="@crop\.Id"/);
    assert.match(farm, /Choose a plot and crop/);
    assert.doesNotMatch(farm, /FirstOrDefault\(c => c\.Status == CropCycleStatus\.Active\)/);
});
test("Worker task queue excludes other workers and owner-only actions remain isolated", () => {
    assert.match(rules, /tasks\.Where\(t => t\.AssignedToUserId == actorId\)/);
    assert.match(farm, /VisibleOpenTaskIds\.Contains\(a\.TargetId\)/);
    assert.match(farm, /Your assigned work/);
    assert.match(farm, /Team requests needing a decision/);
    assert.match(farm, /@if \(role\.IsOwner\)/);
    assert.match(farm, /@if \(!role\.IsFieldWorker\)/);
    assert.match(farm, /asp-route-view="@\(role\.IsFieldWorker \? "mine" : "today"\)"/);
});
test("Super Admin is still authorized and displays verified job counts only", () => {
    const controller = read("Areas/Admin/Controllers/DashboardController.cs");
    const diagnostic = read("Services/Admin/AdminDiagnosticsService.cs");
    assert.match(controller, /Authorize\(Policy = SuperAdminSecurity\.Policy\)/);
    assert.match(controller, /WorkerSummaryAsync\(ct\)/);
    assert.match(diagnostic, /WorkerStatusAsync\(DateTime\.UtcNow, ct\)/);
    assert.match(admin, /workerAvailable \? Model\.Worker!\.FailedJobsLast7Days\.ToString\(\) : "—"/);
    assert.match(admin, /Unresolved support/);
    assert.match(admin, /Failed jobs · 7 days/);
    assert.match(admin, /Administrative activity/);
    assert.match(admin, /Recently registered farms/);
    assert.match(admin, /READ-ONLY STATUS/);
    assert.doesNotMatch(admin, /asp-action="(Delete|Impersonate|RetryJobs|TransferOwnership)"/);
});
