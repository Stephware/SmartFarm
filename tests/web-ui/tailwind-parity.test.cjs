const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.join(__dirname, "../../src/SmartFarm.Web");
const read = name => fs.readFileSync(path.join(root, name), "utf8");

test("Tailwind uses prefixed utilities without global Preflight resets", () => {
    const source = read("Styles/anigo-tailwind.css");
    const built = read("wwwroot/css/anigo-tailwind.css");
    assert.match(source, /prefix\(tw\)/);
    assert.match(source, /source\(none\)/);
    assert.doesNotMatch(source, /@import\s+["']tailwindcss\/preflight/);
    assert.doesNotMatch(built, /button\s*,\s*input\s*,\s*select/);
    assert.match(built, /\.tw\\:min-w-0/);
    assert.match(source, /--color-ani-pine:\s*var\(--pine\)/);
    assert.match(source, /--color-ani-gold:\s*var\(--gold\)/);
    assert.match(source, /--color-ani-surface:\s*var\(--card\)/);
    assert.match(source, /--color-ani-text:\s*var\(--ink\)/);
});
test("Generated Tailwind CSS is self-hosted and loaded in both Razor layouts", () => {
    for (const layout of [
        "Views/Shared/_Layout.cshtml",
        "Areas/Admin/Views/Shared/_Layout.cshtml"
    ]) {
        const markup = read(layout);
        const tailwind = markup.indexOf('href="~/css/anigo-tailwind.css"');
        const original = markup.indexOf('href="~/css/reference-ui.css"');
        assert.ok(tailwind >= 0 && original >= 0 && tailwind < original,
            layout + ": utility stylesheet must precede the existing theme");
        assert.doesNotMatch(markup, /cdn\.tailwindcss\.com/);
    }
    assert.ok(read("wwwroot/css/anigo-tailwind.css").length > 150);
});
test("Shared Razor presentation components preserve established CSS classes", () => {
    const empty = read("Views/Shared/_UiEmptyState.cshtml");
    const status = read("Views/Shared/_UiStatusChip.cshtml");
    assert.match(empty, /class="sf-empty-state tw:min-w-0/);
    assert.match(status, /class="sf-status-chip @Model\.Variant/);
    for (const view of [
        "Views/Inventory/Index.cshtml",
        "Views/Notifications/Index.cshtml",
        "Views/Tasks/Index.cshtml",
        "Views/Weather/Index.cshtml"
    ])
        assert.match(read(view), /partial name="_UiEmptyState"|partial name="_UiStatusChip"/);
});
