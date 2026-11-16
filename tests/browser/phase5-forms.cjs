"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;
const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5083";
const fixtures = path.join(__dirname, "authenticated-fixtures");
const shots = process.env.SMARTFARM_PHASE5_SHOTS || path.join(__dirname, "screenshots", "phase5");
const cases = [
    ["phase5-supply-edit-Owner", "/Inventory/Edit"],
    ["phase5-supply-edit-Manager", "/Inventory/Edit"],
    ["farm-create", "/Farms/Create"], ["12d-edit-plan", "/Crops/EditPlan"],
    ["12d-edit-plot", "/Farms/EditPlot"], ["12e-tasks-owner", "/Tasks"],
    ["phase5-tasks-Manager", "/Tasks"], ["12e-tasks-worker", "/Tasks"]
];
(async () => {
    fs.mkdirSync(shots, { recursive: true });
    const browser = await chromium.launch();
    let count = 0;
    try {
        for (const mode of ["light", "dark"]) {
            const context = await browser.newContext({ reducedMotion: "reduce" });
            await context.addInitScript(value => localStorage.setItem("anigo-color-mode", value), mode);
            const page = await context.newPage();
            const errors = [];
            page.on("pageerror", error => errors.push(error.message));
            let selected;
            const posts = [];
            await page.route(/\/(?:Inventory|Farms|Crops|Tasks)(?:\/|\?|$)/, route => {
                if (route.request().method() === "POST") posts.push(new URLSearchParams(route.request().postData()));
                return route.fulfill({ contentType: "text/html", body: fs.readFileSync(path.join(fixtures, selected + ".html")) });
            });
            for (const width of [320, 390, 768, 1440]) for (const [name, url] of cases) {
                selected = name;
                await page.setViewportSize({ width, height: 900 });
                await page.goto(new URL(url + "?fixture", base).href, { waitUntil: "networkidle" });
                await page.evaluate(() => document.fonts.ready);
                assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode);
                assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 2), name + " overflow");
                const form = page.locator("form[data-ani-p5-form]");
                if (name.endsWith("tasks-worker")) assert.equal(await form.count(), 0);
                else assert.equal(await form.count(), 1);
                if (name === "12d-edit-plan" || name === "12d-edit-plot") {
                    const actions = form.locator(".ani12d-form-actions");
                    const save = await actions.locator("button").boundingBox();
                    const cancel = await actions.locator("a").boundingBox();
                    assert.ok(Math.abs(save.y + save.height / 2 - cancel.y - cancel.height / 2) <= 1,
                        name + ": save and cancel must align without an inherited button margin");
                }
                for (const summary of await form.locator("details.ani-p5-optional > summary").all()) {
                    const size = await summary.evaluate(el => ({ height: el.getBoundingClientRect().height,
                        font: parseFloat(getComputedStyle(el).fontSize) }));
                    assert.ok(size.height >= 44 && size.font >= 14, name + ": optional controls need readable text and a usable target");
                }
                if (name.includes("supply-edit")) {
                    const details = form.locator("details.ani-p5-optional");
                    await details.locator("summary").click();
                    await form.locator("#Input_Description").fill("Keep this supply draft");
                    await form.locator("#Input_ReorderLevel").fill("-1");
                    await details.locator("summary").click();
                    const before = posts.length;
                    await form.getByRole("button", { name: "Save details", exact: true }).click();
                    assert.equal(posts.length, before, "invalid closed settings must never submit");
                    assert.equal(await details.getAttribute("open"), "", "invalid settings must become visible");
                    assert.equal(await form.locator("#Input_ReorderLevel").evaluate(el => el === document.activeElement), true);
                    assert.equal(await form.locator("#Input_Description").inputValue(), "Keep this supply draft");
                    await form.locator("#Input_ReorderLevel").fill("2.500");
                    await Promise.all([page.waitForNavigation({ waitUntil: "networkidle" }),
                        form.getByRole("button", { name: "Save details", exact: true }).evaluate(button => {
                        button.name = "decision"; button.value = "save";
                        button.form.requestSubmit(button); button.form.requestSubmit(button);
                    })]);
                    assert.equal(posts.length, before + 1, "a rapid second submit must be cancelled");
                    assert.equal(posts.at(-1).get("decision"), "save");
                    assert.equal(posts.at(-1).get("Input.ReorderLevel"), "2.500");
                    assert.ok(posts.at(-1).get("Input.RowVersion"));
                    assert.ok(posts.at(-1).get("__RequestVerificationToken"));
                }
                if (name === "farm-create") {
                    await form.locator("#Name").fill("A");
                    assert.equal(await form.locator("#Name").evaluate(el => el.validity.tooShort), true);
                    await form.locator("#Name").fill("AniGo farm");
                    await form.locator("#LocationText").fill("B");
                    assert.equal(await form.locator("#LocationText").evaluate(el => el.validity.tooShort), true);
                    await form.locator("#LocationText").fill("Bulacan");
                }
                if (name === "12d-edit-plan") {
                    await form.locator("[data-ani-p5-start-date]").fill("2026-11-20");
                    await form.locator("[data-ani-p5-end-date]").fill("2026-11-19");
                    assert.equal(await form.locator("[data-ani-p5-end-date]").evaluate(el => el.checkValidity()), false);
                    assert.equal(await form.locator("[data-ani-p5-date-error]").isVisible(), true);
                    await form.locator("[data-ani-p5-end-date]").fill("2026-11-21");
                    assert.equal(await form.locator("[data-ani-p5-date-error]").isVisible(), false);
                }
                if (name.includes("tasks-") && !name.endsWith("worker")) {
                    await page.getByRole("link", { name: "Add a task", exact: true }).click();
                    assert.equal(await page.locator("dialog[open] #add-task").count(), 1);
                }
                await page.screenshot({ path: path.join(shots, `${name}-${mode}-${width}.png`), fullPage: true });
                if (width === 390) {
                    const scan = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
                    assert.deepEqual(scan.violations.map(v => ({ id: v.id, targets: v.nodes.map(n => n.target) })), [], name + " accessibility");
                }
                count++;
            }
            assert.deepEqual(errors, []);
            await context.close();
        }
        const noJs = await browser.newContext({ javaScriptEnabled: false });
        const page = await noJs.newPage();
        await page.route("**/Inventory/Edit*", route => route.fulfill({ contentType: "text/html",
            body: fs.readFileSync(path.join(fixtures, "phase5-supply-edit-Owner.html")) }));
        await page.goto(new URL("/Inventory/Edit?fixture", base).href);
        await page.locator("details.ani-p5-optional summary").click();
        assert.equal(await page.locator("#Input_Description").isVisible(), true);
        assert.equal(await page.getByRole("button", { name: "Save details" }).isEnabled(), true);
        await noJs.close();
        console.log(`Phase 5: ${count} real Razor role/theme/viewport renders, 16 WCAG scans, draft/validation/duplicate-submit and no-JS checks passed.`);
    } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
