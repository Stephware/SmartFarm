const errorSummary = document.querySelector("[data-error-summary]");
if (errorSummary) {
    for (const link of errorSummary.querySelectorAll('a[href^="#"]')) {
        const field = document.getElementById(link.hash.slice(1));
        if (field && field.type !== "hidden") {
            field.setAttribute("aria-invalid", "true");
            link.addEventListener("click", () => field.focus());
        } else {
            link.replaceWith(document.createTextNode(link.textContent));
        }
    }
    errorSummary.focus();
}

for (const form of document.querySelectorAll("form[data-confirm]")) {
    form.addEventListener("submit", event => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
}

for (const button of document.querySelectorAll("[data-copy-target]")) {
    button.addEventListener("click", async () => {
        const field = document.getElementById(button.dataset.copyTarget);
        const status = document.getElementById(button.dataset.copyStatus);
        try {
            await navigator.clipboard.writeText(field.value);
            status.textContent = "Farm code copied.";
        } catch {
            field.focus();
            field.select();
            status.textContent = "Select and copy the farm code.";
        }
    });
}


/* Harvest Gold + Pine navigation enhancement.
   The original _FarmNavigation is always usable without JavaScript. */
(() => {
    const icons = {
        farms: '<rect x="3" y="7" width="18" height="14" rx="2"/><path d="M3 12h18M7 7V4h10v3"/>',
        overview: '<path d="m3 11 9-8 9 8"/><path d="M5 10v11h14V10M9 21v-7h6v7"/>',
        dashboard: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
        tasks: '<rect x="4" y="4" width="16" height="17" rx="2"/><path d="m8 12 3 3 5-6"/>',
        crops: '<path d="M12 21v-9"/><path d="M12 14C5 14 4 9 4 6c6 0 8 3 8 8ZM12 12c0-5 3-8 8-8 0 4-1 8-8 8Z"/>',
        plots: '<path d="M3 7 9 3l6 4 6-4v14l-6 4-6-4-6 4V7Z"/><path d="M9 3v14m6-10v14"/>',
        finance: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 9h18"/><circle cx="12" cy="14" r="2"/>',
        inventory: '<path d="M3 7 12 3l9 4v11l-9 4-9-4Z"/><path d="m3 7 9 5 9-5M12 12v10"/>',
        weather: '<path d="M8 18H6a4 4 0 1 1 1.2-7.8A6 6 0 0 1 19 12a3 3 0 0 1 0 6H8Z"/>',
        reports: '<path d="M4 20V4m0 16h16M8 16v-5m5 5V7m5 9V9"/>',
        notifications: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4"/>',
        account: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
        more: '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>'
    };
    const farmIconNames = {
        "Farm overview": "overview", "Dashboard": "dashboard", "Tasks": "tasks",
        "Crops": "crops", "Plot layout": "plots", "Finances": "finance",
        "Inventory": "inventory", "Weather": "weather", "Reports": "reports"
    };
    const ns = "http://www.w3.org/2000/svg";
    const icon = name => {
        const svg = document.createElementNS(ns, "svg");
        svg.setAttribute("viewBox", "0 0 24 24");
        svg.setAttribute("fill", "none");
        svg.setAttribute("stroke", "currentColor");
        svg.setAttribute("stroke-width", "1.8");
        svg.setAttribute("stroke-linecap", "round");
        svg.setAttribute("stroke-linejoin", "round");
        svg.setAttribute("aria-hidden", "true");
        svg.setAttribute("focusable", "false");
        svg.classList.add("sf-nav-icon");
        svg.innerHTML = icons[name] || icons.overview;
        return svg;
    };
    const addIcon = (a, name) => {
        if (!a || a.querySelector(".sf-nav-icon")) return;
        a.prepend(icon(name));
    };
    for (const link of document.querySelectorAll("[data-nav-icon]")) {
        addIcon(link, link.dataset.navIcon);
    }

    const source = document.querySelector("main .farm-navigation");
    const desktop = document.getElementById("sidebar-farm-links");
    const sidebarGroup = document.getElementById("sidebar-farm-group");
    const mobile = document.getElementById("mobile-farm-links");
    const bottom = document.getElementById("mobile-workspace-links");
    const bottomBar = document.getElementById("mobile-workspace-bar");
    const menu = document.getElementById("mobile-workspace-menu");
    if (!source || !desktop || !sidebarGroup || !mobile || !bottom || !bottomBar || !menu) return;

    const links = Array.from(source.querySelectorAll("a[href]"));
    if (!links.length) return;
    const duplicate = (original, withIcon = true) => {
        const link = original.cloneNode(true);
        if (withIcon) addIcon(link, farmIconNames[original.textContent.trim()] || "overview");
        return link;
    };
    for (const link of links) {
        desktop.append(duplicate(link));
        mobile.append(duplicate(link, false));
    }
    const shortcuts = ["Dashboard", "Tasks", "Crops", "Inventory"];
    for (const label of shortcuts) {
        const original = links.find(link => link.textContent.trim() === label);
        if (original) bottom.append(duplicate(original));
    }
    const more = document.getElementById("mobile-more-button");
    if (more) {
        more.prepend(icon("more"));
        more.addEventListener("click", () => {
            menu.open = true;
            menu.querySelector("summary")?.focus();
        });
    }
    sidebarGroup.hidden = false;
    mobile.hidden = false;
    bottomBar.hidden = false;
    document.body.classList.add("farm-nav-enhanced");
})();
