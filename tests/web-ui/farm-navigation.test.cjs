const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const root = path.join(__dirname, '../../src/SmartFarm.Web');
const script = fs.readFileSync(path.join(root, 'wwwroot/js/farm-navigation.js'), 'utf8');
const razor = fs.readFileSync(path.join(root, 'Views/Shared/_FarmNavigation.cshtml'), 'utf8');

function node(tag = 'div') {
    const listeners = {};
    const el = {
        tagName: tag.toUpperCase(), children: [], attributes: {}, dataset: {}, hidden: false,
        open: false, textContent: '', href: '', className: '', listeners,
        classList: { classes: new Set(), add(name) { this.classes.add(name); } },
        append(...items) { this.children.push(...items); },
        querySelector(selector) {
            if (selector === 'span' || selector === 'summary') {
                return this.children.find(child => child.tagName === selector.toUpperCase()) || null;
            }
            return null;
        },
        setAttribute(name, value) { this.attributes[name] = value; },
        getAttribute(name) { return this.attributes[name] ?? null; },
        addEventListener(name, callback) { listeners[name] = callback; },
        fire(name, value) { listeners[name]?.(value); },
        focus() { this.focused = true; },
        contains(other) {
            return this === other || this.children.some(child => child.contains?.(other));
        }
    };
    return el;
}
const labels = { dashboard: 'Today', crops: 'Crops & Calendar', inventory: 'Supplies', reports: 'Harvests', farm: 'Farm & team' };
function link(section, href, current = null) {
    const a = node('a');
    a.dataset.navSection = section;
    a.href = href;
    const text = node('span');
    text.textContent = labels[section] || section;
    a.append(text);
    if (current) a.setAttribute('aria-current', current);
    a.cloneNode = () => link(section, href, current);
    return a;
}
function flatten(node, result = []) {
    if (node.tagName === 'A') result.push(node);
    for (const child of node.children) flatten(child, result);
    return result;
}
function navigationPage(sections, active = 'dashboard') {
    const ids = ['sidebar-farm-links', 'sidebar-farm-group', 'mobile-farm-links',
        'mobile-workspace-links', 'mobile-workspace-bar', 'mobile-workspace-menu',
        'mobile-more-button', 'sidebar-farm-context', 'sidebar-current-farm',
        'breadcrumb-farm', 'breadcrumb-farm-link'];
    const controls = Object.fromEntries(ids.map(id => [id, node()]));
    for (const id of ['sidebar-farm-group', 'mobile-farm-links',
        'mobile-workspace-bar', 'sidebar-farm-context', 'breadcrumb-farm'])
        controls[id].hidden = true;
    controls['sidebar-current-farm'].textContent = 'North Demo Farm';
    controls['mobile-workspace-menu'].append(node('summary'));
    const events = {};
    const classes = new Set();
    const urls = Object.fromEntries(sections.map(section =>
        [section, '/farm/' + section + '?farmId=north-farm']));
    const source = { querySelectorAll: () => sections.map(section =>
        link(section, urls[section], active === section ? 'page' : null)) };
    vm.runInNewContext(script, {
        document: {
            querySelector: selector => selector === 'main .farm-navigation' ? source : null,
            getElementById: id => controls[id] ?? null,
            createElement: tag => node(tag),
            addEventListener(name, callback) { events[name] = callback; },
            body: { classList: { add: name => classes.add(name) } }
        }
    });
    return { controls, events, classes, urls };
}

test('server farm links remain unique and contain a harvest-report destination', () => {
    const sections = [...razor.matchAll(/data-nav-section="([^"]+)"/g)].map(match => match[1]);
    assert.equal(sections.length, new Set(sections).size);
    assert.deepEqual(sections, ['dashboard', 'crops', 'inventory', 'reports', 'farm']);
    for (const label of ['Today', 'Crops &amp; Calendar', 'Supplies', 'Harvests']) assert.ok(razor.includes(label));
    assert.ok(!sections.includes('harvests'), 'same report must not appear twice');
    assert.match(razor, /asp-controller="Reports"\s+asp-action="Harvests"/);
});

test('desktop exposes four primary sections and secondary farm settings with server-owned URLs', () => {
    const sections = ['dashboard', 'crops', 'inventory', 'reports', 'farm'];
    const { controls, classes, urls } = navigationPage(sections, 'crops');
    const desktop = controls['sidebar-farm-links'];
    const anchors = flatten(desktop);
    assert.deepEqual(anchors.map(a => a.dataset.navSection), sections);
    assert.equal(desktop.children.filter(element => element.tagName === 'DETAILS').length, 0);
    assert.ok(anchors.every(a => a.href === urls[a.dataset.navSection]));
    assert.equal(anchors.find(a => a.dataset.navSection === 'crops').getAttribute('aria-current'), 'page');
    assert.equal(controls['sidebar-farm-context'].hidden, false);
    assert.equal(controls['breadcrumb-farm'].hidden, false);
    assert.equal(controls['breadcrumb-farm-link'].href, urls.dashboard);
    assert.equal(controls['breadcrumb-farm-link'].textContent, 'North Demo Farm');
    assert.ok(classes.has('farm-nav-enhanced'));
});

test('farm overview has no duplicated farm-name breadcrumb', () => {
    const { controls } = navigationPage(['dashboard', 'farm'], 'farm');
    assert.equal(controls['breadcrumb-farm'].hidden, true);
    assert.equal(controls['sidebar-farm-context'].hidden, false);
});

test('mobile bar offers all four MVP sections and Harvests stays visibly active', () => {
    const sections = ['dashboard', 'crops', 'inventory', 'reports', 'farm'];
    const { controls, urls } = navigationPage(sections, 'reports');
    const bottom = flatten(controls['mobile-workspace-links']);
    assert.deepEqual(bottom.map(a => a.dataset.navSection), ['dashboard', 'crops', 'inventory', 'reports']);
    assert.deepEqual(bottom.map(a => a.querySelector('span').textContent), ['Today', 'Crops & Calendar', 'Supplies', 'Harvests']);
    assert.ok(bottom.every(a => a.href === urls[a.dataset.navSection]));
    assert.equal(bottom[3].getAttribute('aria-current'), 'page');
    assert.notEqual(controls['mobile-more-button'].dataset.hasCurrent, 'true');
    assert.deepEqual(flatten(controls['mobile-farm-links']).map(a => a.dataset.navSection), ['farm']);
    assert.equal(controls['mobile-workspace-bar'].hidden, false);
});

test('secondary farm settings highlight More without becoming a fifth primary destination', () => {
    const { controls } = navigationPage(['dashboard', 'crops', 'inventory', 'reports', 'farm'], 'farm');
    assert.equal(controls['mobile-more-button'].dataset.hasCurrent, 'true');
    assert.equal(flatten(controls['mobile-workspace-links']).length, 4);
    assert.equal(flatten(controls['mobile-farm-links'])[0].getAttribute('aria-current'), 'page');
});

test('enhancement never invents sections not supplied by the server', () => {
    const only = ['dashboard', 'crops'];
    const { controls, urls } = navigationPage(only, 'crops');
    for (const id of ['sidebar-farm-links', 'mobile-workspace-links']) {
        const anchors = flatten(controls[id]);
        assert.ok(anchors.length > 0);
        assert.ok(anchors.every(a => only.includes(a.dataset.navSection)));
        assert.ok(anchors.every(a => a.href === urls[a.dataset.navSection]));
    }
    const bottom = flatten(controls['mobile-workspace-links']);
    assert.equal(bottom.find(a => a.dataset.navSection === 'crops').getAttribute('aria-current'), 'page');
});

test('mobile More toggles, Escape restores focus, and outside tap closes the menu', () => {
    const { controls, events } = navigationPage(['dashboard', 'tasks', 'crops'], 'dashboard');
    const menu = controls['mobile-workspace-menu'];
    const button = controls['mobile-more-button'];
    button.fire('click');
    assert.equal(menu.open, true);
    assert.equal(button.attributes['aria-expanded'], 'true');
    assert.equal(menu.querySelector('summary').focused, true);
    events.keydown({ key: 'Escape' });
    assert.equal(menu.open, false);
    assert.equal(button.attributes['aria-expanded'], 'false');
    assert.equal(button.focused, true);
    button.fire('click');
    events.pointerdown({ target: node('div') });
    assert.equal(menu.open, false);
    assert.equal(button.attributes['aria-expanded'], 'false');
});
