"use strict";

// Real Razor fixtures and browser history, without privileged cookies or database writes.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const fixtures = path.join(__dirname, "authenticated-fixtures");
const output = path.join(__dirname, "screenshots", "back-navigation");
const cases = [
    ["12d-edit-plot", "/Farms/EditPlot", "12d-farm-owner", "/Farms/Details", "Back to plots", ["id"], "#plots-heading"],
    ["12d-edit-plan", "/Crops/EditPlan", "12d-crops-owner", "/Crops", "Back to crops", ["farmId"], "#crop-cycle-"],
    ["phase5-supply-edit-Owner", "/Inventory/Edit", "12e-stock-owner", "/Inventory/Details", "Back to supply", ["farmId", "itemId"], ""]
];
(async () => {
    fs.mkdirSync(output, { recursive: true });
    const browser = await chromium.launch();
    let checked = 0;
    try {
        for (const mode of ["light", "dark"]) for (const width of [390, 768, 1440]) {
            const context = await browser.newContext({ viewport: { width, height: 900 }, reducedMotion: "reduce" });
            await context.addInitScript(value => localStorage.setItem("anigo-color-mode", value), mode);
            const page = await context.newPage();
            for (const [name, editPath, parentName, parentPath, label, ids, fragment] of cases) {
                await page.route("**/*", route => {
                    const url = new URL(route.request().url());
                    const file = url.pathname === editPath ? name : (url.pathname === parentPath || url.pathname.startsWith(parentPath + "/")) ? parentName : null;
                    return file ? route.fulfill({ contentType: "text/html", body: fs.readFileSync(path.join(fixtures, file + ".html")) }) : route.continue();
                });
                await page.goto(new URL(editPath + "?navigation-review", base).href, { waitUntil: "networkidle" });
                const link = page.getByRole("link", { name: label, exact: true });
                await link.waitFor({ state: "visible" });
                const target = new URL(await link.getAttribute("href"), base);
                const form = new URL(await page.locator("main form[data-ani-p5-form]").getAttribute("action"), base);
                assert.equal(target.origin, new URL(base).origin);
                const routeValue = (url, key) => url.searchParams.get(key) || (key === "id" ? url.pathname.split("/").pop() : null);
                assert.equal(target.pathname, ids.includes("id") ? parentPath + "/" + routeValue(form, "id") : parentPath);
                for (const key of ids) {
                    assert.match(routeValue(target, key), /^[\da-f-]{36}$/i);
                    assert.equal(routeValue(target, key), routeValue(form, key));
                }
                assert.ok(target.hash.startsWith(fragment));
                assert.ok(await link.evaluate(el => !!(el.compareDocumentPosition(document.querySelector("main h1")) & Node.DOCUMENT_POSITION_FOLLOWING)));
                assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1));
                await page.screenshot({ path: path.join(output, `${name}-${mode}-${width}.png`), fullPage: true });
                await link.click();
                await page.waitForURL(target.href);
                assert.equal(new URL(page.url()).pathname, target.pathname);
                await page.goBack();
                assert.equal(new URL(page.url()).pathname, editPath);
                await page.goForward();
                assert.equal(new URL(page.url()).pathname, target.pathname);
                await page.unrouteAll({ behavior: "wait" });
                checked++;
            }
            await context.close();
        }
        console.log(`PASS ${checked} contextual parent links and browser Back/Forward flows; IDs, fragments, themes and viewport widths preserved.`);
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
