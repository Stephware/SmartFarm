// Default to the reference's light workspace; an explicit choice persists per browser.
(() => {
    let mode = "light";
    try {
        if (localStorage.getItem("anigo-color-mode") === "dark") mode = "dark";
    } catch { /* Storage can be unavailable in private or restricted browsing. */ }
    document.documentElement.dataset.colorMode = mode;
})();
