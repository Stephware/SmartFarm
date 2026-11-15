"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = p => fs.readFileSync(path.join(root, p), "utf8");

test("Phase 4 shares concise first-use guidance with a real action", () => {
  const partial = read("Views/Shared/_UiEmptyState.cshtml");
  assert.match(partial, /ani-p4-empty/);
  assert.match(partial, /Model\.Title/);
  assert.match(partial, /Model\.Description/);
  assert.match(partial, /Model\.ActionUrl/);
  assert.match(partial, /href="@Model\.ActionUrl"/);
});

test("large inventories show at most 25 rows per page and preserve search/filter scope", () => {
  const inventory = read("Views/Inventory/Index.cshtml");
  assert.match(inventory, /const int pageSize = 25/);
  assert.match(inventory, /visibleItems\.Skip\(/);
  assert.match(inventory, /foreach \(var item in pageItems\)/);
  assert.match(inventory, /asp-route-filter="@Model\.SelectedFilter"/);
  assert.match(inventory, /asp-route-search="@Model\.Search"/);
  assert.match(inventory, /asp-route-page="/);
  assert.match(inventory, /Model\.Workspace\.Items\.Count == 0/);
});

test("notifications use bounded, filter-preserving pages", () => {
  const notifications = read("Views/Notifications/Index.cshtml");
  assert.match(notifications, /const int pageSize = 20/);
  assert.match(notifications, /shown\.Skip\(/);
  assert.match(notifications, /foreach \(var notification in visibleNotifications\)/);
  assert.match(notifications, /asp-route-view="\@\(unreadOnly \? "unread" : null\)"/);
  assert.match(notifications, /asp-action="Read"/);
  assert.match(notifications, /name="returnView"/);
});

test("shared data-state styles do not alter public landing composition", () => {
  const source = read("Styles/anigo-data-states-phase4.css");
  const entry = read("Styles/anigo-tailwind.css");
  const bundle = read("wwwroot/css/anigo-tailwind.css");
  assert.match(entry, /@import "\.\/anigo-data-states-phase4\.css"/);
  assert.match(source, /body\.is-authenticated/);
  assert.doesNotMatch(source, /\.ani-entry-(?:hero|site|story)/);
  assert.match(bundle, /ani-p4-pager/);
});
