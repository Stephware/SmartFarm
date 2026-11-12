const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const script = fs.readFileSync(
    path.join(__dirname, "../../src/SmartFarm.Web/wwwroot/js/inventory-movement.js"), "utf8");

function field(value = "") {
    return {
        value, hidden: false, required: false, disabled: false, min: "", textContent: "",
        options: [], handlers: {},
        addEventListener(name, fn) { this.handlers[name] = fn; },
        fire(name) { this.handlers[name]?.(); }
    };
}
function setup(manager, hasActivity) {
    const kind = field(manager ? "2" : "3");
    const quantity = field();
    const activityField = field();
    const activity = field();
    activity.options = hasActivity ? [field(""), field("recorded-id")] : [field("")];
    const reasonField = field();
    const reason = field();
    const hint = field();
    const submit = field();
    const fields = {
        "[data-movement-kind]": kind,
        "[data-movement-quantity]": quantity,
        "[data-activity-field]": activityField,
        "[data-movement-activity]": activity,
        "[data-reason-field]": reasonField,
        "[data-movement-reason]": reason,
        "[data-movement-hint]": hint,
        "[data-movement-submit]": submit
    };
    const form = {querySelector: selector => fields[selector] || null};
    vm.runInNewContext(script, {
        document: { querySelector: selector => selector === "[data-inventory-movement-form]" ? form : null }
    });
    return {kind, quantity, activityField, activity, reasonField, reason, hint, submit};
}
test("receipt only asks for a positive quantity and doesn't require a crop activity", () => {
    const state = setup(true, false);
    assert.equal(state.activityField.hidden, true);
    assert.equal(state.reasonField.hidden, true);
    assert.equal(state.activity.required, false);
    assert.equal(state.submit.disabled, false);
    assert.equal(state.quantity.min, "0.001");
    assert.match(state.hint.textContent, /added/);
});
test("consumption requires an existing recorded crop activity", () => {
    const state = setup(false, true);
    assert.equal(state.activityField.hidden, false);
    assert.equal(state.activity.required, true);
    assert.equal(state.reasonField.hidden, true);
    assert.equal(state.quantity.min, "0.001");
    assert.equal(state.submit.disabled, false);
    assert.match(state.hint.textContent, /subtracted/);
});
test("no linked activity blocks misleading consumption while receipts remain available", () => {
    const state = setup(true, false);
    state.kind.value = "3";
    state.kind.fire("change");
    assert.equal(state.submit.disabled, true);
    assert.match(state.hint.textContent, /Record fieldwork first/);
    state.kind.value = "2";
    state.kind.fire("change");
    assert.equal(state.submit.disabled, false);
});
test("adjustments allow signed quantities and require a reason", () => {
    const state = setup(true, true);
    state.kind.value = "4";
    state.kind.fire("change");
    assert.equal(state.activityField.hidden, true);
    assert.equal(state.reasonField.hidden, false);
    assert.equal(state.reason.required, true);
    assert.equal(state.activity.required, false);
    assert.equal(state.quantity.min, "");
    assert.match(state.hint.textContent, /not the final stock balance/);
});
test("script has no effect on unrelated screens", () => {
    assert.doesNotThrow(() => vm.runInNewContext(script, {
        document: { querySelector: () => null }
    }));
});
