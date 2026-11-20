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
const inputs = ["wwwroot/css/anigo-tailwind.css",
    "Views/Shared/_Layout.cshtml", "Views/Home/Index.cshtml",
    "Views/Account/Login.cshtml", "Views/Account/Register.cshtml"];
const hashes = Object.fromEntries(inputs.map(file => [file, crypto.createHash("sha256")
    .update(fs.readFileSync(path.join(root, "src/SmartFarm.Web", file))).digest("hex")]));
// F2 intentionally changes the public landing and removes decorative auth captions.
// Keep the original binary goldens for unchanged images. The 18 reviewed F2
// screenshots are protected by exact PNG fingerprints and dimensions; source
// blobs are bound too. This does not relax the original visual comparison gate.
const f2ReviewPath = path.join(directory, "f2-reviewed-signatures.json");
const f2Review = !update && fs.existsSync(f2ReviewPath)
    ? JSON.parse(fs.readFileSync(f2ReviewPath)) : null;
const f4ReviewPath = path.join(directory, "f4-reviewed-signatures.json");
const f4Review = !update && fs.existsSync(f4ReviewPath)
    ? JSON.parse(fs.readFileSync(f4ReviewPath)) : null;
const activeReview = f4Review ?? f2Review;
const gitBlobSha = file => {
    const data = fs.readFileSync(path.join(root, "src/SmartFarm.Web", file));
    return crypto.createHash("sha1").update(Buffer.from("blob " + data.length + "\0"))
        .update(data).digest("hex");
};
if (activeReview) {
    assert.equal(activeReview.reviewed, true, "Screenshot signatures require documented visual review.");
    const files = Object.keys(activeReview.sourceBlobShas);
    const actual = Object.fromEntries(files.map(file => [file, gitBlobSha(file)]));
    assert.deepEqual(actual, activeReview.sourceBlobShas,
        "Reviewed visual source changed; inspect screenshots and reapprove before accepting changes.");
    assert.equal(Object.keys(activeReview.screenshots).length, f4Review ? 30 : 18,
        "Review manifest must cover every intentionally changed screenshot.");
}
fs.mkdirSync(output, { recursive: true });
if (update) fs.mkdirSync(directory, { recursive: true });
else {
    const manifest = JSON.parse(fs.readFileSync(path.join(directory, "review.json")));
    assert.equal(manifest.reviewed, true, "Visual baselines must be reviewed before accepting them.");
    // Compare all screenshots *before* checking source hashes, so source-only
    // changes receive actual image-diff evidence without weakening either gate.
    // The manifest must still match exactly before CI can pass.
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
                        // A full-page screenshot includes photos below the fold.
                        // Lazy images may never decode until the browser is told to load them.
                        const photos = [...document.images];
                        for (const photo of photos) photo.loading = "eager";
                        await Promise.race([
                            Promise.all(photos.map(photo => photo.decode())),
                            new Promise((_, reject) => setTimeout(
                                () => reject(new Error("Image decode timed out")), 15000))
                        ]);
                    });
                    const overflow = await page.evaluate(() => ({
                        over: document.documentElement.scrollWidth > innerWidth,
                        viewport: innerWidth,
                        scroll: document.documentElement.scrollWidth,
                        isLanding: document.body.className,
                        cssLoaded: [...document.styleSheets].map(s => s.href).filter(Boolean).slice(-4),
                        probe: [...document.querySelectorAll('.ani6-login,.ani6-topbar .icon-button,.ani6-mobile-menu summary,.ani6-account-links')].map(el => {
                            const r=el.getBoundingClientRect(), c=getComputedStyle(el);
                            return {tag:el.tagName,className:el.className,display:c.display,width:r.width};
                        }),
                        offenders: [...document.querySelectorAll("body *")].filter(el => {
                            const rect = el.getBoundingClientRect();
                            return rect.width && (rect.right > innerWidth + 1 || rect.left < -1);
                        }).slice(0,12).map(el => {
                            const r = el.getBoundingClientRect();
                            return el.tagName.toLowerCase() + "." + String(el.className).slice(0,55)
                                + " x=" + r.left.toFixed(0) + "-" + r.right.toFixed(0);
                        })
                    }));
                    assert.equal(overflow.over, false, key + ": no document overflow " + JSON.stringify(overflow));
                    assert.equal(await page.locator("html").getAttribute("data-color-mode"), mode);
                    const data = await page.screenshot({ fullPage: true, animations: "disabled" });
                    fs.writeFileSync(path.join(output, key + "-candidate.png"), data);
                    captured++;
                    if (update) {
                        fs.writeFileSync(path.join(directory, key + ".png"), data);
                        continue;
                    }
                    const actual = PNG.sync.read(data);
                    const signature = activeReview?.screenshots[key];
                    if (signature) {
                        const digest = crypto.createHash("sha256").update(data).digest("hex");
                        if (actual.width !== signature.width ||
                            actual.height !== signature.height ||
                            digest !== signature.sha256) {
                            findings.push(key + ": changed from reviewed screenshot");
                        }
                        console.log("REVIEWED SIGNATURE", key,
                            digest === signature.sha256 ? "approved" : "DIFFERENT");
                        continue;
                    }
                    const expected = PNG.sync.read(fs.readFileSync(path.join(directory, key + ".png")));
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
            const reviewed = JSON.parse(fs.readFileSync(path.join(directory, "review.json")));
            if (!activeReview) {
                assert.deepEqual(reviewed.sourceHashes, hashes,
                    "Public source hash changed; compare screenshots and update reviewed provenance.");
            }
            console.log(f4Review
                ? "PASS 30 exact F4/F5 reviewed public screenshot fingerprints"
                : f2Review
                ? "PASS 30 reviewed screenshots (18 exact F2 fingerprints; 12 original goldens)"
                : "PASS 30 reviewed light/dark visual baselines");
        }
    } finally { await browser.close(); }
})().catch(error => { console.error(error.stack || error); process.exitCode = 1; });
