"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "../..");
const read = relative => fs.readFileSync(path.join(root, relative), "utf8");
const webRoot = "src/SmartFarm.Web/wwwroot/images/branding/";
const androidRoot = "mobile/SmartFarm.Android/app/src/main/res/drawable/";

// Parse only the authored vector paths; preserve the exact path geometry and order.
const svgPaths = content => [...content.matchAll(/<path\s+fill="#([A-F0-9]{6})"\s+fill-rule="evenodd"\s+d="([^"]+)"/g)]
  .map(([, color, geometry]) => ({ color, geometry }));
const androidPaths = content => [...content.matchAll(/<path\s+android:fillColor="#([A-F0-9]{6})"\s+android:fillType="evenOdd"\s+android:pathData="([^"]+)"/g)]
  .map(([, color, geometry]) => ({ color, geometry }));

function expectOnlyCreamRecolor(light, dark, label) {
  assert.equal(light.length, 2, label + " original path count");
  assert.equal(dark.length, 2, label + " dark path count");
  assert.deepEqual(light.map(item => item.geometry), dark.map(item => item.geometry),
    label + " geometry must not change");
  assert.deepEqual(light.map(item => item.color), ["134D30", "CBA14E"], label + " original palette");
  assert.deepEqual(dark.map(item => item.color), ["FFFDF6", "CBA14E"], label + " dark palette");
}

test("Web dark A mark preserves approved green/gold symbol geometry", () => {
  const light = read(webRoot + "anigo-mark.svg");
  const dark = read(webRoot + "anigo-mark-on-dark.svg");
  expectOnlyCreamRecolor(svgPaths(light), svgPaths(dark), "web");
  assert.match(light, /viewBox="136 93 380 293"/);
  assert.match(dark, /viewBox="136 93 380 293"/);
  assert.match(dark, /<title id="title">AniGo mark on dark<\/title>/);
  assert.ok(!dark.includes("#134D30"), "dark SVG must not retain dark green");
});

test("Android dark A mark preserves exact path data and viewport", () => {
  const light = read(androidRoot + "ani_go_mark.xml");
  const dark = read(androidRoot + "ani_go_mark_on_dark.xml");
  expectOnlyCreamRecolor(androidPaths(light), androidPaths(dark), "Android");
  for (const key of ['android:width="48dp"', 'android:height="48dp"',
    'android:viewportWidth="440"', 'android:viewportHeight="440"',
    'android:translateX="-86"', 'android:translateY="-20"']) {
    assert.ok(light.includes(key) && dark.includes(key), key);
  }
  assert.ok(!dark.includes("#134D30"), "dark VectorDrawable must not retain dark green");
});

test("Approved full-size dark wordmark and compact marks use matching cream and gold", () => {
  const fullWeb = read(webRoot + "anigo-logo-on-dark.svg");
  const fullAndroid = read(androidRoot + "ani_go_logo_on_dark.xml");
  assert.ok(fullWeb.includes('fill="#FFFDF6"') && fullWeb.includes('fill="#CBA14E"'));
  assert.ok(fullAndroid.includes('android:fillColor="#FFFDF6"') &&
    fullAndroid.includes('android:fillColor="#CBA14E"'));
});
