// Build farmer-first navigation from the links rendered by the authorized farm page.
// Never construct farm routes in JavaScript: access and farm IDs stay server-owned.
(() => {
    const source = document.querySelector("main .farm-navigation");
    const desktop = document.getElementById("sidebar-farm-links");
    const group = document.getElementById("sidebar-farm-group");
    const mobile = document.getElementById("mobile-farm-links");
    const bottom = document.getElementById("mobile-workspace-links");
    const bar = document.getElementById("mobile-workspace-bar");
    const menu = document.getElementById("mobile-workspace-menu");
    const more = document.getElementById("mobile-more-button");

    // The original farm-navigation stays available as a no-JavaScript fallback.
    if (!source || !desktop || !group || !mobile || !bottom || !bar || !menu || !more) return;

    const links = new Map();
    for (const link of source.querySelectorAll("a[data-nav-section][href]")) {
        const section = link.dataset.navSection;
        if (section && !links.has(section)) links.set(section, link);
    }
    if (!links.size) return;

    const isCurrent = section => links.get(section)?.getAttribute("aria-current") === "page";
    const copy = (section, label) => {
        const original = links.get(section);
        if (!original) return null;
        const item = original.cloneNode(true);
        item.classList.add("nav-link");
        if (label) {
            const text = item.querySelector("span");
            if (text) text.textContent = label;
        }
        return item;
    };

    const direct = (container, section, label) => {
        const item = copy(section, label);
        if (item) container.append(item);
        return item;
    };

    const disclosure = (container, label, sections) => {
        const available = sections.filter(section => links.has(section));
        if (!available.length) return;
        const details = document.createElement("details");
        details.className = "farm-nav-disclosure";
        const summary = document.createElement("summary");
        summary.textContent = label;
        const items = document.createElement("div");
        items.className = "farm-nav-disclosure-links";
        for (const section of available) direct(items, section);
        if (available.some(isCurrent)) {
            details.open = true;
            details.classList.add("has-current");
        }
        details.append(summary, items);
        container.append(details);
    };

    // The primary sidebar stays short even for farms with every module enabled.
    direct(desktop, "dashboard");
    disclosure(desktop, "Fields & crops", ["crops", "plots"]);
    direct(desktop, "tasks");
    direct(desktop, "inventory");
    direct(desktop, "weather");
    direct(desktop, "reports");
    disclosure(desktop, "Farm management", ["farm", "finance"]);

    const mobileSection = (label, sections) => {
        const available = sections.filter(section => links.has(section));
        if (!available.length) return;
        const section = document.createElement("section");
        section.className = "mobile-menu-section";
        const title = document.createElement("h2");
        title.textContent = label;
        section.append(title);
        for (const item of available) direct(section, item);
        mobile.append(section);
    };
    mobileSection("Daily work", ["dashboard", "tasks", "crops"]);
    mobileSection("Farm tools", ["plots", "inventory", "weather"]);
    mobileSection("Records & management", ["reports", "finance", "farm"]);

    for (const section of ["dashboard", "tasks", "crops", "inventory"])
        direct(bottom, section, section === "dashboard" ? "Home" : undefined);

    // A secondary farm page stays visibly located under More on a phone.
    if (["plots", "weather", "reports", "farm", "finance"].some(isCurrent))
        more.dataset.hasCurrent = "true";

    const syncMenuState = () => more.setAttribute("aria-expanded", String(menu.open));
    more.addEventListener("click", () => {
        menu.open = !menu.open;
        syncMenuState();
        if (menu.open) menu.querySelector("summary")?.focus();
        else more.focus();
    });
    menu.addEventListener("toggle", syncMenuState);
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && menu.open) {
            menu.open = false;
            syncMenuState();
            more.focus();
        }
    });
    document.addEventListener("pointerdown", event => {
        if (menu.open && !menu.contains(event.target) && !more.contains(event.target)) {
            menu.open = false;
            syncMenuState();
        }
    });

    // Current farm and breadcrumbs derive from the same server-rendered links.
    const context = document.getElementById("sidebar-farm-context");
    if (context) context.hidden = false;
    const breadcrumb = document.getElementById("breadcrumb-farm");
    const breadcrumbLink = document.getElementById("breadcrumb-farm-link");
    const farmName = document.getElementById("sidebar-current-farm");
    const dashboard = links.get("dashboard");
    if (breadcrumb && breadcrumbLink && farmName && dashboard && !isCurrent("farm")) {
        breadcrumbLink.href = dashboard.href;
        breadcrumbLink.textContent = farmName.textContent.trim();
        breadcrumb.hidden = false;
    }

    group.hidden = false;
    mobile.hidden = false;
    bar.hidden = false;
    document.body.classList.add("farm-nav-enhanced");
})();
