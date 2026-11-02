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
            document: { querySelector: () => null, querySelectorAll: selector => selector === 'form[data-confirm]' ? [form] : [] },
            window: { confirm: message => { assert.equal(message, 'Archive North farm?'); return approved; } }
        });
        assert.equal(typeof handler, 'function');
        handler({ preventDefault: () => { prevented = true; } });
        assert.equal(prevented, !approved);
    }
});

test('light and dark semantic colors preserve readable text and control boundaries', () => {
    const css = fs.readFileSync(require('node:path').join(__dirname, '../../src/SmartFarm.Web/wwwroot/css/site.css'), 'utf8');
    const luminance = hex => {
        hex = hex.slice(1);
        if (hex.length === 3) hex = [...hex].map(c => c + c).join('');
        const rgb = hex.match(/../g).map(c => {
            const value = parseInt(c, 16) / 255;
            return value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4;
        });
        return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
    };
    for (const root of css.matchAll(/:root\s*\{([^}]+)\}/g)) {
        const colors = Object.fromEntries([...root[1].matchAll(/--([\w-]+):\s*(#[\da-f]+)/gi)].map(m => [m[1], m[2]]));
        const contrast = (a, b) => (Math.max(luminance(colors[a]), luminance(colors[b])) + .05) /
            (Math.min(luminance(colors[a]), luminance(colors[b])) + .05);
        for (const [a, b] of [['text', 'surface'], ['muted', 'surface'], ['on-primary', 'primary'], ['danger', 'surface']])
            assert.ok(contrast(a, b) >= 4.5, `${a}/${b}: ${contrast(a, b)}`);
        assert.ok(contrast('outline', 'surface') >= 3);
        assert.ok(contrast('focus', 'surface') >= 3);
    }
});


test('copy code reports success and selects the code when clipboard access fails', async () => {
    for (const works of [true, false]) {
        let handler;
        let selected = false;
        let focused = false;
        const field = { value: 'ABCD12345678', focus: () => { focused = true; }, select: () => { selected = true; } };
        const status = { textContent: '' };
        const button = { dataset: { copyTarget: 'code', copyStatus: 'status' }, addEventListener: (_, callback) => { handler = callback; } };
        vm.runInNewContext(source, {
            document: {
                querySelector: () => null,
                querySelectorAll: selector => selector === '[data-copy-target]' ? [button] : [],
                getElementById: id => id === 'code' ? field : status
            },
            navigator: { clipboard: { writeText: async value => { assert.equal(value, 'ABCD12345678'); if (!works) throw new Error('denied'); } } }
        });
        assert.equal(typeof handler, 'function');
        await handler();
        assert.equal(selected, !works);
        assert.equal(focused, !works);
        assert.match(status.textContent, works ? /copied/i : /select.*copy/i);
    }
});
