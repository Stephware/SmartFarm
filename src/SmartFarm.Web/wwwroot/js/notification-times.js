(() => {
    "use strict";
    const times = new Map([...document.querySelectorAll("[data-notification-time]")].map(node => [node.dataset.notificationTime, node]));
    if (!times.size) return;
    const render = async () => {
        let page = 1, more = true;
        while (more && times.size) {
            const response = await fetch("/Notifications/AniAlerts?unread=false&page=" + page, { cache: "no-store", credentials: "same-origin", headers: { Accept: "application/json" } });
            if (!response.ok || response.redirected) throw new Error();
            const data = await response.json();
            for (const item of data.items) {
                const node = times.get(item.id); if (!node) continue;
                const zone = item.farmId ? data.farmTimeZones[item.farmId] : "Asia/Manila";
                if (!zone) continue;
                node.textContent = new Intl.DateTimeFormat(document.documentElement.lang, { timeZone: zone, dateStyle: "medium", timeStyle: "short" }).format(new Date(node.dateTime));
                times.delete(item.id);
            }
            more = data.hasMore === true; page++;
        }
    };
    render().catch(() => {}).finally(() => { for (const node of times.values()) node.textContent = node.dataset.unavailable; });
})();
