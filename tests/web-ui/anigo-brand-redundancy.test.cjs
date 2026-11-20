"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../..");
const read = file => fs.readFileSync(path.join(root, file), "utf8");

const web = "src/SmartFarm.Web/";
const android = "mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/";
const layout = read(web + "Views/Shared/_Layout.cshtml");
const brandCss = read(web + "Views/Shared/_AniGoBrandStyles.cshtml");
const login = read(web + "Views/Account/Login.cshtml");
const register = read(web + "Views/Account/Register.cshtml");
const landing = read(web + "Views/Home/Index.cshtml");
const photoAuth = read(android + "ui/components/PhotoAuthScaffold.kt");
const welcome = read(android + "ui/components/WelcomeScreen.kt");
const auth = read(android + "features/auth/AuthScreens.kt");

test("Web auth retains the single official guest wordmark in the shared header", () => {
  assert.match(layout, /anigo-guest-logo[^\n]*anigo-logo\.svg/);
  assert.match(layout, /aria-label="@T\["AniGo home"\]"/);
  for (const [name, screen] of [["login", login], ["registration", register]]) {
    assert.doesNotMatch(screen, /anigo-auth-brand|anigo-logo(-on-dark)?\.svg/, name + " must not display second full logo");
    assert.doesNotMatch(screen, /ANI GO\s*\//, name + " must not repeat the brand as a kicker");
  }
  assert.doesNotMatch(brandCss, /\.anigo-auth-brand/, "remove dead duplicate-logo CSS");
  assert.match(login, /<h1 id="login-heading">@T\["Log in"\]<\/h1>/);
  assert.match(register, /<h1 id="register-heading">@T\["Create your account"\]<\/h1>/);
  assert.match(login, /asp-action="Login"/);
  assert.match(register, /asp-action="Register"/);
});

test("Landing header owns the brand while hero content focuses on farm actions", () => {
  assert.match(landing, /anigo-footer-logo/, "footer keeps its well-separated official logo");
  assert.match(landing, /All rights reserved/, "copyright attribution remains");
  assert.match(landing, /How would you like to get started\?/);
  const hero = landing.split('<section class="ani6-hero"')[1].split("</section>")[0];
  assert.doesNotMatch(hero, /with AniGo|Get started with AniGo/,
    "hero should not repeat the guest-header wordmark");
  assert.match(hero, /Get started/);
  assert.match(landing, /What you can do with AniGo/,
    "retain accessible feature-section naming");
});

test("Signed-in web account navigation is context-first", () => {
  assert.match(layout, /<small>@T\["My account"\]<\/small>/);
  assert.doesNotMatch(layout, /My AniGo account/);
  assert.match(layout, /<title>@ViewData\["Title"\] - AniGo<\/title>/,
    "browser title keeps accessible application identification");
});

test("Android uses one accessible official full wordmark per photo-auth screen", () => {
  assert.match(photoAuth, /R\.drawable\.ani_go_logo_on_dark/);
  assert.match(photoAuth, /contentDescription = uiText\(R\.string\.\w+, "AniGo"\)/);
  assert.match(welcome, /headline = uiText\(R\.string\.\w+, "Your farm starts here\."\)/);
  for (const repeated of [
    'formTitle = "Log in to AniGo"',
    'headline = "Grow with AniGo."',
    'Text("New to AniGo?"',
    'title = { Text("Sign out of AniGo?") }',
    "Contact your AniGo system administrator"
  ]) {
    assert.ok(!auth.includes(repeated), "duplicate branding text remains: " + repeated);
  }
  assert.match(auth, /Email password reset is not available yet\./,
    "retain truthful account recovery notice");
  assert.match(auth, /Back to log in/, "preserve recovery navigation");
});
