const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync(require('node:path').join(__dirname, '../../src/SmartFarm.Web/wwwroot/js/site.js'), 'utf8');

// Minimal browser globals are explicit so unrelated progressive modules can initialize.
const runScript = (script, context) => vm.runInNewContext(script, {
    ...context,
    window: { location: { hash: '' }, addEventListener: () => {}, ...(context.window || {}) }
});

test('consequential forms submit only after explicit confirmation', () => {
    for (const approved of [false, true]) {
        let handler;
        let prevented = false;
        const form = {
            dataset: { confirm: 'Archive North farm?' },
            addEventListener: (event, callback) => { if (event === 'submit') handler = callback; }
        };
        runScript(source, {
            document: { getElementById: () => null, querySelector: () => null, querySelectorAll: selector => selector === 'form[data-confirm]' ? [form] : [] },
            window: { confirm: message => { assert.equal(message, 'Archive North farm?'); return approved; } }
        });
        assert.equal(typeof handler, 'function');
        handler({ preventDefault: () => { prevented = true; } });
        assert.equal(prevented, !approved);
    }
});

test('light and dark semantic colors preserve readable text and control boundaries', () => {
    const path = require('node:path');
    const web = path.join(__dirname, '../../src/SmartFarm.Web');
    const layout = fs.readFileSync(path.join(web, 'Views/Shared/_Layout.cshtml'), 'utf8');
    const styles = [...layout.matchAll(/<link\b[^>]*rel="stylesheet"[^>]*href="~\/([^"?]+)"/g)]
        .map(match => match[1]);
    // Core theme first; narrowly scoped farmer operations may layer afterward.
    assert.equal(styles.at(-1), 'css/farmer-operations.css');
    assert.equal(styles.at(-2), 'css/reference-ui.css');
    const css = styles.map(file => fs.readFileSync(path.join(web, 'wwwroot', file), 'utf8'))
        .join('\n').replace(/\/\*[\s\S]*?\*\//g, '');
    const luminance = hex => {
        assert.match(hex, /^#[\da-f]{3}(?:[\da-f]{3})?$/i, 'semantic colors must resolve to opaque hex');
        hex = hex.slice(1);
        if (hex.length === 3) hex = [...hex].map(c => c + c).join('');
        const rgb = hex.match(/../g).map(c => {
            const value = parseInt(c, 16) / 255;
            return value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4;
        });
        return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
    };
    // Apply root overrides in source order, including partial theme blocks.
    const themes = { light: {}, dark: {} };
    const stack = [];
    let boundary = 0;
    let roots = 0;
    for (const brace of css.matchAll(/[{}]/g)) {
        const segment = css.slice(boundary, brace.index);
        if (brace[0] === '{') stack.push(segment.trim());
        else {
            if (/^(?::root|html)(?=\s|\[|,|$)/.test(stack.at(-1))) {
                roots++;
                const colors = Object.fromEntries([...segment.matchAll(/--([\w-]+):\s*([^;{}]+)/g)].map(m => [m[1], m[2].trim()]));
                const darkOnly = stack.some(selector => /prefers-color-scheme:\s*dark|data-color-mode\s*=\s*["']?dark/.test(selector));
                const lightOnly = stack.some(selector => /data-color-mode\s*=\s*["']?light/.test(selector));
                if (!lightOnly) Object.assign(themes.dark, colors);
                if (!darkOnly) Object.assign(themes.light, colors);
            }
            stack.pop();
        }
        boundary = brace.index + 1;
    }
    assert.ok(roots > 0, 'semantic root declarations are present');
    for (const [theme, colors] of Object.entries(themes)) {
        for (const token of ['text', 'surface', 'muted', 'on-primary', 'primary', 'danger', 'outline', 'focus'])
            assert.ok(colors[token], `${theme} is missing --${token}`);
        const value = token => {
            let color = colors[token];
            for (let depth = 0; color.startsWith('var(') && depth < 10; depth++) {
                const reference = /^var\(--([\w-]+)\)$/.exec(color);
                assert.ok(reference, `unsupported ${theme} color: ${color}`);
                color = colors[reference[1]];
                assert.ok(color, `${theme} is missing --${reference[1]}`);
            }
            return color;
        };
        const contrast = (a, b) => (Math.max(luminance(value(a)), luminance(value(b))) + .05) /
            (Math.min(luminance(value(a)), luminance(value(b))) + .05);
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
        runScript(source, {
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

function themePage(savedMode, storageFails = false) {
    const attributes = { 'aria-pressed': 'false' };
    const listeners = {};
    let stored = savedMode;
    const button = {
        addEventListener: (event, callback) => { listeners[event] = callback; },
        setAttribute: (name, value) => { attributes[name] = value; },
        getAttribute: name => attributes[name],
        querySelector: () => null,
        dataset: {},
    };
    const document = {
        documentElement: { dataset: {} },
        querySelector: () => null,
        querySelectorAll: selector => selector === '[data-color-mode-toggle]' ? [button] : [],
        getElementById: () => null,
    };
    const storage = {
        getItem: key => {
            assert.equal(key, 'anigo-color-mode');
            if (storageFails) throw new Error('storage unavailable');
            return stored;
        },
        setItem: (key, value) => {
            assert.equal(key, 'anigo-color-mode');
            if (storageFails) throw new Error('storage unavailable');
            stored = value;
        },
    };
    const context = {
        document, localStorage: storage,
        window: { localStorage: storage, matchMedia: () => ({ matches: true }) },
    };
    const init = fs.readFileSync(require('node:path').join(__dirname, '../../src/SmartFarm.Web/wwwroot/js/color-mode.js'), 'utf8');
    runScript(init, context);
    runScript(source, context);
    return { document, attributes, listeners, stored: () => stored };
}

test('appearance defaults to light even when the system prefers dark', () => {
    const page = themePage(null);
    assert.equal(page.document.documentElement.dataset.colorMode, 'light');
    assert.equal(page.attributes['aria-pressed'], 'false');
});

test('appearance button changes the rendered mode and remembers the explicit choice', () => {
    const page = themePage('dark');
    assert.equal(page.document.documentElement.dataset.colorMode, 'dark');
    assert.equal(page.attributes['aria-pressed'], 'true');
    assert.equal(typeof page.listeners.click, 'function');
    page.listeners.click();
    assert.equal(page.document.documentElement.dataset.colorMode, 'light');
    assert.equal(page.attributes['aria-pressed'], 'false');
    assert.equal(page.stored(), 'light');
    page.listeners.click();
    assert.equal(page.document.documentElement.dataset.colorMode, 'dark');
    assert.equal(page.attributes['aria-pressed'], 'true');
    assert.equal(page.stored(), 'dark');
});

test('appearance still works when browser storage is unavailable', () => {
    const page = themePage(null, true);
    assert.equal(page.document.documentElement.dataset.colorMode, 'light');
    page.listeners.click();
    assert.equal(page.document.documentElement.dataset.colorMode, 'dark');
    assert.equal(page.attributes['aria-pressed'], 'true');
});

test('initial and changed hash targets reveal native forms even without a same-page opener', () => {
    let opens = 0;
    const dialog = { open: false, showModal() { this.open = true; opens++; } };
    const target = { closest: selector => selector === 'dialog' ? dialog : null, parentElement: null };
    const events = {};
    runScript(source, {
        document: { querySelector: () => null, querySelectorAll: () => [], getElementById: id => id === 'add-task' ? target : null },
        window: { location: { hash: '#add-task' }, addEventListener: (type, callback) => { events[type] = callback; } }
    });
    assert.equal(dialog.open, true);
    dialog.open = false;
    events.hashchange();
    assert.equal(dialog.open, true);
    assert.equal(opens, 2);
});

test('crop calendar anchors open their full schedule without requiring dialog forms', () => {
    const disclosure = { tagName: 'DETAILS', open: false, parentElement: null };
    const target = { closest: () => null, parentElement: disclosure };
    runScript(source, {
        document: { querySelector: () => null, querySelectorAll: () => [], getElementById: id => id === 'agenda-date-2026-11-11' ? target : null },
        window: { location: { hash: '#agenda-date-2026-11-11' } }
    });
    assert.equal(disclosure.open, true);
});
