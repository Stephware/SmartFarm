"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = p => fs.readFileSync(path.join(root, p), "utf8");

test("Phase 6 landing retains actual roles/routes, photos and section navigation", () => {
    const home = read("Views/Home/Index.cshtml");
    const layout = read("Views/Shared/_Layout.cshtml");
    const css = read("Styles/anigo-landing-phase6.css");
    const workspace = read("Styles/anigo-workspace-phase6.css");
    for (const section of ["home","features","how-it-works","about","contact"]) {
        assert.match(home, new RegExp('id="' + section + '"'));
        assert.match(layout + home, new RegExp('href="#' + section + '"'));
    }
    for (const block of ["ani6-hero","ani6-features","ani6-steps","ani6-story","ani6-paths","ani6-cta","ani6-footer"])
        assert.ok(home.includes(block), block);
    assert.match(home, /fields-hero\.webp/);
    assert.match(home, /farmers-working\.webp/);
    assert.match(home, /fields-detail\.webp/);
    assert.match(home, /asp-route-returnUrl=/);
    assert.match(home, /#join-farm/);
    assert.match(home, /asp-controller="Home" asp-action="Help"/);
    const source = read("Styles/anigo-tailwind.css");
    assert.match(source, /anigo-landing-phase6\.css/);
    assert.match(source, /anigo-workspace-phase6\.css/);
    assert.doesNotMatch(layout, /<link[^>]+anigo-(?:landing|workspace)-phase6\.css/);
    assert.match(css, /@media \(max-width:375px\)/);
    assert.match(css, /html\[data-color-mode="dark"\]/);
    assert.match(workspace, /body\.is-authenticated/);
    assert.doesNotMatch(home, /href="javascript:|lorem ipsum|Coming soon/i);
});
