"use strict";

// Real Chromium tests against the ASP.NET Core MVC site, without production credentials
// or database writes. Authenticated business flows are covered separately by test-host tests.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;

const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const screenshots = path.join(__dirname, "screenshots");
fs.mkdirSync(screenshots, { recursive: true });

async function checkPage(page, url, expectedTitle) {
    const response = await page.goto(new URL(url, base).href, { waitUntil: "networkidle" });
    assert.equal(response?.status(), 200, `${url} should load without a server error`);
    assert.equal(await page.locator("main#main-content").count(), 1);
    assert.equal(await page.locator("h1").first().innerText(), expectedTitle);
    const measure = await page.evaluate(() => ({
        documentWidth: document.documentElement.scrollWidth,
        viewportWidth: window.innerWidth,
        focusablesWithoutName: [...document.querySelectorAll("main button, main a[href]")]
            .filter(e => {
                const box = e.getBoundingClientRect();
                return box.width > 0 && box.height > 0 &&
                    !(e.textContent?.trim() || e.getAttribute("aria-label") || e.getAttribute("title"));
            }).length
    }));
    assert.ok(measure.documentWidth <= measure.viewportWidth + 2,
        `${url}: page is horizontally overflowing (${measure.documentWidth} > ${measure.viewportWidth})`);
    assert.equal(measure.focusablesWithoutName, 0,
        `${url}: an interactive element is missing a readable label`);
}

async function checkAccessibility(page, screen, viewport) {
    const audit = await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
        .analyze();
    const blocking = audit.violations.filter(v =>
        v.impact === "serious" || v.impact === "critical");
    assert.equal(blocking.length, 0,
        screen + " " + viewport + " accessibility violations: " +
        blocking.map(v => v.id + ": " + v.description +
            " [" + v.nodes.map(node => node.target.join(" ")).join(", ") + "]").join("; "));
}

(async () => {
    const browser = await chromium.launch({ headless: true });
    const results = [];
    try {
        for (const [label, width, height] of [
            ["small-phone", 320, 720],
            ["phone", 375, 812],
            ["tablet", 820, 1100],
            ["desktop", 1280, 900]
        ]) {
            const context = await browser.newContext({ viewport: { width, height } });
            const page = await context.newPage();
            const errors = [];
            page.on("pageerror", error => errors.push(error.message));
            try {
                await checkPage(page, "/", "Every farm, crop, and harvest in one place.");
                if (label === "phone") {
                    const photos = page.locator('img[src*="/images/photography/"]');
                    assert.ok(await photos.count() >= 2, "the landing must render actual farm photography");
                    for (const asset of ["fields-hero.webp", "farmers-working.webp", "fields-detail.webp"]) {
                        const response = await page.request.get(
                            new URL("/images/photography/" + asset, base).href);
                        assert.equal(response.status(), 200, asset + " must be self-hosted");
                        assert.match(response.headers()["content-type"] || "", /image\/webp/);
                        assert.ok((await response.body()).length >= 4000, asset + " must not be empty");
                    }
                }
                if (label === "phone" || label === "desktop")
                    await checkAccessibility(page, "landing", label);
                await page.screenshot({ path: path.join(screenshots, `landing-${label}.png`), fullPage: true });
                await checkPage(page, "/Account/Login", "Log in to AniGo");
                if (label === "phone" || label === "desktop")
                    await checkAccessibility(page, "login", label);
                assert.equal(await page.locator('input[name="Email"][type="email"]').count(), 1);
                assert.equal(await page.locator('input[name="Password"][type="password"]').count(), 1);
                assert.equal(await page.locator('input[name="__RequestVerificationToken"]').count(), 1);
                const show = page.locator("[data-password-toggle]").first();
                await show.click();
                assert.equal(await page.locator('input[name="Password"]').getAttribute("type"), "text");
                await show.click();
                assert.equal(await page.locator('input[name="Password"]').getAttribute("type"), "password");

                await checkPage(page, "/Account/Register", "Create your account");
                if (label === "phone" || label === "desktop")
                    await checkAccessibility(page, "registration", label);
                assert.equal(await page.locator('input[name="FullName"]').count(), 1);
                assert.equal(await page.locator('input[name="__RequestVerificationToken"]').count(), 1);
                await page.screenshot({ path: path.join(screenshots, `register-${label}.png`), fullPage: true });
                assert.deepEqual(errors, [], `JavaScript errors at ${label}`);
                results.push(`${label}: landing, login, registration, accessible controls, viewport and JS passed`);
            } finally { await context.close(); }
        }

        const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
        const page = await context.newPage();
        await checkPage(page, "/", "Every farm, crop, and harvest in one place.");
        await page.keyboard.press("Tab");
        assert.equal(await page.evaluate(() => document.activeElement?.getAttribute("href")), "#main-content");
        await page.keyboard.press("Enter");
        assert.equal(await page.evaluate(() => document.activeElement?.id), "main-content");

        const toggle = page.locator("[data-color-mode-toggle]").first();
        await toggle.click();
        assert.equal(await page.locator("html").getAttribute("data-color-mode"), "dark");
        await page.goto(new URL("/Account/Login", base).href);
        assert.equal(await page.locator("html").getAttribute("data-color-mode"), "dark",
            "appearance setting must persist between pages");
        await context.close();
        results.push("keyboard skip link and persistent appearance passed");

        const context2 = await browser.newContext();
        const page2 = await context2.newPage();
        const response = await page2.goto(new URL("/Farms", base).href);
        assert.ok(response?.url().includes("/Account/Login"), "private pages must redirect to sign-in");
        await context2.close();
        results.push("private workspace requires login");

        for (const result of results) console.log("PASS", result);
    } catch (error) {
        console.error("FAIL", error.stack || error);
        process.exitCode = 1;
    } finally {
        await browser.close();
    }
})();
