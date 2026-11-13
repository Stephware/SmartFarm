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
    const direct = (container, section) => {
        const original = links.get(section);
        if (!original) return null;
        const item = original.cloneNode(true);
        item.classList.add("nav-link");
        container.append(item);
        return item;
    };

    // Four daily destinations; settings stay secondary.
    for (const section of ["dashboard", "crops", "inventory", "reports"]) {
        direct(desktop, section);
        direct(bottom, section);
    }
    direct(desktop, "farm")?.classList.add("farm-nav-secondary");
    direct(mobile, "farm");
    if (isCurrent("farm")) more.dataset.hasCurrent = "true";

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
