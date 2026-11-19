// F0: restore context only when the farmer explicitly returns or updates the same view.
// Browser Back/Forward and #anchors retain native scrolling; no cross-farm/global resets.
(() => {
    "use strict";
    const prefix = "anigo:scroll:v1:";
    const lifetimeMs = 5 * 60 * 1000;
    const pendingPostKey = prefix + "pending-post";
    let storage;
    try { storage = window.sessionStorage; } catch { return; }
    if (!storage || !window.location || !window.addEventListener) return;

    const current = new URL(window.location.href);
    const key = url => prefix + url.pathname + url.search;
    const parse = url => {
        try {
            const raw = storage.getItem(key(url));
            const value = raw ? JSON.parse(raw) : null;
            return value && Number.isFinite(value.y) && value.expires > Date.now() ? value : null;
        } catch { return null; }
    };
    const store = (url, intent) => {
        if (url.origin !== current.origin || url.hash) return;
        try {
            storage.setItem(key(url), JSON.stringify({
                y: Math.max(0, window.scrollY || 0),
                intent,
                expires: Date.now() + lifetimeMs
            }));
        } catch { /* Private browsing or disabled storage: ordinary navigation still works. */ }
    };

    // A user choosing another filter on the same page stays at the working section.
    // A detail-page Back control can resume its previous parent when it was visited.
    document.addEventListener("click", event => {
        const link = event.target.closest?.("a[href]");
        if (!link || !document.querySelector("main")?.contains(link) ||
            link.target === "_blank" || link.hasAttribute("download") ||
            event.defaultPrevented || event.metaKey || event.ctrlKey ||
            event.shiftKey || event.altKey) return;
        let destination;
        try { destination = new URL(link.href, current); } catch { return; }
        if (destination.origin !== current.origin || destination.hash) return;
        if (link.classList.contains("anigo-back-control")) {
            const previous = parse(destination);
            if (previous) storePrevious(destination, previous, "back");
        } else if (destination.pathname === current.pathname &&
                   destination.search !== current.search) {
            store(destination, "filter");
        }
    });

    const storePrevious = (url, previous, intent) => {
        try { storage.setItem(key(url), JSON.stringify({ ...previous, intent })); }
        catch { /* Navigation works without storage. */ }
    };

    // A normal POST back to the same address should not jump to the top. Server
    // redirects to a different page keep their own intentional landing location.
    for (const form of document.querySelectorAll("main form[method]")) {
        form.addEventListener("submit", event => {
            if (event.defaultPrevented || form.method.toLowerCase() !== "post") return;
            let action;
            try { action = new URL(form.action || current.href, current); } catch { return; }
            // MVC often POSTS to a different action, then redirects to this same
            // view. Keep its previous position without guessing redirect targets.
            if (action.origin === current.origin) {
                store(current, "submit");
                // Redirects may normalize ?view or ?page; an exact query key
                // alone is insufficient for returning to the same work list.
                try {
                    storage.setItem(pendingPostKey, JSON.stringify({
                        url: current.href, y: Math.max(0, window.scrollY || 0),
                        expires: Date.now() + 2 * 60 * 1000
                    }));
                } catch { /* Ordinary form submission still works. */ }
            }
        });
    }

    // Preserve a snapshot for an explicit Back link to this same path/query.
    window.addEventListener("pagehide", () => {
        if (parse(current)?.intent !== "submit") store(current, "history");
    });

    window.addEventListener("pageshow", event => {
        if (event.persisted || window.location.hash) return;
        let previous = parse(current);
        let pending;
        try { pending = JSON.parse(storage.getItem(pendingPostKey) || "null"); }
        catch { /* malformed storage can be ignored */ }
        if (pending && pending.expires > Date.now()) {
            const origin = new URL(pending.url);
            const sameResource = ["farmId", "itemId", "cycleId", "id"].every(name =>
                origin.searchParams.get(name) === current.searchParams.get(name));
            if (origin.origin === current.origin && origin.pathname === current.pathname &&
                sameResource && (!previous || previous.intent === "history")) {
                previous = { y: pending.y, intent: "submit" };
            }
        }
        try { storage.removeItem(pendingPostKey); } catch { /* harmless */ }
        if (!previous || previous.intent === "history") return;
        // A validation error takes priority over restoring previous scroll.
        const summary = document.querySelector("[data-error-summary]");
        if (document.querySelector("[aria-invalid='true'], .input-validation-error") ||
            (summary && summary.textContent.trim())) return;
        try { storage.removeItem(key(current)); } catch { /* harmless */ }
        window.requestAnimationFrame(() => window.scrollTo({ top: previous.y, behavior: "instant" }));
    });
})();
