"use strict";
/* Phase 6: check actual responsive public rendering without claiming visual sign-off.
   Public PNG goldens remain separately governed by visual-parity.cjs. */
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const AxeBuilder = require("@axe-core/playwright").default;
const base = process.env.SMARTFARM_BROWSER_BASE_URL || "http://127.0.0.1:5082";
const shots = path.join(__dirname, "screenshots", "phase6");
fs.mkdirSync(shots, { recursive: true });
const widths = [320, 375, 768, 1024, 1440];
(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    let count = 0;
    for (const mode of ["light", "dark"]) {
      const context = await browser.newContext({ reducedMotion: "reduce", locale: "en-US" });
      await context.addInitScript(m => localStorage.setItem("anigo-color-mode", m), mode);
      const page = await context.newPage();
      for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        const response = await page.goto(new URL("/", base).href, { waitUntil: "networkidle" });
        assert.equal(response.status(), 200, `${mode}/${width} response`);
        await page.evaluate(async () => {
          await document.fonts.ready;
          for (const img of document.images) img.loading = "eager";
          await Promise.all(Array.from(document.images, img => img.decode()));
        });
        assert.equal(await page.locator("#ani-entry-title").count(), 1);
        assert.equal(await page.locator(".ani6-features .ani6-feature").count(), 4);
        assert.equal(await page.locator(".ani6-steps .ani6-step").count(), 0,
          "Redundant workflow section was deliberately removed in F2");
        assert.equal(await page.locator(".ani6-paths .ani6-path").count(), 2);
        assert.equal(await page.locator(".ani6-hero-image").count(), 1);
        const heroLoaded = await page.locator(".ani6-hero-image").evaluate(img => img.naturalWidth > 0);
        assert.ok(heroLoaded, "hero photo must load");
        const startHref = await page.locator(".ani6-btn-green").getAttribute("href");
        assert.match(startHref, /^\/Account\/Register(?:\?|$)/, "guest CTA uses registration");
        const owner = await page.locator(".ani6-path-owner").getAttribute("href");
        const worker = await page.locator(".ani6-path-worker").getAttribute("href");
        assert.match(owner, /^\/Account\/Register\?/);
        assert.match(worker, /^\/Account\/Register\?/);
        assert.ok(owner.includes("Farms") && worker.includes("Farms"),
          "role paths preserve their existing farm destinations");
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth),
          false, `no horizontal overflow at ${width}/${mode}`);
        assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode);
        const menu = page.locator(".ani6-mobile-menu");
        if (width <= 600) {
          assert.equal(await menu.locator("summary").isVisible(), true,
            "Compact mobile menu remains accessible");
          await menu.locator("summary").click();
          assert.equal(await menu.locator('a[href="/Account/Login"]').isVisible(), true,
            "Mobile menu retains direct login access");
          assert.equal(await menu.locator("button[data-color-mode-toggle]").count(), 1,
            "Mobile menu retains appearance switching");
          await menu.locator("summary").click();
        } else {
          assert.equal(await page.locator(".ani6-account-links .ani6-login").isVisible(), true,
            "Login remains visible on larger viewports");
        }
        for (const id of ["home", "features", "get-started"]) {
          assert.equal(await page.locator(`#${id}`).count(), 1, `active section ${id}`);
        }
        for (const id of ["how-it-works", "about", "contact"]) {
          assert.equal(await page.locator(`#${id}`).count(), 0, `removed section ${id}`);
        }
        const a11y = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
          .analyze();
        const serious = a11y.violations.filter(x => ["serious", "critical"].includes(x.impact));
        assert.deepEqual(serious.map(x => x.id + ":" + x.nodes.length), [],
          `WCAG serious/critical ${mode}/${width}`);
        await page.screenshot({ path: path.join(shots, `landing-${mode}-${width}.png`),
          fullPage: true, animations: "disabled" });
        count++;
      }
      await context.close();
    }
    console.log(`Phase 6 landing: ${count} responsive screenshots checked at 5 sizes and both themes`);
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
