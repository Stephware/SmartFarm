"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;

const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const shots = path.join(__dirname, "screenshots", "phase7");
const widths = [320, 375, 768, 1024, 1440];
fs.mkdirSync(shots, { recursive: true });

(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    for (const mode of ["light", "dark"]) {
      const context = await browser.newContext({ reducedMotion: "reduce", locale: "en-US" });
      await context.addInitScript(v => localStorage.setItem("anigo-color-mode", v), mode);
      const page = await context.newPage();
      for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        const res = await page.goto(new URL("/", base).href, { waitUntil: "networkidle" });
        assert.equal(res?.status(), 200);
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
        assert.equal(snapshot.card, mode === "dark" ? "#202b26" : "#ffffff");
        assert.equal(snapshot.action, mode === "dark" ? "#397a58" : "");
        assert.equal(snapshot.button, mode === "dark" ? "rgb(57, 122, 88)" : "rgb(36, 140, 82)");
        const a11y = await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
        assert.deepEqual(a11y.violations.filter(v => ["serious", "critical"].includes(v.impact))
          .map(v => v.id + ":" + v.nodes.length), [], "WCAG serious/critical " + mode + "/" + width);
        await page.screenshot({ path: path.join(shots, "landing-" + mode + "-" + width + ".png"),
          fullPage: true, animations: "disabled" });
      }
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
      }
      await context.close();
    }
    console.log("Phase 7 dark-mode palette, responsive pages, persisted mode and accessibility passed");
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
