"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "../..");
const read = p => fs.readFileSync(path.join(root, p), "utf8");
const source = read("src/SmartFarm.Web/Styles/anigo-foundation.css");
const compiled = read("src/SmartFarm.Web/wwwroot/css/anigo-tailwind.css");
const android = read("mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/theme/Theme.kt");

const expected = {
  "--ui-page": "#121917",
  "--ui-surface": "#202b26",
  "--ui-surface-subtle": "#2a3832",
  "--ui-text": "#f1f4f0",
  "--ui-text-secondary": "#b6c5bb",
  "--ui-primary": "#8fcfa5",
  "--ui-gold": "#d4ad61",
  "--ui-nav": "#19382c",
  "--ui-action": "#397a58",
  "--ui-action-hover": "#3e835e",
  "--ui-on-action": "#fff"
};

const cssDark = source.split(':root[data-color-mode="dark"] {')[1].split("\n  }")[0];
const compiledDark = compiled.split(":root[data-color-mode=dark]{")[1].split("}*,:before,:after{")[0];
function luminance(hex) {
  const rgb = hex.match(/[0-9a-f]{2}/gi).map(n => parseInt(n, 16) / 255)
    .map(n => n <= 0.04045 ? n / 12.92 : Math.pow((n + 0.055) / 1.055, 2.4));
  return rgb[0] * 0.2126 + rgb[1] * 0.7152 + rgb[2] * 0.0722;
}
function contrast(a, b) {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

test("Phase 7 palette uses matching authored and committed CSS tokens", () => {
  for (const [token, hex] of Object.entries(expected)) {
    assert.ok(cssDark.includes(token + ": " + hex + ";"), "CSS source " + token);
    assert.ok(compiledDark.includes(token + ":" + hex), "compiled CSS " + token);
  }
  assert.match(source, /--ui-page: #f5f7f5;/, "light theme remains intact");
  assert.match(source, /--ui-surface: #ffffff;/, "light surfaces remain intact");
  assert.match(compiled, /--ui-action,var\(--primary\)/, "filled buttons use distinct pine actions");
  assert.match(compiled, /--ui-action-hover,var\(--primary-hover\)/, "filled buttons have contrast-safe hover");
});

test("Phase 7 theme provides legible text and filled action pairs", () => {
  assert.ok(contrast("#397a58", "#ffffff") >= 4.5, "pine on white reaches WCAG AA");
  assert.ok(contrast("#3e835e", "#ffffff") >= 4.5, "pine hover on white reaches WCAG AA");
  assert.ok(contrast("#8fcfa5", "#202b26") >= 4.5, "mint links on charcoal reach WCAG AA");
  assert.ok(contrast("#b6c5bb", "#202b26") >= 4.5, "secondary text on charcoal reaches WCAG AA");
  assert.ok(contrast("#d4ad61", "#121917") >= 4.5, "harvest gold on page reaches WCAG AA");
});

test("Phase 7 Android follows charcoal-first system without changing light palette", () => {
  const dark = android.split("internal val DarkColors = darkColorScheme(")[1].split("\n)")[0];
  const light = android.split("internal val LightColors = lightColorScheme(")[1].split("\n)")[0];
  for (const [key, code] of Object.entries({
    background: "121917", surface: "202B26", surfaceVariant: "2A3832",
    onBackground: "F1F4F0", primaryContainer: "19382C", secondary: "D4AD61"
  })) {
    assert.match(dark, new RegExp(key + " = Color\\(0xFF" + code + "\\)"), key);
  }
  assert.match(android, /val darkAction = Color\(0xFF397A58\)/);
  assert.match(android, /getSharedPreferences\("appearance"/);
  assert.match(light, /background = AniGoPalette\.cream/);
  assert.match(light, /surface = Color\.White/);
});

test("Approved Phase 6 photography and route-oriented landing remain untouched", () => {
  const landing = read("src/SmartFarm.Web/Views/Home/Index.cshtml");
  assert.match(landing, /fields-hero\.webp/);
  assert.match(landing, /farmers-working\.webp/);
  assert.match(landing, /asp-route-returnUrl=/);
});
