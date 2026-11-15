"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;
const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const fixtures = path.join(__dirname, "authenticated-fixtures");
const shots = process.env.SMARTFARM_PHASE4_SHOTS || path.join(__dirname, "screenshots", "phase4");
(async () => {
    fs.mkdirSync(shots, { recursive: true });
    const browser = await chromium.launch();
    let count = 0;
    try {
        for (const mode of ["light", "dark"]) {
            const context = await browser.newContext({ reducedMotion: "reduce" });
            await context.addInitScript(mode => localStorage.setItem("anigo-color-mode", mode), mode);
            const page = await context.newPage();
            const errors = [];
            page.on("pageerror", error => errors.push(error.message));
            let selected;
            await page.route(/\/(?:Inventory|Notifications)(?:\?|$)/, route => route.fulfill({
                contentType: "text/html", body: fs.readFileSync(path.join(fixtures, selected + ".html"), "utf8")
            }));
            for (const width of [320, 390, 768, 1440])
            for (const role of ["Owner", "Manager", "FieldWorker"])
            for (const state of ["empty", "populated", "large", "filtered"])
            for (const area of ["inventory", "notifications"]) {
                selected = `phase4-${area}-${role}-${state}`;
                await page.setViewportSize({ width, height: 900 });
                await page.goto(new URL(area === "inventory" ? "/Inventory?fixture" : "/Notifications?fixture", base).href,
                    { waitUntil: "networkidle" });
                await page.evaluate(() => document.fonts.ready);
                assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode);
                assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 2), selected + " overflow");
                if (area === "inventory") {
                    assert.equal(await page.locator("#add-item").count(), role === "FieldWorker" ? 0 : 1);
                    if (state === "empty") assert.equal(await page.locator("#inventory-search").count(), 0);
                    else {
                        const searchHeight = await page.locator("#inventory-search").evaluate(el => el.getBoundingClientRect().height);
                        assert.ok(searchHeight >= 44 && searchHeight <= 60, selected + ": search should be a compact usable field, got " + searchHeight);
                    }
                    assert.equal(await page.locator("tbody tr").count(), state === "large" ? 25 : state === "populated" ? 1 : 0);
                } else {
                    assert.equal(await page.locator(".sf-notifications-list > li").count(), state === "large" ? 20 : state === "populated" ? 1 : 0);
                    if (state === "large") {
                        for (const input of await page.locator('input[name="returnPage"]').all()) assert.equal(await input.inputValue(), "2");
                    }
                }
                if (state === "large") {
                    const pager = page.getByRole("navigation", { name: area === "inventory" ? "Supply pages" : "Notification pages" });
                    assert.equal(await pager.locator('a[rel="prev"]').count(), 1);
                    assert.equal(await pager.locator('a[rel="next"]').count(), 1);
                    assert.match(await pager.getByText("Next", { exact: true }).getAttribute("href"), /page=3/);
                    await pager.getByText("Next", { exact: true }).focus();
                    assert.ok(await pager.getByText("Next", { exact: true }).evaluate(el => getComputedStyle(el).outlineStyle !== "none"));
                }
                for (const empty of await page.locator(".sf-empty-state").all()) {
                    assert.ok(await empty.evaluate(el => ["start", "left"].includes(getComputedStyle(el).textAlign)), selected + ": empty text alignment");
                }
                if (area === "inventory" && state === "empty" && role !== "FieldWorker" && width === 390) {
                    await page.getByRole("link", { name: "Add your first supply", exact: false }).click();
                    assert.equal(await page.locator("dialog[open] #add-item").count(), 1);
                    await page.locator("dialog[open] button[aria-label^='Close']").click();
                }
                await page.screenshot({ path: path.join(shots, `${selected}-${mode}-${width}.png`), fullPage: true });
                if (width === 390) {
                    const scan = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
                    assert.deepEqual(scan.violations.map(v => ({ id: v.id, nodes: v.nodes.map(n => n.target) })), [], selected + " accessibility");
                }
                count++;
            }
            assert.deepEqual(errors, []);
            await context.close();
        }
        console.log(`Phase 4: ${count} real Razor state/theme/role/viewport renders passed, with 48 WCAG scans.`);
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
