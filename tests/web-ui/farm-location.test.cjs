const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
const path = require("node:path");

const source = fs.readFileSync(path.join(__dirname,
  "../../src/SmartFarm.Web/wwwroot/js/farm-location-picker.js"), "utf8");

function setup() {
    const listeners = {};
    const controls = {};
    const mock = name => controls[name] = {
        value: "", textContent: "", disabled: false, children: [], innerHTML: "",
        addEventListener: (evt, listener) => { listeners[name + ":" + evt] = listener; },
        replaceChildren: () => { controls[name].children = []; },
        append: item => { controls[name].children.push(item); }
    };
    for (const name of ["[data-farm-location-label]", "[data-farm-latitude]",
      "[data-farm-longitude]", "[data-farm-search]", "[data-farm-search-button]",
      "[data-farm-results]", "[data-farm-location-status]", "[data-farm-clear]",
      "[data-farm-gps]", "[data-farm-map]"]) mock(name);
    controls["[data-farm-location-label]"].value = "My farm";
    const form = {
        querySelector: name => controls[name],
        addEventListener: (name, fn) => { listeners["form:" + name] = fn; }
    };
    const disclosure = {
        open: false,
        addEventListener: (name, fn) => { listeners["disclosure:" + name] = fn; }
    };
    const root = {
        closest: selector => selector === "form" ? form : selector === "details" ? disclosure : null,
        dataset: { searchUrl: "/farms/locations/search" },
        querySelector: name => controls[name]
    };
    let invalidations = 0;
    const map = {
        setView: () => map,
        on: (name, fn) => { listeners["map:" + name] = fn; },
        invalidateSize: () => { invalidations++; },
        removeLayer: () => {}
    };
    const L = {
        map: () => map,
        tileLayer: () => ({ addTo: () => {} }),
        marker: () => ({
            addTo: function () { return this; },
            setLatLng: () => {},
            on: () => {},
        })
    };
    const fetches = [];
    const context = {
        document: {
            querySelectorAll: () => [root],
            createElement: () => ({
                addEventListener: function (event, cb) { this.choose = cb; }
            })
        },
        window: { L, addEventListener: () => {}, requestAnimationFrame: cb => cb() },
        L,
        navigator: {},
        fetch: async url => {
            fetches.push(url);
            return { ok: true, json: async () => [
                { name: "A nearby barangay", latitude: 15.123456, longitude: 120.987654 }
            ] };
        },
        encodeURIComponent
    };
    vm.runInNewContext(source, context);
    return { listeners, controls, fetches, disclosure, invalidations: () => invalidations };
}

test("location search is only called by a deliberate user action", async () => {
    const { listeners, controls, fetches } = setup();
    assert.equal(fetches.length, 0);
    controls["[data-farm-search]"].value = "Angeles City";
    await listeners["[data-farm-search-button]:click"]();
    assert.equal(fetches.length, 1);
    assert.match(fetches[0], /query=Angeles%20City/);
});

test("choosing a search result fills location and six-decimal coordinates", async () => {
    const { listeners, controls } = setup();
    controls["[data-farm-search]"].value = "Angeles City";
    await listeners["[data-farm-search-button]:click"]();
    const choice = controls["[data-farm-results]"].children[0];
    assert.equal(choice.textContent, "A nearby barangay");
    choice.choose();
    assert.equal(controls["[data-farm-location-label]"].value, "A nearby barangay");
    assert.equal(controls["[data-farm-latitude]"].value, "15.123456");
    assert.equal(controls["[data-farm-longitude]"].value, "120.987654");
    controls["[data-farm-location-label]"].value = "Different place";
    listeners["[data-farm-location-label]:input"]();
    assert.equal(controls["[data-farm-latitude]"].value, "");
    assert.equal(controls["[data-farm-longitude]"].value, "");
});

test("optional location map refreshes after its details panel is opened", () => {
    const { listeners, disclosure, invalidations } = setup();
    assert.equal(invalidations(), 0);
    disclosure.open = true;
    listeners["disclosure:toggle"]();
    assert.equal(invalidations(), 1);
    disclosure.open = false;
    listeners["disclosure:toggle"]();
    assert.equal(invalidations(), 1);
});
