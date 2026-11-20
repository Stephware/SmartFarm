"use strict";
// F5: exercise live MVC locale selection, not synthetic generated HTML fixtures.
const assert = require("node:assert/strict");
const { chromium } = require("playwright");
const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
(async () => {
    const browser = await chromium.launch({ headless: true });
    try {
        for (const mode of ["light", "dark"]) {
            const ctx = await browser.newContext({
                viewport: { width: 320, height: 750 },
                reducedMotion: "reduce"
            });
            await ctx.addInitScript(value => localStorage.setItem("anigo-color-mode", value), mode);
            const page = await ctx.newPage();
            const errors = [];
            page.on("pageerror", e => errors.push(e.message));
            await page.goto(new URL("/Account/Login", base).href);
            assert.equal(await page.locator("html").getAttribute("lang"), "en");
            await page.locator(".anigo-language summary").click();
            await page.locator('.anigo-language-picker select[name="language"]').selectOption("fil");
            await Promise.all([
                page.waitForURL(/\/Account\/Login/),
                page.locator(".anigo-language-picker button[type=submit]").click()
            ]);
            assert.equal(await page.locator("html").getAttribute("lang"), "fil");
            assert.notEqual((await page.locator("h1").innerText()).trim(), "Log in");
            for (const path of ["/Account/Login", "/Account/Register",
                "/Account/ForgotPassword", "/Account/ResetPassword"]) {
                const response = await page.goto(new URL(path, base).href);
                assert.equal(response?.status(), 200, path);
                assert.equal(await page.locator("html").getAttribute("lang"), "fil", path);
                const overflow = await page.evaluate(() =>
                    document.documentElement.scrollWidth > innerWidth + 2);
                assert.equal(overflow, false, path + " Filipino on 320px " + mode);
            }
            assert.notEqual((await page.locator("h1").innerText()).trim(), "Invalid reset link");
            assert.deepEqual(errors, [], mode + " localized browser runtime errors");
            await ctx.close();
        }
        console.log("PASS public MVC Filipino locale persists across login/registration/recovery on 320px light/dark");
    } finally {
        await browser.close();
    }
})().catch(err => { console.error("FAIL", err.stack || err); process.exitCode = 1; });
