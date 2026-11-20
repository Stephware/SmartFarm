"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "../..");
const read = relative => fs.readFileSync(path.join(root, relative), "utf8");
const photoAuth = read("mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/PhotoAuthScaffold.kt");
const androidAccount = read("mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/auth/AuthScreens.kt");
const layout = read("src/SmartFarm.Web/Views/Shared/_Layout.cshtml");
const profile = read("src/SmartFarm.Web/Views/Account/Profile.cshtml");
const picker = read("src/SmartFarm.Web/Views/Shared/_LanguagePicker.cshtml");
const style = read("src/SmartFarm.Web/Views/Shared/_AniGoAccountPreferencesStyles.cshtml");
const controller = read("src/SmartFarm.Web/Controllers/LanguageController.cs");
const filipino = read("src/SmartFarm.Web/Resources/SharedResource.fil.resx");

test("Language choices are removed from guest and Android photo-backed login surfaces", () => {
  assert.doesNotMatch(photoAuth, /LanguagePicker\(\)/);
  assert.doesNotMatch(layout, /<partial name="_LanguagePicker"/);
});

test("Web My Account groups appearance and language under a single settings card", () => {
  const start = profile.indexOf('class="panel appearance-setting ani-account-preferences"');
  assert.ok(start >= 0, "My Account renders a shared preferences card");
  const preferences = profile.slice(start, profile.indexOf("</section>", start) + "</section>".length);
  assert.match(preferences, /data-color-mode-toggle/);
  assert.match(preferences, /<partial name="_LanguagePicker"\s*\/>/);
  assert.ok(!profile.slice(0, start).includes('<partial name="_LanguagePicker"'),
    "the language setting does not float above account information");
  assert.match(layout, /_AniGoAccountPreferencesStyles\.cshtml/);
});

test("English and Filipino are accessible one-click choices with clear selection", () => {
  assert.match(picker, /method="post"/);
  assert.match(picker, /asp-controller="Language" asp-action="Select"/);
  assert.match(picker, /name="returnUrl"/);
  assert.match(picker, /name="language" value="en"/);
  assert.match(picker, /name="language" value="fil"/);
  assert.match(picker, /aria-pressed=/);
  assert.doesNotMatch(picker, /<select\b|Apply language/);
  assert.match(style, /\.ani-pref-language-choice\.is-selected/);
  assert.match(style, /@@media \(max-width: 640px\)/);
});

test("Choice posts preserve the existing validated browser culture cookie", () => {
  assert.match(controller, /language is not \("en" or "fil"\)/);
  assert.match(controller, /CookieRequestCultureProvider\.DefaultCookieName/);
  assert.match(controller, /Url\.IsLocalUrl\(returnUrl\)/);
  assert.match(filipino, /name="App preferences"/);
  assert.match(filipino, /name="Choose the language used throughout AniGo\."/);
});

test("Android preferences group both theme and radio language choices", () => {
  const start = androidAccount.indexOf("FarmSectionCard(\n            uiText(R.string.account_preferences_title");
  assert.ok(start >= 0, "Android account preferences card is present");
  const prefs = androidAccount.slice(start, androidAccount.indexOf("if (onReplayGuide", start));
  assert.match(prefs, /Switch\(/);
  assert.match(prefs, /LanguagePicker\(\)/);
  assert.doesNotMatch(androidAccount.slice(0, start), /LanguagePicker\(\)/);
});
