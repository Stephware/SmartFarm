"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "../..");
const read = relative => fs.readFileSync(path.join(root, relative), "utf8");
const webLayout = read("src/SmartFarm.Web/Views/Shared/_Layout.cshtml");
const adminLayout = read("src/SmartFarm.Web/Areas/Admin/Views/Shared/_Layout.cshtml");
const brandStyles = read("src/SmartFarm.Web/Views/Shared/_AniGoBrandStyles.cshtml");
const farmComponents = read("mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/FarmDesignComponents.kt");
const authSurface = read("mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/PhotoAuthScaffold.kt");

function mobileHeader(layout) {
  const index = layout.indexOf('class="anigo-logo-pair anigo-mobile-logo"');
  assert.notEqual(index, -1, "signed-in mobile logo exists");
  return layout.slice(index, layout.indexOf("</span>", index) + "</span>".length);
}
function assertMobileMark(layout, area) {
  const region = mobileHeader(layout);
  assert.match(region, /anigo-mark\.svg/);
  assert.match(region, /anigo-mark-on-dark\.svg/);
  assert.doesNotMatch(region, /anigo-logo\.svg|anigo-logo-on-dark\.svg/, area + " must not duplicate wordmark");
  assert.match(layout, /aria-label="@T\["AniGo (home|administration)"\]"/, area + " needs an accessible link");
}

test("Signed-in MVC and Admin mobile bars show the compact mark, not wordmark", () => {
  assertMobileMark(webLayout, "farm");
  assertMobileMark(adminLayout, "admin");
  assert.match(brandStyles, /\.anigo-mobile-brand \.anigo-logo-pair \{ width: 42px; height: 42px;/);
});

test("Desktop and guest entry points still display the official complete wordmark", () => {
  assert.match(webLayout, /class="anigo-sidebar-logo"[^>]*anigo-logo-on-dark\.svg/);
  assert.match(adminLayout, /class="anigo-sidebar-logo"[^>]*anigo-logo-on-dark\.svg/);
  assert.match(webLayout, /anigo-guest-logo[^\n]*anigo-logo\.svg/);
  assert.match(webLayout, /anigo-guest-logo[^\n]*anigo-logo-on-dark\.svg/);
});

test("Android workspace app bar selects compact mark for both theme surfaces", () => {
  const bar = farmComponents.split("fun FarmAppBar(")[1]
    .split("Box(Modifier.weight(1f))")[0];
  assert.match(bar, /R\.drawable\.ani_go_mark_on_dark/);
  assert.match(bar, /R\.drawable\.ani_go_mark\b/);
  assert.doesNotMatch(bar, /R\.drawable\.ani_go_logo/);
  assert.match(bar, /contentDescription = uiText\(R\.string\.\w+, "AniGo"\)/);
  assert.match(bar, /Modifier\.size\(40\.dp\)/);
});

test("Photo auth uses real official light-on-dark full wordmark only", () => {
  assert.match(authSurface, /R\.drawable\.ani_go_logo_on_dark/);
  assert.match(authSurface, /contentDescription = uiText\(R\.string\.\w+, "AniGo"\)/);
  assert.doesNotMatch(authSurface, /AniGoPhotoMark|Canvas\(|drawPath\(|R\.drawable\.ani_go_mark/);
  assert.doesNotMatch(authSurface, /Text\(\s*"AniGo"/);
  assert.match(authSurface, /R\.drawable\.ani6_fields_hero/, "approved natural farm photography remains");
});
