const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const web = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = file => fs.readFileSync(path.join(web, file), "utf8");
const landing = read("Views/Home/Index.cshtml");
const login = read("Views/Account/Login.cshtml");
const register = read("Views/Account/Register.cshtml");
const farms = read("Views/Farms/Index.cshtml");
const create = read("Views/Farms/Create.cshtml");
const edit = read("Views/Farms/Edit.cshtml");
const styles = read("Styles/anigo-entry.css");

test("Real photo assets are bundled and optimized WebP, not external runtime dependencies", () => {
    for (const name of ["fields-hero.webp", "farmers-working.webp", "fields-detail.webp"]) {
        const file = path.join(web, "wwwroot/images/photography", name);
        const bytes = fs.readFileSync(file);
        assert.ok(bytes.length >= 4000, name + " must be an actual image");
        assert.ok(bytes.length <= 550000, name + " is too large for a marketing image");
        assert.equal(bytes.toString("ascii", 0, 4), "RIFF");
        assert.equal(bytes.toString("ascii", 8, 12), "WEBP");
    }
    assert.match(landing, /fields-hero\.webp/);
    assert.match(landing, /farmers-working\.webp/);
    assert.match(login, /fields-detail\.webp/);
    assert.match(register, /farmers-working\.webp/);
    assert.match(landing, /Photos?|photography/i);
    assert.doesNotMatch(landing + login + register, /images\/crops\/.*?\.svg/);
    assert.doesNotMatch(landing + login + register, /images\.unsplash\.com/);
});
test("Editorial photographs are not presented as real farm records", () => {
    assert.match(landing, /not a farm record/i);
    assert.match(landing, /do not represent AniGo customer farms/);
    assert.match(login, /Agricultural photography for illustration only/);
    assert.match(register, /Agricultural photography for illustration only/);
    assert.doesNotMatch(farms, /images\/photography/);
    assert.match(landing, /Unsplash/);
});
test("Entry flows preserve accessible forms, routes and owner approval rules", () => {
    for (const [file, fields] of [
        [login, ["Email", "Password"]],
        [register, ["FullName", "Email", "Password"]]
    ]) {
        for (const name of fields) assert.match(file, new RegExp('asp-for="' + name + '"'));
        assert.match(file, /asp-action="(?:Login|Register)"/);
        assert.match(file, /data-password-toggle/);
        assert.doesNotMatch(file, /<form[^>]*novalidate/);
    }
    assert.match(landing, /Get started with AniGo/);
    assert.match(landing, /How would you like to use AniGo\?/);
    assert.match(landing, /wait for the owner to approve/);
    assert.match(register, /A farm owner must approve join requests/);
    assert.match(farms, /id="join-farm"/);
    assert.match(farms, /asp-action="RequestJoin"/);
    assert.match(farms, /@Html\.ValidationMessage\("FarmCode"/);
    assert.match(farms, /asp-action="CancelJoin"/);
    assert.match(farms, /Create my first farm/);
    assert.match(farms, /farm\.Role/);
    assert.match(farms, /farm\.LocationText/);
});
test("Farm creation keeps its required fields, timezone and opt-in map", () => {
    for (const field of ["Name", "LocationText", "TimeZoneId"])
        assert.match(create, new RegExp('asp-for="' + field + '"'));
    assert.match(create, /farm-advanced-settings/);
    assert.match(create, /data-farm-location-picker/);
    assert.match(create, /data-picker-script-url/);
    assert.match(create, /js\/farm-location-loader\.js/);
    assert.doesNotMatch(create, /<script src="https:\/\/unpkg\.com/);
    assert.match(edit, /asp-for="Input\.RowVersion"/);
    assert.match(edit, /data-farm-location-picker/);
    assert.match(edit, /js\/farm-location-loader\.js/);
});
test("Completion keeps create/join intent and mobile control usability", () => {
    const controller = read("Controllers/AccountController.cs");
    assert.match(landing, /asp-route-returnUrl="\\@\\(signedIn \\? null : Url\\.Action\\("Create", "Farms"\\)\\)"/);
    assert.match(landing, /#join-farm/);
    assert.match(register, /asp-route-returnUrl="\\@nextUrl"/);
    assert.match(register, /placeholder="Password"/);
    assert.match(login, /asp-route-returnUrl="\\@ViewData\\["ReturnUrl"\\]"/);
    assert.match(controller, /Url\\.IsLocalUrl\\(returnUrl\\)/);
    assert.match(styles, /top: 50%/);
    assert.match(styles, /min-height: 44px; min-width: 72px/);
    assert.match(styles, /max-width:420px/);
    assert.match(styles, /list-style: disclosure-closed inside/);
    assert.match(landing, /Photo credits and sources/);
});

test("New visual composition remains scoped and respects responsive / reduced motion", () => {
    const src = read("Styles/anigo-tailwind.css");
    assert.match(src, /@import "\.\/anigo-entry\.css"/);
    assert.match(styles, /\.ani-entry-hero/);
    assert.match(styles, /\.ani-entry-auth-layout/);
    assert.match(styles, /\.ani-entry-join-layout/);
    assert.match(styles, /max-width:600px/);
    assert.match(styles, /max-width:850px/);
    assert.match(styles, /prefers-reduced-motion:reduce/);
    assert.match(styles, /var\(--ui-surface\)/);
    assert.doesNotMatch(styles, /\.ani-entry-farm-card\s*img\s*\{/);
});
