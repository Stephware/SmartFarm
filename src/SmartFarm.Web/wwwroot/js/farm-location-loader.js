/* The optional farm map is only loaded after its disclosure is opened.
   No Leaflet, map tiles or location requests are needed for basic farm details. */
(() => {
    "use strict";
    const pickers = [...document.querySelectorAll("[data-farm-location-picker]")];
    if (!pickers.length) return;

    let resources = null;
    const leafletCss = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
    const leafletJs = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";

    const script = (url, integrity) => new Promise((resolve, reject) => {
        const element = document.createElement("script");
        element.src = url;
        if (integrity) {
            element.integrity = integrity;
            element.crossOrigin = "anonymous";
        }
        element.addEventListener("load", resolve, { once: true });
        element.addEventListener("error", reject, { once: true });
        document.body.append(element);
    });

    const start = () => {
        if (resources) return resources;
        resources = (async () => {
            if (!window.L) {
                const sheet = document.createElement("link");
                sheet.rel = "stylesheet";
                sheet.href = leafletCss;
                sheet.integrity = "sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=";
                sheet.crossOrigin = "anonymous";
                document.head.append(sheet);
                await script(leafletJs,
                    "sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=");
            }
            if (!window.L) throw new Error("Mapping library unavailable");
            // The picker attaches its handlers only after Leaflet has loaded.
            const pickerUrl = pickers[0].dataset.pickerScriptUrl;
            if (!pickerUrl) throw new Error("Missing picker script");
            await script(pickerUrl);
        })().catch(error => {
            // A transient CDN failure should not block a later deliberate retry.
            resources = null;
            throw error;
        });
        return resources;
    };

    for (const root of pickers) {
        const disclosure = root.closest("details");
        const status = root.querySelector("[data-farm-location-status]");
        const openMap = () => {
            if (disclosure && !disclosure.open) return;
            if (status) status.textContent = "Loading the optional farm map...";
            start().catch(() => {
                if (status) status.textContent =
                    "Map could not load. You can still save the farm name and nearby location.";
            });
        };
        disclosure?.addEventListener("toggle", openMap);
        if (!disclosure || disclosure.open) openMap();
    }
})();
