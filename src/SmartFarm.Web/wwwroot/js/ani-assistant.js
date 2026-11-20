/* Presentation preferences contain no credentials or farm records. */
(() => {
    "use strict";
    const text = english => globalThis.anigoText?.[english] ?? english;
    const get = (store, key) => { try { return window[store].getItem(key); } catch { return null; } };
    const set = (store, key, value) => { try { window[store].setItem(key, value); } catch { /* storage can be disabled */ } };
    document.querySelectorAll("[data-ani-guide]").forEach(root => {
        const key = "anigo:guide:v1:" + root.dataset.aniKey;
        const content = root.querySelector("[data-ani-content]");
        const replay = root.querySelector("[data-ani-replay]");
        const hide = () => { content.hidden = true; replay.hidden = false; };
        if (get("localStorage", key) || get("sessionStorage", key)) hide();
        root.querySelector("[data-ani-hide]").onclick = () => { set("sessionStorage", key, "later"); hide(); };
        root.querySelector("[data-ani-dismiss]").onclick = () => { set("localStorage", key, "dismissed"); hide(); };
        root.querySelector("[data-ani-action]").addEventListener("click", () => { set("localStorage", key, "introduced"); });
        replay.onclick = () => { content.hidden = false; replay.hidden = true; };
    });
    const root = document.querySelector("[data-ani-alert]");
    if (!root) return;
    fetch("/Notifications/AniAlerts", { credentials: "same-origin", cache: "no-store", headers: { "Accept": "application/json" } })
        .then(r => { if (!r.ok) throw new Error(); return r.json(); })
        .then(page => {
            const alert = page.items.filter(a => a.farmId === root.dataset.farm && a.shouldInterrupt && !a.isStale && !a.readAtUtc && !a.acknowledgedAtUtc)
                .sort((a,b) => (b.severity || 0) - (a.severity || 0))
                .find(a => !get("sessionStorage", "anigo:alert:" + root.dataset.actor + ":" + a.id));
            if (!alert) return;
            const key = "anigo:alert:" + root.dataset.actor + ":" + alert.id;
            root.querySelector("[data-alert-title]").textContent = alert.title;
            root.querySelector("[data-alert-message]").textContent = alert.message;
            root.querySelector("[data-alert-source]").textContent = alert.source;
            root.querySelector("[data-alert-id]").value = alert.id;
            root.querySelector("[data-alert-open]").href = "/Weather?farmId=" + encodeURIComponent(alert.farmId);
            root.querySelector("[data-alert-later]").onclick = () => { set("sessionStorage", key, "later"); root.hidden = true; };
            root.querySelector("form").onsubmit = async e => {
                e.preventDefault(); const button = e.target.querySelector('[type="submit"]'); button.disabled = true;
                try {
                    const r = await fetch(e.target.action, { method: "POST", body: new FormData(e.target), credentials: "same-origin" });
                    if (r.status !== 204 || r.redirected) throw new Error();
                    set("sessionStorage", key, "acknowledged"); root.hidden = true;
                } catch { root.querySelector("[data-alert-status]").textContent = text("Could not acknowledge. Reconnect and try again."); }
                finally { button.disabled = false; }
            };
            root.hidden = false;
        }).catch(() => { /* Existing notification center and saved-weather state remain available. */ });
})();
