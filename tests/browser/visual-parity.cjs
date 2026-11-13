"use strict";

// Intentional redesigns update reviewed goldens, never compare against a discarded UI.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const { chromium } = require("playwright");
const { PNG } = require("pngjs");
const pixelmatch = require("pixelmatch");
const root = path.resolve(__dirname, "../..");
const url = process.env.ANIGO_CANDIDATE_URL || "http://127.0.0.1:5082";
const directory = path.join(__dirname, "baselines");
const output = path.join(__dirname, "parity-output");
const update = process.env.ANIGO_UPDATE_VISUAL_BASELINES === "1";
const widths = [320, 375, 768, 1024, 1440];
const pages = { landing: "/", login: "/Account/Login", registration: "/Account/Register" };
const inputs = ["wwwroot/css/anigo-tailwind.css", "Views/Shared/_Layout.cshtml",
    "Views/Home/Index.cshtml", "Views/Account/Login.cshtml", "Views/Account/Register.cshtml"];
const hashes = Object.fromEntries(inputs.map(file => [file, crypto.createHash("sha256")
    .update(fs.readFileSync(path.join(root, "src/SmartFarm.Web", file))).digest("hex")]));
fs.mkdirSync(output, { recursive: true });
if (update) fs.mkdirSync(directory, { recursive: true });
else {
    const manifest = JSON.parse(fs.readFileSync(path.join(directory, "review.json")));
    assert.equal(manifest.reviewed, true, "Visual baselines must be reviewed before accepting them.");
    assert.deepEqual(manifest.sourceHashes, hashes,
        "Public presentation changed. Capture candidate screens, review all sizes/themes, then explicitly update goldens.");
}

(async () => {
    const browser = await chromium.launch({ headless: true,
        ...(process.env.ANIGO_BROWSER_CHANNEL ? { channel: process.env.ANIGO_BROWSER_CHANNEL } : {}) });
    const findings = [];
    let captured = 0;
    try {
        for (const mode of ["light", "dark"]) {
            const context = await browser.newContext({ reducedMotion: "reduce", deviceScaleFactor: 1,
                locale: "en-US", colorScheme: "light" });
            await context.addInitScript(mode => localStorage.setItem("anigo-color-mode", mode), mode);
            const page = await context.newPage();
            for (const width of widths) {
                await page.setViewportSize({ width, height: 900 });
                for (const [name, route] of Object.entries(pages)) {
                    const key = `${name}-${mode}-${width}`;
                    const response = await page.goto(new URL(route, url).href, { waitUntil: "networkidle" });
                    assert.equal(response?.status(), 200, key + ": public route");
                    await page.evaluate(async () => {
                        await document.fonts.ready;
                        await Promise.all([...document.images].map(image => image.decode().catch(() => {})));
                    });
                    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth),
                        false, key + ": no document overflow");
                    assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode);
                    const data = await page.screenshot({ fullPage: true, animations: "disabled" });
                    fs.writeFileSync(path.join(output, key + "-candidate.png"), data);
                    captured++;
                    if (update) {
                        fs.writeFileSync(path.join(directory, key + ".png"), data);
                        continue;
                    }
                    const expected = PNG.sync.read(fs.readFileSync(path.join(directory, key + ".png")));
                    const actual = PNG.sync.read(data);
                    if (actual.width !== expected.width || actual.height !== expected.height) {
                        findings.push(key + ": page dimensions changed");
                        continue;
                    }
                    const difference = new PNG({ width: actual.width, height: actual.height });
                    const changed = pixelmatch(expected.data, actual.data, difference.data,
                        actual.width, actual.height, { threshold: 0.15, includeAA: false });
                    const ratio = changed / (actual.width * actual.height);
                    if (changed) fs.writeFileSync(path.join(output, key + "-diff.png"), PNG.sync.write(difference));
                    console.log("BASELINE", key, (ratio * 100).toFixed(3) + "% changed");
                    // Allow rasterizer differences; composition/content changes remain failures.
                    if (ratio > 0.015) findings.push(key + ": " + (ratio * 100).toFixed(3) + "% changed");
                }
            }
            await context.close();
        }
        assert.equal(captured, 30);
        if (update) {
            fs.writeFileSync(path.join(directory, "review.json"), JSON.stringify({
                reviewed: false, sourceHashes: hashes, widths, modes: ["light", "dark"],
                pages, note: "Review the generated candidate images before changing reviewed to true."
            }, null, 2) + "\n");
            console.log("Captured 30 candidates. Visual review is required; review.json remains unapproved.");
        } else {
            assert.deepEqual(findings, [], "Reviewed visual baseline regressions: " + findings.join("; "));
            console.log("PASS 30 reviewed light/dark visual baselines");
        }
    } finally { await browser.close(); }
})().catch(error => { console.error(error.stack || error); process.exitCode = 1; });
