const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const web = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = file => fs.readFileSync(path.join(web, file), "utf8");

test("Phase 1 design foundation is part of the same compiled stylesheet", () => {
    const entry = read("Styles/anigo-tailwind.css");
    const foundation = read("Styles/anigo-design-foundation.css");
    const compiled = read("wwwroot/css/anigo-tailwind.css");
    assert.match(entry, /@import "\.\/anigo-design-foundation\.css"/);
    for (const token of [
        "--ui-type-page:", "--ui-type-body:", "--ui-type-supporting:",
        "--ui-space-5:", "--ui-space-7:", "--ui-radius-button:", "--ui-card-padding:"
    ]) {
        assert.ok(foundation.includes(token), "source missing " + token);
        assert.ok(compiled.includes(token), "bundle missing " + token);
    }
    assert.match(foundation, /"Manrope", "Segoe UI", system-ui, sans-serif/);
    assert.match(foundation, /@media \(max-width: 600px\)/);
    assert.match(compiled, /\.sf-status-chip/);
});

test("Phase 1 preserves shared web entry and admin stylesheet loading", () => {
    for (const file of ["Views/Shared/_Layout.cshtml", "Areas/Admin/Views/Shared/_Layout.cshtml"]) {
        const markup = read(file);
        assert.match(markup, /css\/anigo-tailwind\.css/);
        assert.doesNotMatch(markup, /cdn\.tailwindcss\.com/);
    }
});
