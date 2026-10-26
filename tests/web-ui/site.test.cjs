const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync(require('node:path').join(__dirname, '../../src/SmartFarm.Web/wwwroot/js/site.js'), 'utf8');

test('consequential forms submit only after explicit confirmation', () => {
    for (const approved of [false, true]) {
        let handler;
        let prevented = false;
        const form = {
            dataset: { confirm: 'Archive North farm?' },
            addEventListener: (event, callback) => { if (event === 'submit') handler = callback; }
        };
        vm.runInNewContext(source, {
            document: { querySelector: () => null, querySelectorAll: () => [form] },
            window: { confirm: message => { assert.equal(message, 'Archive North farm?'); return approved; } }
        });
        assert.equal(typeof handler, 'function');
        handler({ preventDefault: () => { prevented = true; } });
        assert.equal(prevented, !approved);
    }
});
