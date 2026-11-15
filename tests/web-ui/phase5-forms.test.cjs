"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const root = path.resolve(__dirname, "../../src/SmartFarm.Web");
const read = file => fs.readFileSync(path.join(root, file), "utf8");

test("Phase 5 edit forms keep existing routes and optional settings", () => {
  const plan = read("Views/Crops/EditPlan.cshtml");
  const inventory = read("Views/Inventory/Edit.cshtml");
  const plot = read("Views/Farms/EditPlot.cshtml");
  const farm = read("Views/Farms/Create.cshtml");
  const tasks = read("Views/Tasks/Index.cshtml");
  const layout = read("Views/Shared/_Layout.cshtml");
  for (const file of [plan, inventory, plot, farm, tasks]) {
    assert.match(file, /data-ani-p5-form/, "opted-in form must have safe submit handling");
    assert.match(file, /method="post"/);
  }
  assert.match(plan, /data-ani-p5-start-date/);
  assert.match(plan, /data-ani-p5-end-date/);
  assert.match(plan, /data-ani-p5-date-error/);
  assert.match(plan, /<details class="sf-edit-disclosure ani-p5-optional"/);
  assert.match(plan, /Input\.WateringIntervalDays/);
  assert.match(inventory, /<details class="sf-edit-disclosure ani-p5-optional"/);
  assert.match(inventory, /Input\.ReorderLevel/);
  assert.match(plot, /Input\.RowVersion/);
  assert.match(tasks, /name="TaskType"/);
  assert.match(tasks, /name="AssignedToUserId"/);
  assert.match(layout, /if \(signedIn\)[\s\S]*forms-workflow\.js/);
});

function setup() {
  const handlers = {};
  const field = value => ({
    value, validationMessage: "", listeners: {},
    setCustomValidity(v) { this.validationMessage = v; },
    addEventListener(type, callback) { this.listeners[type] = callback; },
  });
  const planting = field("2026-11-20");
  const harvest = field("2026-11-19");
  const error = { textContent: "", hidden: true };
  const attrs = new Map();
  const form = {
    dataset: {}, listeners: {},
    querySelector(selector) {
      return { "[data-ani-p5-start-date]": planting,
        "[data-ani-p5-end-date]": harvest,
        "[data-ani-p5-date-error]": error }[selector] ?? null;
    },
    addEventListener(type, callback) { this.listeners[type] = callback; },
    setAttribute(k, v) { attrs.set(k, v); },
    removeAttribute(k) { attrs.delete(k); },
    checkValidity() { return !harvest.validationMessage; },
  };
  const window = { listeners: {}, addEventListener(type, callback) { this.listeners[type] = callback; } };
  vm.runInNewContext(read("wwwroot/js/forms-workflow.js"), {
    document: { querySelectorAll: () => [form] }, window,
  });
  return { form, window, harvest, planting, error, attrs };
}

test("invalid harvest date gives actionable feedback and blocks the save guard", () => {
  const { form, harvest, error } = setup();
  assert.equal(harvest.validationMessage, "Expected harvest must not be before planting.");
  assert.equal(error.hidden, false);
  const event = { defaultPrevented: false, preventDefault() { this.defaultPrevented = true; } };
  form.listeners.submit(event);
  assert.notEqual(form.dataset.aniP5Submitting, "true");
  harvest.value = "2026-11-22";
  harvest.listeners.input();
  assert.equal(error.hidden, true);
  assert.equal(harvest.validationMessage, "");
  form.listeners.submit(event);
  assert.equal(form.dataset.aniP5Submitting, "true");
});

test("second submission is prevented, but first named submitter remains enabled", () => {
  const { form, harvest, window, attrs } = setup();
  harvest.value = "";
  harvest.listeners.change();
  const named = { name: "approve", value: "true", disabled: false };
  let prevented = 0;
  const e = { defaultPrevented: false, submitter: named, preventDefault() { prevented++; } };
  form.listeners.submit(e);
  form.listeners.submit(e);
  assert.equal(prevented, 1);
  assert.equal(named.disabled, false);
  assert.equal(attrs.get("aria-busy"), "true");
  window.listeners.pageshow();
  assert.equal(attrs.has("aria-busy"), false);
  assert.equal(form.dataset.aniP5Submitting, undefined);
});
