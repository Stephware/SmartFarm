"use strict";

// The HTML is rendered by MVC TestServer using deterministic, signed-in farm
// roles and test records. The browser loads the real app's current CSS/JS.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;

const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const fixtures = path.join(__dirname, "authenticated-fixtures");
const shots = path.join(__dirname, "screenshots", "phase12e");
const names = [
    "12e-tasks-owner", "12e-tasks-worker",
    "12e-inventory-owner", "12e-inventory-worker",
    "12e-stock-owner", "12e-stock-worker",
    "12e-monitoring-owner", "12e-monitoring-worker",
    "12e-harvests-owner", "12e-harvests-worker",
    "12e-weather-saved", "12e-weather-stale", "12e-reports"
];
const routes = name => {
    if (name.includes("tasks")) return "/Tasks?farmId=123&view=all&month=2026-11";
    if (name.includes("inventory")) return "/Inventory?farmId=123";
    if (name.includes("stock")) return "/Inventory/Details?farmId=123&itemId=123";
    if (name.includes("monitoring")) return "/Monitoring?farmId=123&cycleId=123";
    if (name.includes("harvests")) return "/Harvests?farmId=123&cycleId=123";
    if (name.includes("weather")) return "/Weather?farmId=123";
    return "/Reports/Harvests?farmId=123";
};
const widths = [320, 375, 768, 1024, 1440];

(async () => {
    fs.mkdirSync(shots, { recursive: true });
    names.forEach(name => assert.ok(fs.existsSync(path.join(fixtures, name + ".html")), name));
    const browser = await chromium.launch();
    let count = 0;
    try {
        for (const mode of ["light", "dark"]) {
            const context = await browser.newContext({ colorScheme: "light", reducedMotion: "reduce" });
            await context.addInitScript(color => localStorage.setItem("anigo-color-mode", color), mode);
            const page = await context.newPage();
            const errors = [];
            page.on("pageerror", e => errors.push(e.message));
            let selected = "";
            await page.route(/\/(?:Tasks|Monitoring|Harvests|Inventory|Weather|Reports)(?:\/|\?|$)/,
                async route => route.fulfill({
                    status: 200, contentType: "text/html; charset=utf-8",
                    body: fs.readFileSync(path.join(fixtures, selected + ".html"))
                }));
            for (const width of widths) {
                await page.setViewportSize({ width, height: 900 });
                for (const name of names) {
                    selected = name;
                    const response = await page.goto(new URL(routes(name), base).href, { waitUntil: "networkidle" });
                    assert.equal(response?.status(), 200, name);
                    assert.equal(await page.locator("main#main-content").count(), 1, name + " landmark");
                    assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode, name + " theme");
                    await page.evaluate(() => document.fonts.ready);
                    const size = await page.evaluate(() => ({
                        document: document.documentElement.scrollWidth, window: innerWidth
                    }));
                    assert.ok(size.document <= size.window + 2,
                        name + " " + mode + "/" + width + " horizontal overflow " + size.document + " > " + size.window);
                    assert.equal(await page.locator(".ani12e-workspace").count(), 1, name + " scoped workspace");
                    if (name.includes("tasks")) {
                        assert.equal(await page.locator(".ani12e-task-workspace").count(), 1);
                        if (width <= 375) {
                            assert.ok(await page.locator(".ani12e-mobile-agenda").isVisible());
                            assert.equal(await page.locator(".ani12e-task-calendar .reference-calendar").isVisible(), false);
                        }
                        if (name.endsWith("worker")) assert.equal(await page.locator("#add-task").count(), 0);
                        else assert.equal(await page.locator("#add-task").count(), 1);
                    }
                    if (name.includes("inventory")) {
                        assert.equal(await page.locator(".ani12e-inventory-table").count(), 1);
                        assert.ok(await page.getByText("Rice seed").count());
                        if (name.endsWith("worker")) assert.equal(await page.locator("#add-item").count(), 0);
                    }
                    if (name.includes("stock")) {
                        assert.equal(await page.locator("[data-inventory-movement-form]").count(), 1);
                        if (name.endsWith("worker")) {
                            assert.equal(await page.getByRole("option", { name: "Supplies arrived (add stock)" }).count(), 0);
                            assert.equal(await page.getByText("+ Supplies arrived").count(), 0);
                            assert.equal(await page.getByText("± Count correction").count(), 0);
                        }
                    }
                    if (name.includes("monitoring")) {
                        assert.equal(await page.locator(".ani12e-recording-grid").count(), 1);
                        assert.equal(await page.locator(".ani12e-photo-panel").count(), 1);
                        if (width === 375) {
                            await page.locator('a[href="#record-activity"]').first().click();
                            assert.equal(await page.locator("dialog[open] #record-activity").count(), 1,
                                "Recording form must open from the fieldwork shortcut");
                            await page.locator("dialog[open] button[aria-label^='Close']").click();
                        }
                    }
                    if (name.includes("harvests")) {
                        assert.equal(await page.locator("#record-harvest").count(), 1);
                        assert.equal(await page.getByText("Confirmed harvest total").count(), 1);
                    }
                    if (name.includes("weather")) {
                        assert.ok(await page.getByText("Saved forecast, not live conditions.").count());
                        if (name.endsWith("stale"))
                            assert.ok(await page.getByText("Weather data may be out of date.").count());
                    }
                    if (name === "12e-reports") {
                        assert.equal(await page.locator("#report-filters").count(), 1);
                        assert.equal(await page.locator('#from').isVisible(), true);
                        assert.equal(await page.locator('#to').isVisible(), true);
                        assert.equal(await page.locator('a[href*="ExportCsv"]').count(), 1);
                        assert.equal(await page.locator('a[href*="ExportCycleCsv"]').count(), 1);
                        assert.equal(await page.locator('.ani-report-chart').count(), 0);
                        assert.equal(await page.locator('a[href*="FarmFinance"]').count(), 0);
                    }
                    if (width === 375) {
                        const audit = await new AxeBuilder({ page })
                            .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
                        assert.deepEqual(audit.violations.filter(v => ["serious", "critical"].includes(v.impact))
                            .map(v => v.id + ":" + v.nodes.map(n => n.target.join(" ")).join(",")), [],
                            name + " " + mode + ": accessibility");
                    }
                    await page.screenshot({
                        path: path.join(shots, name + "-" + mode + "-" + width + ".png"),
                        fullPage: true, animations: "disabled"
                    });
                    count++;
                }
            }
            assert.deepEqual(errors, [], "Page script errors " + mode);
            await context.close();
        }
    } finally {
        await browser.close();
    }
    assert.equal(count, names.length * widths.length * 2);
    console.log("PASS " + count + " Phase 12E authenticated screenshots.");
})().catch(error => { console.error(error.stack || error); process.exitCode = 1; });
