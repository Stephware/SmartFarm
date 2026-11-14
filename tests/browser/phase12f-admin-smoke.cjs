"use strict";
// Test-only pages: real Razor admin markup rendered with synthetic data by xUnit.
// Routing only replaces /admin requests in the local Chromium test context.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;

const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const fixtureDir = path.join(__dirname, "admin-fixtures");
const outputDir = path.join(__dirname, "screenshots", "phase12f");
const pages = [
    ["overview", "Dashboard", "/admin"],
    ["users", "Users", "/admin/users"],
    ["farms", "Farms", "/admin/farms"],
    ["user-detail", "Users", "/admin/users/123"],
    ["farm-detail", "Farms", "/admin/farms/123"],
    ["monitoring", "Monitoring", "/admin/monitoring"],
    ["sync", "Monitoring", "/admin/monitoring/sync"],
    ["audit", "Audit", "/admin/audit"],
    ["announcements", "Announcements", "/admin/announcements"],
    ["support", "Support", "/admin/support"],
    ["support-detail", "Support", "/admin/support/123"],
    ["security", "Security", "/admin/security/setup"]
];
const widths = [320, 375, 768, 1024, 1440];

(async () => {
    fs.mkdirSync(outputDir, { recursive: true });
    for (const [name] of pages) assert.ok(fs.existsSync(path.join(fixtureDir, name + ".html")), name);
    const browser = await chromium.launch({ headless: true });
    let total = 0;
    try {
        for (const mode of ["light", "dark"]) {
            const context = await browser.newContext({ colorScheme: "light", reducedMotion: "reduce" });
            await context.addInitScript(x => localStorage.setItem("anigo-color-mode", x), mode);
            const page = await context.newPage();
            const errors = [];
            page.on("pageerror", e => errors.push(e.message));
            let selected = "";
            await page.route(/\/admin(?:\/|\?|$)/i, async route => route.fulfill({
                status: 200, contentType: "text/html; charset=utf-8",
                body: fs.readFileSync(path.join(fixtureDir, selected + ".html"))
            }));
            for (const width of widths) {
                await page.setViewportSize({ width, height: 900 });
                for (const [name, section, url] of pages) {
                    selected = name;
                    const response = await page.goto(new URL(url, base).href, { waitUntil: "networkidle" });
                    assert.equal(response?.status(), 200, name + " served");
                    await page.evaluate(() => document.fonts.ready);
                    assert.equal(await page.locator("main#main-content").count(), 1, name + " main");
                    assert.equal(await page.locator(".ani12f-admin-shell").count(), 1, name + " scoped");
                    assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode, name + " theme");
                    const layout = await page.evaluate(() => ({
                        screen: innerWidth,
                        document: document.documentElement.scrollWidth
                    }));
                    assert.ok(layout.document <= layout.screen + 2,
                        name + "/" + mode + "/" + width + ": page overflow " + layout.document + " > " + layout.screen);
                    const nav = page.locator('.admin-shell .sidebar-links a[aria-current="page"]');
                    assert.equal(await nav.count(), 1, name + ": only one current sidebar route");
                    assert.ok((await nav.getAttribute("href") || "").toLowerCase().includes(
                        section === "Dashboard" ? "/admin" : section.toLowerCase()), name + " sidebar route");
                    if (width <= 768) {
                        const menu = page.locator(".admin-mobile-nav summary");
                        assert.ok(await menu.isVisible(), name + ": navigation accessible on phones/tablets");
                        await menu.click();
                        const mobile = page.locator('.admin-mobile-nav nav a[aria-current="page"]');
                        assert.equal(await mobile.count(), 1, name + ": current mobile route");
                        assert.ok(await mobile.isVisible(), name + ": current mobile item visible");
                        await menu.click();
                    }
                    if (name === "users" || name === "farms" || name === "audit" || name === "sync") {
                        assert.equal(await page.locator("form.admin-filters").count(), 1);
                        for (const control of await page.locator("form.admin-filters button").all()) {
                            const h = await control.evaluate(e => e.getBoundingClientRect().height);
                            assert.ok(h >= 44, name + ": filter button touch target " + h);
                        }
                    }
                    assert.equal(await page.locator('a[href^="/admin/reports"]').count(), 0);
                    if (name === "support-detail") {
                        assert.equal(await page.getByRole("heading", { name: "Read-only history", exact: true }).count(), 1);
                        assert.equal(await page.locator('form[action$="/reply"],form[action$="/status"]').count(), 0);
                        assert.equal(await page.getByText("Retained internal note", {exact: true}).count(), 1);
                    }
                    if (name === "announcements") {
                        assert.equal(await page.getByRole("heading", { name: "Announcement history", exact: true }).count(), 1);
                        assert.equal(await page.getByRole("link", { name: "New announcement", exact: true }).count(), 0);
                    }
                    if (name === "security")
                        assert.ok(await page.locator('.admin-security-setup input[type="password"]').count());
                    if (name === "farm-detail" || name === "user-detail") {
                        assert.equal(await page.locator('form[data-confirm]').count(), 1);
                        assert.equal(await page.locator('input[name="currentPassword"]').count(), 1);
                    }
                    if (width === 375 || width === 1440) {
                        const audit = await new AxeBuilder({ page })
                            .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
                        const errors = audit.violations.filter(v => ["serious", "critical"].includes(v.impact));
                        assert.deepEqual(errors.map(v => v.id + ":" + v.nodes.map(n => n.target.join(" ")).join(",")),
                            [], name + "/" + mode + "/" + width + ": accessibility violations");
                    }
                    await page.screenshot({
                        path: path.join(outputDir, name + "-" + mode + "-" + width + ".png"),
                        animations: "disabled", fullPage: true
                    });
                    total++;
                }
            }
            assert.deepEqual(errors, [], mode + " script errors");
            await context.close();
        }
    } finally {
        await browser.close();
    }
    assert.equal(total, pages.length * widths.length * 2);
    console.log("PASS " + total + " Super Admin screen/theme/width combinations.");
})().catch(error => { console.error(error.stack || error); process.exitCode = 1; });
