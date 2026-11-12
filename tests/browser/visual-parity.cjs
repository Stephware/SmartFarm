"use strict";

// Compare current candidate screens to the actual base branch on one runner.
// Screenshots are produced with the same Chromium, fonts, viewport and motion settings.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const { PNG } = require("pngjs");
const pixelmatch = require("pixelmatch");

const current = process.env.ANIGO_CANDIDATE_URL || "http://127.0.0.1:5082";
const baseline = process.env.ANIGO_BASELINE_URL || "http://127.0.0.1:5083";
const output = path.join(__dirname, "parity-output");
fs.mkdirSync(output, { recursive: true });

async function imageOf(browser, url, viewport, mode) {
    const context = await browser.newContext({ viewport, reducedMotion: "reduce",
        deviceScaleFactor: 1, locale: "en-US", colorScheme: "light" });
    try {
        const page = await context.newPage();
        const response = await page.goto(url, { waitUntil: "networkidle" });
        assert.equal(response?.status(), 200, "Page should be available: " + url);
        await page.evaluate(mode => document.documentElement.setAttribute("data-color-mode", mode), mode);
        await page.addStyleTag({ content:
            "*,*::before,*::after{animation:none!important;transition:none!important;caret-color:transparent!important}" });
        return PNG.sync.read(await page.screenshot({ fullPage: true, animations: "disabled" }));
    } finally { await context.close(); }
}

(async () => {
    const browser = await chromium.launch({ headless: true });
    const findings = [];
    try {
        for (const [device, viewport] of [
            ["small-phone", { width: 320, height: 720 }],
            ["phone", { width: 375, height: 812 }],
            ["tablet", { width: 820, height: 1100 }],
            ["desktop", { width: 1280, height: 900 }]
        ]) {
            for (const [name, route] of [
                ["landing", "/"],
                ["login", "/Account/Login"],
                ["registration", "/Account/Register"]
            ]) {
                for (const mode of ["light", "dark"]) {
                    const key = [device, name, mode].join("-");
                    const oldImage = await imageOf(browser, new URL(route, baseline).href, viewport, mode);
                    const newImage = await imageOf(browser, new URL(route, current).href, viewport, mode);
                    fs.writeFileSync(path.join(output, key + "-before.png"), PNG.sync.write(oldImage));
                    fs.writeFileSync(path.join(output, key + "-after.png"), PNG.sync.write(newImage));
                    if (oldImage.width !== newImage.width || oldImage.height !== newImage.height) {
                        findings.push(key + ": dimensions differ (" +
                            oldImage.width + "x" + oldImage.height + " vs " +
                            newImage.width + "x" + newImage.height + ")");
                        continue;
                    }
                    const difference = new PNG({ width: oldImage.width, height: oldImage.height });
                    const count = pixelmatch(oldImage.data, newImage.data, difference.data,
                        oldImage.width, oldImage.height, { threshold: 0.1, includeAA: false });
                    const ratio = count / (oldImage.width * oldImage.height);
                    if (count > 0)
                        fs.writeFileSync(path.join(output, key + "-diff.png"), PNG.sync.write(difference));
                    console.log("PARITY", key, (ratio * 100).toFixed(4) + "% changed");
                    // Strict tolerance for antialiasing differences on identical Ubuntu images.
                    if (ratio > 0.001)
                        findings.push(key + ": visual change " + (ratio * 100).toFixed(3) + "%");
                }
            }
        }
        assert.deepEqual(findings, [],
            "Unintended visual changes: " + findings.join("; "));
        console.log("PASS 24 paired light/dark baseline comparisons");
    } finally { await browser.close(); }
})().catch(error => {
    console.error(error.stack || error);
    process.exitCode = 1;
});
