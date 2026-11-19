const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

const root = path.join(__dirname, "../../src/SmartFarm.Web");
const script = fs.readFileSync(path.join(root, "wwwroot/js/navigation-context.js"), "utf8");

function fakeStorage() {
    const data = new Map();
    return {
        data,
        getItem: key => data.get(key) ?? null,
        setItem: (key, value) => data.set(key, value),
        removeItem: key => data.delete(key)
    };
}

function mount(address, storage = fakeStorage(), options = {}) {
    const documentEvents = {};
    const windowEvents = {};
    const calls = [];
    const forms = options.forms ?? [];
    const current = new URL(address);
    const win = {
        location: { href: current.href, hash: current.hash },
        sessionStorage: storage,
        scrollY: options.y ?? 0,
        addEventListener: (type, cb) => { windowEvents[type] = cb; },
        requestAnimationFrame: cb => cb(),
        scrollTo: value => calls.push(value)
    };
    const doc = {
        addEventListener: (type, cb) => { documentEvents[type] = cb; },
        querySelector: selector => selector === "main" ? { contains: () => true } :
            selector.includes("aria-invalid") && options.hasError ? {} : null,
        querySelectorAll: selector => selector === "main form[method]" ? forms : []
    };
    vm.runInNewContext(script, { window: win, document: doc, URL, Date, JSON, Number });
    return { win, doc, documentEvents, windowEvents, calls, storage };
}

function clickLink(page, href, back = false, flags = {}) {
    const link = {
        href, target: "", hasAttribute: () => false,
        classList: { contains: name => back && name === "anigo-back-control" }
    };
    page.documentEvents.click({
        target: { closest: () => link },
        defaultPrevented: false,
        ...flags
    });
    return link;
}

test("same-page filter navigation restores the working scroll position", () => {
    const store = fakeStorage();
    const previous = mount("https://app.example/Tasks?farmId=one&view=all", store, { y: 680 });
    clickLink(previous, "https://app.example/Tasks?farmId=one&view=today");
    previous.windowEvents.pagehide();
    const next = mount("https://app.example/Tasks?farmId=one&view=today", store);
    next.windowEvents.pageshow({ persisted: false });
    assert.equal(next.calls.length, 1);
    assert.equal(next.calls[0].top, 680);
});

test("an explicit Back icon returns to its previously visited parent section", () => {
    const store = fakeStorage();
    const list = mount("https://app.example/Inventory/Details?farmId=one", store, { y: 540 });
    list.windowEvents.pagehide();
    const detail = mount("https://app.example/Inventory/Edit?farmId=one&itemId=x", store);
    clickLink(detail, "https://app.example/Inventory/Details?farmId=one", true);
    const resumed = mount("https://app.example/Inventory/Details?farmId=one", store);
    resumed.windowEvents.pageshow({ persisted: false });
    assert.equal(resumed.calls.length, 1);
    assert.equal(resumed.calls[0].top, 540);
});

test("Back from a detail restores the actual filtered parent URL", () => {
    const store = fakeStorage();
    const filtered = "https://app.example/Inventory?farmId=one&filter=low&page=2";
    const detailUrl = "https://app.example/Inventory/Details?farmId=one&itemId=x";
    const list = mount(filtered, store, { y: 800 });
    clickLink(list, detailUrl);
    list.windowEvents.pagehide();

    const detail = mount(detailUrl, store);
    const back = clickLink(detail, "https://app.example/Inventory?farmId=one", true);
    assert.equal(back.href, filtered);
    const returned = mount(filtered, store);
    returned.windowEvents.pageshow({ persisted: false });
    assert.equal(returned.calls[0].top, 800);
});

test("Back never reuses an unrelated farm's parent route", () => {
    const store = fakeStorage();
    const detailUrl = "https://app.example/Inventory/Details?farmId=one&itemId=x";
    const list = mount("https://app.example/Inventory?farmId=two&filter=low", store);
    clickLink(list, detailUrl);
    const detail = mount(detailUrl, store);
    const back = clickLink(detail, "https://app.example/Inventory?farmId=one", true);
    assert.equal(back.href, "https://app.example/Inventory?farmId=one");
});

test("same-route POST keeps its context across pagehide and successful redirect", () => {
    const storage = fakeStorage();
    let submit;
    const form = {
        method: "post",
        action: "https://app.example/Farms/Details?id=one",
        addEventListener: (_, cb) => { submit = cb; }
    };
    const view = mount("https://app.example/Farms/Details?id=one", storage, { y: 930, forms: [form] });
    submit({ defaultPrevented: false });
    view.windowEvents.pagehide();
    const returned = mount("https://app.example/Farms/Details?id=one", storage);
    returned.windowEvents.pageshow({ persisted: false });
    assert.equal(returned.calls[0].top, 930);
});

test("MVC POST redirect that normalizes query parameters restores the same resource", () => {
    const storage = fakeStorage();
    let submit;
    const form = {
        method: "post",
        action: "https://app.example/Notifications/Read",
        addEventListener: (_, cb) => { submit = cb; }
    };
    const source = mount("https://app.example/Notifications?page=1", storage, { y: 460, forms: [form] });
    submit({ defaultPrevented: false });
    source.windowEvents.pagehide();
    const result = mount("https://app.example/Notifications?view=all&page=1", storage);
    result.windowEvents.pageshow({ persisted: false });
    assert.equal(result.calls[0].top, 460);
});

test("pending POST state cannot restore a different farm's view", () => {
    const storage = fakeStorage();
    let submit;
    const form = {
        method: "post",
        action: "https://app.example/Farms/Update",
        addEventListener: (_, cb) => { submit = cb; }
    };
    const source = mount("https://app.example/Farms/Details?farmId=one", storage, { y: 460, forms: [form] });
    submit({ defaultPrevented: false });
    source.windowEvents.pagehide();
    const differentFarm = mount("https://app.example/Farms/Details?farmId=two", storage);
    differentFarm.windowEvents.pageshow({ persisted: false });
    assert.equal(differentFarm.calls.length, 0);
});

test("direct anchored navigation takes precedence over stored positions", () => {
    const storage = fakeStorage();
    const source = mount("https://app.example/Tasks?farmId=one", storage, { y: 500 });
    source.windowEvents.pagehide();
    const anchored = mount("https://app.example/Tasks?farmId=one#task-123", storage);
    anchored.windowEvents.pageshow({ persisted: false });
    assert.equal(anchored.calls.length, 0);
});

test("validation errors, cross-origin clicks and browser-restored pages do not force scroll", () => {
    const store = fakeStorage();
    const source = mount("https://app.example/Tasks?farmId=one&view=all", store, { y: 720 });
    clickLink(source, "https://other.example/Tasks?farmId=one");
    source.windowEvents.pagehide();
    assert.equal(store.data.size, 1);
    const page = mount("https://app.example/Tasks?farmId=one&view=all", store);
    page.windowEvents.pageshow({ persisted: false });
    assert.equal(page.calls.length, 0);
    const filtered = mount("https://app.example/Tasks?farmId=one&view=all", store);
    clickLink(filtered, "https://app.example/Tasks?farmId=one&view=mine");
    const errors = mount("https://app.example/Tasks?farmId=one&view=mine", store, { hasError: true });
    errors.windowEvents.pageshow({ persisted: false });
    assert.equal(errors.calls.length, 0);
});

test("Back links and the restoration script remain server-owned and accessible", () => {
    const views = [
        "Views/Shared/_UiPageHeader.cshtml",
        "Views/Farms/Create.cshtml",
        "Views/Farms/Edit.cshtml",
        "Views/Farms/Details.cshtml",
        "Views/Inventory/Details.cshtml",
        "Views/Monitoring/Index.cshtml",
        "Views/Harvests/Index.cshtml",
        "Views/Weather/Index.cshtml",
        "Views/Account/TwoFactor.cshtml",
        "Views/Notifications/Index.cshtml",
        "Views/Support/Details.cshtml",
        "Areas/Admin/Views/Announcements/Details.cshtml",
        "Areas/Admin/Views/Monitoring/Sync.cshtml",
        "Areas/Admin/Views/Security/Setup.cshtml",
        "Areas/Admin/Views/Users/Details.cshtml",
        "Areas/Admin/Views/Farms/Details.cshtml",
        "Areas/Admin/Views/Audit/Details.cshtml",
        "Areas/Admin/Views/Support/Details.cshtml"
    ];
    for (const view of views) {
        const html = fs.readFileSync(path.join(root, view), "utf8");
        assert.match(html, /anigo-back-control/, view);
        assert.match(html, /visually-hidden/, view);
        assert.match(html, /chevLeft/, view);
    }
    for (const layout of ["Views/Shared/_Layout.cshtml", "Areas/Admin/Views/Shared/_Layout.cshtml"]) {
        const html = fs.readFileSync(path.join(root, layout), "utf8");
        assert.match(html, /navigation-context\\.js/, layout);
    }
});
