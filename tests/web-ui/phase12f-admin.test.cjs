"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = p => fs.readFileSync(path.join(root, p), "utf8");
const layout = read("Areas/Admin/Views/Shared/_Layout.cshtml");
const styles = read("Styles/anigo-admin-12f.css");
const app = read("Styles/anigo-tailwind.css");

test("Super Admin reuses the AniGo design tokens and no second CSS reset", () => {
    assert.ok(app.includes('@import "./anigo-admin-12f.css";'));
    assert.ok(styles.includes("ani12f-admin-shell"));
    for (const token of ["--ui-surface", "--ui-border", "--ui-text", "--ui-primary", "--ui-focus"]) {
        assert.ok(styles.includes(token), token);
    }
    assert.doesNotMatch(styles, /@import|--ui-page:\s*#/);
    assert.match(styles, /max-width:850px/);
    assert.match(styles, /max-width:600px/);
    assert.match(styles, /max-width:360px/);
    assert.match(styles, /prefers-reduced-motion:\s*reduce/);
});
test("Super Admin routes remain visible on desktop and mobile with active state", () => {
    for (const controller of ["Dashboard", "Users", "Farms", "Support", "Monitoring",
        "Audit", "Announcements", "Reports", "Security"]) {
        assert.ok(layout.includes('asp-area="Admin" asp-controller="' + controller + '"'),
            controller + " navigation");
        assert.ok(layout.includes('section == "' + controller + '" ? "page" : null'),
            controller + " active section");
    }
    assert.match(layout, /aria-label="Platform administration"/);
    assert.match(layout, /aria-label="Mobile platform administration"/);
    assert.match(layout, /aria-label="Admin navigation"/);
    assert.match(layout, /id="ani12f-admin-mobile-links"/);
    assert.ok(layout.includes('asp-controller="Dashboard" asp-action="Index" aria-current="@(section == "Dashboard" ? "page" : null)">Admin overview</a>'));
    assert.ok(!layout.includes('class="crumb"><a asp-area="Admin" asp-controller="Dashboard" asp-action="Index" aria-current='));
    assert.match(layout, /href="#main-content"/);
    assert.match(layout, /id="main-content"/);
});
test("Administrative controls preserve auditing, MFA and explicit confirmations", () => {
    const users = read("Areas/Admin/Views/Users/Details.cshtml");
    const farms = read("Areas/Admin/Views/Farms/Details.cshtml");
    const support = read("Areas/Admin/Views/Support/Details.cshtml");
    const announcements = read("Areas/Admin/Views/Announcements/Editor.cshtml");
    const audit = read("Areas/Admin/Views/Audit/Index.cshtml");
    const monitoring = read("Areas/Admin/Views/Monitoring/Index.cshtml");
    for (const page of [users, farms]) {
        assert.match(page, /data-confirm=/);
        assert.match(page, /name="currentPassword"/);
        assert.match(page, /name="reason"/);
        assert.match(page, /asp-controller="Audit"/);
    }
    assert.match(support, /admin-support-message-internal/);
    assert.match(support, /Internal note \(not visible to requester\)/);
    assert.match(announcements, /asp-validation-summary/);
    assert.match(audit, /This history is read-only/);
    assert.match(monitoring, /Failed jobs are not retried here/);
    assert.match(styles, /min-height:\s*44px/);
});
test("Directory filters and pagination remain semantic and readable", () => {
    for (const p of ["Users/Index.cshtml", "Farms/Index.cshtml",
        "Support/Index.cshtml", "Audit/Index.cshtml",
        "Monitoring/Sync.cshtml", "Announcements/Index.cshtml"]) {
        const html = read("Areas/Admin/Views/" + p);
        assert.match(html, /class="admin-filters/);
        assert.match(html, /class="admin-record-list"/);
        assert.match(html, /aria-label="[^"]*pages?"/i);
        assert.match(html, /asp-route-page=/);
        assert.match(html, /<label for=/);
    }
    const reports = read("Areas/Admin/Views/Reports/Index.cshtml");
    assert.match(reports, /role="region" tabindex="0" aria-label="Daily platform report data"/);
    assert.match(reports, /scope="col"/);
    assert.match(reports, /Export CSV/);
    assert.match(styles, /overflow-x:\s*auto/);
});
