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
function link(section, href, current = null) {
    const a = node('a');
    a.dataset.navSection = section;
    a.href = href;
    const text = node('span');
    text.textContent = section;
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
    assert.ok(sections.includes('dashboard'));
    assert.ok(sections.includes('reports'));
    assert.ok(!sections.includes('harvests'), 'same report must not appear twice');
    assert.match(razor, /asp-controller="Reports"\s+asp-action="Harvests"/);
});

test('desktop menu groups sections and retains original server-owned destinations', () => {
    const sections = ['dashboard', 'crops', 'plots', 'tasks', 'inventory',
        'weather', 'reports', 'finance', 'farm'];
    const { controls, classes, urls } = navigationPage(sections, 'plots');
    const desktop = controls['sidebar-farm-links'];
    assert.equal(desktop.children.length, 7, 'short primary navigation with two groups');
    const grouped = desktop.children.filter(element => element.tagName === 'DETAILS');
    assert.equal(grouped.length, 2);
    assert.equal(grouped[0].open, true, 'active crop/plot page expands Fields & crops');
    assert.equal(grouped[1].open, false);
    const anchors = flatten(desktop);
    assert.deepEqual(anchors.map(a => a.dataset.navSection).sort(), sections.slice().sort());
    assert.ok(anchors.every(a => a.href === urls[a.dataset.navSection]));
    assert.equal(anchors.find(a => a.dataset.navSection === 'plots').getAttribute('aria-current'), 'page');
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

test('mobile primary bar has four tasks and secondary pages highlight More', () => {
    const sections = ['dashboard', 'tasks', 'crops', 'inventory', 'reports', 'weather'];
    const { controls, urls } = navigationPage(sections, 'reports');
    const bottom = flatten(controls['mobile-workspace-links']);
    assert.deepEqual(bottom.map(a => a.dataset.navSection),
        ['dashboard', 'tasks', 'crops', 'inventory']);
    assert.ok(bottom.every(a => a.href === urls[a.dataset.navSection]));
    assert.equal(bottom[0].querySelector('span').textContent, 'Home');
    assert.equal(controls['mobile-more-button'].dataset.hasCurrent, 'true');
    const menu = controls['mobile-farm-links'];
    assert.equal(menu.children.length, 3);
    assert.deepEqual(flatten(menu).map(a => a.dataset.navSection),
        ['dashboard', 'tasks', 'crops', 'inventory', 'weather', 'reports']);
    assert.equal(controls['mobile-workspace-bar'].hidden, false);
});

test('enhancement never invents sections not supplied by the server', () => {
    const only = ['dashboard', 'tasks'];
    const { controls, urls } = navigationPage(only, 'tasks');
    for (const id of ['sidebar-farm-links', 'mobile-farm-links', 'mobile-workspace-links']) {
        const anchors = flatten(controls[id]);
        assert.ok(anchors.length > 0);
        assert.ok(anchors.every(a => only.includes(a.dataset.navSection)));
        assert.ok(anchors.every(a => a.href === urls[a.dataset.navSection]));
    }
    const bottom = flatten(controls['mobile-workspace-links']);
    assert.equal(bottom.find(a => a.dataset.navSection === 'tasks').getAttribute('aria-current'), 'page');
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
