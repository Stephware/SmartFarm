"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const root = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = relative => fs.readFileSync(path.join(root, relative), "utf8");

test("farm selector offers quick Create and Join dialogs without removing full setup fallback", () => {
    const farms = read("Views/Farms/Index.cshtml");
    const full = read("Views/Farms/Create.cshtml");
    assert.match(farms, /href="#create-farm"/);
    assert.match(farms, /href="#join-farm"/);
    for (const id of ["create-farm", "join-farm"]) {
        assert.match(farms, new RegExp('id="' + id + '"[^>]*data-form-dialog'));
    }
    assert.match(farms, /name="Name"[^>]*required/);
    assert.match(farms, /name="LocationText"[^>]*required/);
    assert.match(farms, /name="TimeZoneId" value="Asia\/Manila"/);
    assert.match(farms, /asp-action="Create" method="post"/);
    assert.match(farms, /asp-action="RequestJoin" method="post"/);
    assert.match(farms, /data-form-error/);
    assert.match(farms, /data-ani-p5-form/);
    assert.match(farms, /data-ani-form-progress/);
    assert.match(farms, /Use the detailed setup with map options/);
    assert.match(full, /data-farm-location-picker/);
    assert.doesNotMatch(farms, /My account<\/a>/);
    assert.match(read("Views/Shared/_Layout.cshtml"), /@T\["Account & settings"\]/);
});

function dialogFixture(invalid = false) {
    const listeners = {};
    const buttonListeners = {};
    const field = { name: "FarmCode", type: "text", value: "", focus() { this.focused = true; } };
    const form = {
        querySelectorAll: selector => selector === "input,select,textarea" ? [field] : [],
        querySelector: selector => selector.includes("input") ? field : null
    };
    const heading = { id: "join-farm-heading", textContent: "Join farm" };
    const section = {
        id: "join-farm",
        querySelector(selector) {
            if (selector === "form") return form;
            if (selector === "h2") return heading;
            if (selector.includes("[data-form-error]")) return invalid ? {} : null;
            return null;
        },
        before(dialog) { this.dialog = dialog; }
    };
    const trigger = {
        getAttribute: name => name === "href" ? "#join-farm" : null,
        setAttribute(name,value) { this[name] = value; },
        addEventListener(name,cb) { this[name] = cb; },
        focus() { this.focused = true; }
    };
    const createElement = tag => {
        const el = {
            tagName: tag.toUpperCase(), children: [], classList: { add() {} },
            open: false, attributes: {},
            append(child) { this.children.push(child); },
            setAttribute(name,value) { this.attributes[name] = value; },
            addEventListener(name,cb) { (tag === "dialog" ? listeners : buttonListeners)[name] = cb; },
            showModal() { this.open = true; },
            close() { this.open = false; listeners.close?.(); }
        };
        return el;
    };
    let confirmResult = false, confirmations = 0;
    const document = {
        querySelectorAll(selector) {
            if (selector === "section[data-form-dialog]") return [section];
            if (selector === "a[href]") return [trigger];
            return [];
        },
        createElement, getElementById: () => null
    };
    const window = {
        location: { hash: "" }, addEventListener() {},
        confirm() { confirmations++; return confirmResult; }
    };
    const script = read("wwwroot/js/site.js");
    const snippet = script.slice(script.indexOf("// Native dialogs"),
        script.indexOf("/* Phase 5:"));
    assert.ok(snippet.length > 100);
    vm.runInNewContext(snippet, { document, window, HTMLDialogElement: class {} });
    return {
        field, trigger, section, listeners,
        close: () => buttonListeners.click(),
        approveDiscard(value) { confirmResult = value; },
        confirmed: () => confirmations
    };
}

test("dialog focuses its form and retains unsaved input when Close is canceled", () => {
    const x = dialogFixture();
    x.trigger.click({ preventDefault() {} });
    assert.equal(x.section.dialog.open, true);
    assert.equal(x.field.focused, true);
    assert.equal(x.trigger["aria-haspopup"], "dialog");
    x.field.value = "ABCD12345678";
    x.close();
    assert.equal(x.section.dialog.open, true);
    assert.equal(x.field.value, "ABCD12345678");
    assert.equal(x.confirmed(), 1);
    x.approveDiscard(true);
    x.close();
    assert.equal(x.section.dialog.open, false);
    assert.equal(x.trigger.focused, true);
});

test("server-side validation reopens the dialog instead of losing the invalid draft", () => {
    const x = dialogFixture(true);
    assert.equal(x.section.dialog.open, true);
    assert.equal(x.field.focused, true);
});
