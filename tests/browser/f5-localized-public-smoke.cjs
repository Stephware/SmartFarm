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
            // Language is now selected in signed-in Account & settings.
            // Use the real cookie-backed endpoint and a CSRF token from the
            // login form to verify persistence without a public dropdown.
            assert.equal(await page.locator(".anigo-language").count(), 0);
            const csrf = await page.locator('input[name="__RequestVerificationToken"]')
                .first().inputValue();
            const selected = await ctx.request.post(new URL("/Language/Select", base).href, {
                form: {
                    language: "fil",
                    returnUrl: "/Account/Login",
                    __RequestVerificationToken: csrf
                }
            });
            assert.equal(selected.status(), 200);
            await page.reload();
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
        console.log("PASS Filipino locale cookie persists across login/registration/recovery without guest language selector on 320px light/dark");
    } finally {
        await browser.close();
    }
})().catch(err => { console.error("FAIL", err.stack || err); process.exitCode = 1; });
