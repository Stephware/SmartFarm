"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;

const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const shots = path.join(__dirname, "screenshots", "phase7");
const widths = [320, 375, 768, 1024, 1440];
const luminance = color => color.match(/\d+(?:\.\d+)?/g).slice(0, 3).map(Number)
  .map(v => v / 255).map(v => v <= .04045 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4)
  .reduce((sum, v, i) => sum + v * [.2126, .7152, .0722][i], 0);
fs.mkdirSync(shots, { recursive: true });

(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    for (const mode of ["light", "dark"]) {
      const context = await browser.newContext({ reducedMotion: "reduce", locale: "en-US" });
      await context.addInitScript(v => {
        if (!localStorage.getItem("anigo-color-mode")) localStorage.setItem("anigo-color-mode", v);
      }, mode);
      const page = await context.newPage();
      for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        const res = await page.goto(new URL("/", base).href, { waitUntil: "networkidle" });
        assert.equal(res?.status(), 200);
        await page.evaluate(async () => {
          await document.fonts.ready;
          for (const image of document.images) image.loading = "eager";
          await Promise.all([...document.images].map(image => image.decode()));
        });
        const snapshot = await page.evaluate(() => {
          const root = getComputedStyle(document.documentElement);
          return {
            mode: document.documentElement.getAttribute("data-color-mode"),
            page: root.getPropertyValue("--ui-page").trim().toLowerCase(),
            card: root.getPropertyValue("--ui-surface").trim().toLowerCase(),
            action: root.getPropertyValue("--ui-action").trim().toLowerCase(),
            button: getComputedStyle(document.querySelector(".ani6-btn-green")).backgroundColor,
            overflows: document.documentElement.scrollWidth > window.innerWidth
          };
        });
        assert.equal(snapshot.mode, mode, "mode persisted");
        assert.equal(snapshot.overflows, false, "no overflow at " + width + "/" + mode);
        assert.equal(snapshot.page, mode === "dark" ? "#121917" : "#f5f7f5");
        assert.equal(snapshot.card, mode === "dark" ? "#202b26" : "#fff");
        assert.equal(snapshot.action, mode === "dark" ? "#397a58" : "");
        assert.equal(snapshot.button, mode === "dark" ? "rgb(57, 122, 88)" : "rgb(33, 131, 78)");
        const colors = await page.locator(".ani6-btn-green").evaluate(element => {
          const s = getComputedStyle(element); return [s.color, s.backgroundColor];
        });
        const levels = colors.map(luminance).sort((a, b) => b - a);
        assert.ok((levels[0] + .05) / (levels[1] + .05) >= 4.5, "Opaque CTA text contrast");
        const a11y = await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
        assert.deepEqual(a11y.violations
          .map(v => v.id + ":" + v.nodes.length), [], "WCAG A/AA " + mode + "/" + width);
        await page.screenshot({ path: path.join(shots, "landing-" + mode + "-" + width + ".png"),
          fullPage: true, animations: "disabled" });
        const enlarged = await page.addStyleTag({ content: "html {font-size:200% !important}" });
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false,
          "200% text reflows at " + width + "/" + mode);
        await enlarged.evaluate(element => element.remove());
        for (const route of ["/Account/Login", "/Account/Register"]) {
          const res = await page.goto(new URL(route, base).href, { waitUntil: "networkidle" });
          assert.equal(res?.status(), 200, route);
          assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode);
          const colors = await page.evaluate(() => ({
            page: getComputedStyle(document.documentElement).getPropertyValue("--ui-page").trim().toLowerCase(),
            overflows: document.documentElement.scrollWidth > innerWidth
          }));
          assert.equal(colors.page, mode === "dark" ? "#121917" : "#f5f7f5");
          assert.equal(colors.overflows, false);
          const scan = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
          assert.deepEqual(scan.violations.map(v => v.id), [], route + " A/AA " + width + "/" + mode);
          if (route === "/Account/Login") {
            await page.locator("#Email").fill("theme-check@example.test");
            await page.locator("#Password").fill("ThemeDraft123!");
            await page.locator("[data-color-mode-toggle]:visible").first().click();
            const other = mode === "dark" ? "light" : "dark";
            assert.equal(await page.locator("html").getAttribute("data-color-mode"), other);
            assert.equal(await page.locator("#Email").inputValue(), "theme-check@example.test");
            assert.equal(await page.locator("#Password").inputValue(), "ThemeDraft123!");
            await page.reload({ waitUntil: "networkidle" });
            assert.equal(await page.locator("html").getAttribute("data-color-mode"), other, "actual saved choice survives reload");
            await page.locator("[data-color-mode-toggle]:visible").first().click();
            assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode);
          }
        }
      }
      await context.close();
    }
    console.log("Phase 7 dark-mode palette, responsive pages, persisted mode and accessibility passed");
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
