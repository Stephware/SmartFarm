const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const code = fs.readFileSync('src/SmartFarm.Web/wwwroot/js/fieldwork.js', 'utf8');
const control = value => ({ value, disabled: false, required: false, events: {},
    addEventListener(name, fn) { this.events[name] = fn; } });

test('inspection hides irrelevant fields but preserves inconsistent drafts for correction', () => {
    const health = control('1'), category = control('0'), severity = control('0');
    const details = { hidden: false, open: false, querySelectorAll: () => [category, severity] };
    const form = { querySelector: s => s === '[data-inspection-issues]' ? details : health };
    vm.runInNewContext(code, { document: { querySelectorAll: s => s === '[data-inspection-form]' ? [form] : [] } });
    assert.equal(details.hidden, true);
    assert.equal(category.disabled, true);
    health.value = '2'; health.events.change();
    assert.equal(details.hidden, false); assert.equal(details.open, true);
    category.value = '1'; severity.value = '2';
    health.value = '1'; health.events.change();
    assert.equal(details.hidden, false, 'do not hide or erase an inconsistent draft');
    assert.equal(category.value, '1'); assert.equal(severity.value, '2');
});

test('supply quantity is required only for an explicitly selected supply', () => {
    const selection = control(''), quantity = control(''), reason = control('');
    const fields = { hidden: false, querySelectorAll: () => [quantity, reason] };
    const form = { querySelector: s => s === '[data-supply-fields]' ? fields : s === '[name="Supply.QuantityUsed"]' ? quantity : selection };
    vm.runInNewContext(code, { document: { querySelectorAll: s => s === '[data-activity-form]' ? [form] : [] } });
    assert.equal(fields.hidden, true); assert.equal(quantity.disabled, true);
    selection.value = 'item:version'; selection.events.change();
    assert.equal(fields.hidden, false); assert.equal(quantity.required, true);
    quantity.value = '1.250'; selection.value = ''; selection.events.change();
    assert.equal(quantity.required, false); assert.equal(quantity.disabled, true);
    assert.equal(quantity.value, '1.250', 'changing the choice must not erase the draft');
});
