"use strict";
// Exercise production interaction code in Chromium; no database writes or credentials.
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");
const root = path.resolve(__dirname, "../../src/SmartFarm.Web/wwwroot/js");
async function screen(run) {
    const browser = await chromium.launch();
    try {
        const page = await browser.newPage({ viewport: { width: 375, height: 667 } });
        await page.setContent(`<main>
            <a href="#edit">Edit farm</a><a href="/Farms">Leave</a>
            <section id="edit" data-form-dialog><h2>Edit farm</h2>
                <form method="post"><label>Name<input name="Name" value="North farm" required></label>
                <button name="decision" value="save">Save</button></form></section>
            <form id="ordinary" method="post"><label>Reason<input name="Reason" required></label>
                <button name="decision" value="archive">Archive</button></form>
            <form id="ajax" method="post"><input name="Note"><button>Update</button></form>
            </main>`);
        await page.evaluate(() => {
            window.posts = [];
            document.querySelector("#ajax").addEventListener("submit", e => e.preventDefault());
            // Observe successful native submission after all app listeners, without navigating.
            window.addEventListener("submit", e => {
                if (!e.defaultPrevented) window.posts.push([...new FormData(e.target, e.submitter)]);
                e.preventDefault();
            });
        });
        await page.addScriptTag({ path: path.join(root, "site.js") });
        const guards = path.join(root, "interaction-guards.js");
        if (fs.existsSync(guards)) await page.addScriptTag({ path: guards });
        await run(page);
    } finally { await browser.close(); }
}
test("every native POST blocks repeated submits and preserves named decisions", async () => screen(async page => {
    await page.locator("#ordinary input").fill("Duplicate entry");
    await page.locator("#ordinary button").evaluate(button => {
        button.form.requestSubmit(button); button.form.requestSubmit(button);
    });
    assert.equal(await page.evaluate(() => posts.length), 1);
    assert.deepEqual(await page.evaluate(() => posts[0]), [["Reason", "Duplicate entry"], ["decision", "archive"]]);
    assert.equal(await page.locator("#ordinary button").getAttribute("aria-disabled"), "true");
    await page.evaluate(() => dispatchEvent(new PageTransitionEvent("pageshow", { persisted: true })));
    assert.equal(await page.locator("#ordinary button").getAttribute("aria-disabled"), null);
}));
test("cancelled custom submissions never leave a form busy", async () => screen(async page => {
    await page.locator("#ajax button").click();
    assert.equal(await page.locator("#ajax").getAttribute("aria-busy"), null);
    assert.equal(await page.evaluate(() => posts.length), 0);
}));
test("dirty dialog cancellation retains draft and accepted discard restores initial values", async () => screen(async page => {
    await page.getByRole("link", { name: "Edit farm", exact: true }).click();
    await page.getByRole("textbox", { name: "Name", exact: true }).fill("Unfinished farm");
    page.once("dialog", dialog => dialog.dismiss());
    await page.keyboard.press("Escape");
    assert.equal(await page.locator("dialog").evaluate(d => d.open), true);
    assert.equal(await page.locator("dialog input").inputValue(), "Unfinished farm");
    page.once("dialog", dialog => dialog.accept());
    await page.getByRole("button", { name: "Close Edit farm" }).click();
    assert.equal(await page.locator("dialog").evaluate(d => d.open), false);
    assert.equal(await page.getByRole("link", { name: "Edit farm", exact: true }).evaluate(el => el === document.activeElement), true);
    await page.getByRole("link", { name: "Edit farm", exact: true }).click();
    assert.equal(await page.locator("dialog input").inputValue(), "North farm");
}));
test("clicking dialog padding keeps the form open", async () => screen(async page => {
    await page.getByRole("link", { name: "Edit farm", exact: true }).click();
    await page.locator("dialog").evaluate(d => { d.style.padding = "24px"; });
    const box = await page.locator("dialog").boundingBox();
    await page.mouse.click(box.x + 2, box.y + 2);
    assert.equal(await page.locator("dialog").evaluate(d => d.open), true);
}));
test("native Back/reload warns about an edited business form", async () => screen(async page => {
    await page.locator("#ordinary input").fill("Keep this reason");
    assert.equal(await page.evaluate(() => {
        const event = new Event("beforeunload", { cancelable: true });
        dispatchEvent(event); return event.defaultPrevented;
    }), true);
}));
