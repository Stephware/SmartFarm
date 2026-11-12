const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const web = path.join(__dirname, "../../src/SmartFarm.Web");
const read = file => fs.readFileSync(path.join(web, file), "utf8");
const css = read("wwwroot/css/reference-ui.css");
const layout = read("Views/Shared/_Layout.cshtml");
const admin = read("Areas/Admin/Views/Shared/_Layout.cshtml");

test("tablet navigation stays accessible when the desktop sidebar disappears", () => {
    // Desktop sidebar is hidden below 1001px by site.css; the new breakpoint
    // must not also hide the native mobile menu at 771–1000px.
    assert.match(css, /@media\(min-width:1001px\)\{\.mobile-workspace-bar,\.mobile-nav\{display:none!important\}\}/);
    assert.doesNotMatch(css, /@media\(min-width:771px\)\{\.mobile-workspace-bar,\.mobile-nav\{display:none!important\}\}/);
    assert.match(css, /@media\(max-width:1000px\) and \(min-width:771px\)\{\.app-shell\{grid-template-columns:minmax\(0,1fr\)\}/);
    assert.match(css, /\.app-shell \.mobile-nav \{display:block!important;/);
    assert.match(css, /\.app-shell\.admin-shell \.app-topbar-actions \{display:flex;/);
    assert.match(admin, /class="mobile-nav admin-mobile-nav"/);
    assert.match(layout, /id="mobile-workspace-menu"/);
    assert.match(layout, /id="mobile-more-button"/);
});
test("phone interface preserves readable forms and visible focus", () => {
    assert.match(css, /font-size:16px;min-height:44px;max-width:100%/);
    assert.match(css, /\.app-shell :is\(a,button,input,select,textarea,summary,\[tabindex="0"\]\):focus-visible/);
    assert.match(css, /\.table-scroll:focus-visible,\.table-wrap:focus-visible/);
    assert.match(css, /overscroll-behavior-x:contain/);
    assert.match(css, /prefers-reduced-motion:reduce/);
});
test("nonessential scripts are only referenced by their owning screens", () => {
    const create = read("Views/Farms/Create.cshtml");
    const edit = read("Views/Farms/Edit.cshtml");
    const stock = read("Views/Inventory/Details.cshtml");
    for (const view of [create, edit]) {
        assert.match(view, /js\/farm-location-loader\.js/);
        assert.match(view, /data-picker-script-url=/);
        assert.doesNotMatch(view, /<script src="https:\/\/unpkg\.com\/leaflet/);
        assert.doesNotMatch(view, /<link rel="stylesheet" href="https:\/\/unpkg\.com\/leaflet/);
        assert.match(view, /data-farm-latitude/);
        assert.match(view, /data-farm-longitude/);
    }
    assert.doesNotMatch(layout, /js\/inventory-movement\.js/);
    assert.match(stock, /js\/inventory-movement\.js/);
});

const loader = read("wwwroot/js/farm-location-loader.js");
const flush = () => new Promise(resolve => setImmediate(resolve));

function mapPage() {
    const appended = { head: [], body: [] };
    const events = {};
    const status = { textContent: "" };
    const disclosure = {
        open: false,
        addEventListener(event, fn) { events[event] = fn; }
    };
    const root = {
        dataset: { pickerScriptUrl: "/SmartFarm/js/farm-location-picker.js" },
        closest: key => key === "details" ? disclosure : null,
        querySelector: key => key === "[data-farm-location-status]" ? status : null
    };
    const document = {
        querySelectorAll: () => [root],
        createElement: tag => ({
            tagName: tag.toUpperCase(), events: {},
            addEventListener(event, handler) { this.events[event] = handler; }
        }),
        head: { append(el) { appended.head.push(el); } },
        body: { append(el) { appended.body.push(el); } }
    };
    const window = { L: null };
    vm.runInNewContext(loader, { document, window });
    return { appended, events, status, window, disclosure };
}
test("map is completely deferred until the optional disclosure opens", async () => {
    const page = mapPage();
    assert.equal(page.appended.head.length, 0, "no CDN stylesheet on page load");
    assert.equal(page.appended.body.length, 0, "no CDN script on page load");
    page.disclosure.open = true;
    page.events.toggle();
    assert.match(page.status.textContent, /Loading the optional farm map/);
    assert.equal(page.appended.head.length, 1);
    assert.equal(page.appended.head[0].rel, "stylesheet");
    assert.match(page.appended.head[0].href, /leaflet\.css$/);
    assert.match(page.appended.head[0].integrity, /^sha256-/);
    assert.equal(page.appended.body.length, 1);
    const leaflet = page.appended.body[0];
    assert.match(leaflet.src, /leaflet\.js$/);
    assert.match(leaflet.integrity, /^sha256-/);
    page.window.L = { map() {} };
    leaflet.events.load();
    await flush();
    assert.equal(page.appended.body.length, 2);
    assert.equal(page.appended.body[1].src, "/SmartFarm/js/farm-location-picker.js");
    page.appended.body[1].events.load();
    await flush();
    page.events.toggle();
    assert.equal(page.appended.body.length, 2, "reopening must not load Leaflet twice");
});
test("CDN failures leave the basic farm form available and permit a retry", async () => {
    const page = mapPage();
    page.disclosure.open = true;
    page.events.toggle();
    page.appended.body[0].events.error();
    await flush();
    assert.match(page.status.textContent, /You can still save the farm name and nearby location/);
    page.disclosure.open = false;
    page.events.toggle();
    page.disclosure.open = true;
    page.events.toggle();
    assert.equal(page.appended.body.length, 2, "a new deliberate open can retry the map");
});

test("validation links reveal a closed details section and modal before focusing", () => {
    const source = read("wwwroot/js/site.js");
    const callbacks = {};
    const disclosure = { tagName: "DETAILS", open: false, parentElement: null };
    const dialog = {
        open: false, parentElement: null,
        showModal() { this.open = true; }
    };
    disclosure.parentElement = dialog;
    let focused = false;
    const field = {
        type: "text", parentElement: disclosure,
        closest: selector => selector === "dialog" ? dialog : null,
        setAttribute(name, value) {
            assert.equal(name, "aria-invalid");
            assert.equal(value, "true");
        },
        focus() {
            assert.equal(disclosure.open, true);
            assert.equal(dialog.open, true);
            focused = true;
        }
    };
    const link = {
        hash: "#Notes",
        addEventListener(name, fn) { callbacks[name] = fn; }
    };
    const summary = {
        focus() {},
        querySelectorAll: () => [link]
    };
    const document = {
        querySelector: selector => selector === "[data-error-summary]" ? summary : null,
        querySelectorAll: () => [],
        getElementById: id => id === "Notes" ? field : null,
        documentElement: { dataset: {} }
    };
    vm.runInNewContext(source, {
        document,
        window: {
            location: { hash: "" },
            requestAnimationFrame(callback) { callback(); },
            addEventListener() {}
        }
    });
    assert.equal(field.type, "text");
    callbacks.click();
    assert.equal(focused, true);
});
