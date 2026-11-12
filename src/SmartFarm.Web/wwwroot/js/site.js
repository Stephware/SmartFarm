// Open native disclosures and dialogs before focusing an error or deep-linked field.
const revealConcealedTarget = target => {
    const dialog = target.closest?.("dialog");
    if (dialog && !dialog.open) dialog.showModal();
    for (let parent = target.parentElement; parent; parent = parent.parentElement) {
        if (parent.tagName === "DETAILS") parent.open = true;
    }
};

const errorSummary = document.querySelector("[data-error-summary]");
if (errorSummary) {
    for (const link of errorSummary.querySelectorAll('a[href^="#"]')) {
        const field = document.getElementById(link.hash.slice(1));
        if (field && field.type !== "hidden") {
            field.setAttribute("aria-invalid", "true");
            link.addEventListener("click", () => {
                revealConcealedTarget(field);
                if (window.requestAnimationFrame) window.requestAnimationFrame(() => field.focus());
                else field.focus();
            });
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


// Appearance is independent from the operating system so the default stays light.
(() => {
    const toggles = document.querySelectorAll("[data-color-mode-toggle]");
    if (!toggles.length) return;
    const root = document.documentElement;
    const update = mode => {
        root.dataset.colorMode = mode;
        for (const toggle of toggles) {
            toggle.setAttribute("aria-pressed", String(mode === "dark"));
            toggle.setAttribute("aria-label", mode === "dark" ? "Use light appearance" : "Use dark appearance");
            const status = document.querySelector("[data-color-mode-status]");
            if (status) status.textContent = mode === "dark" ? "Dark appearance" : "Light appearance";
        }
    };
    update(root.dataset.colorMode === "dark" ? "dark" : "light");
    for (const toggle of toggles) toggle.addEventListener("click", () => {
        const mode = root.dataset.colorMode === "dark" ? "light" : "dark";
        update(mode);
        try { localStorage.setItem("anigo-color-mode", mode); } catch { /* Choice still works without storage. */ }
    });
})();

// Native dialogs retain the original POST forms, draft values and inline no-JS fallback.
(() => {
    const sections = document.querySelectorAll("section[data-form-dialog]");
    for (const section of typeof HTMLDialogElement === "undefined" ? [] : sections) {
        if (!section.querySelector("form")) continue;
        const heading = section.querySelector("h2");
        const triggers = Array.from(document.querySelectorAll("a[href]")).filter(a =>
            a.getAttribute("href") === "#" + section.id ||
            (heading?.id && a.getAttribute("href") === "#" + heading.id));
        if (!triggers.length) continue;
        const dialog = document.createElement("dialog");
        dialog.className = "reference-screen reference-form-dialog";
        if (heading) {
            if (!heading.id) heading.id = section.id + "-heading";
            dialog.setAttribute("aria-labelledby", heading.id);
        }
        const toolbar = document.createElement("div");
        toolbar.className = "dialog-toolbar";
        const close = document.createElement("button");
        close.type = "button";
        close.textContent = "×";
        close.setAttribute("aria-label", "Close " + (heading?.textContent.trim() || "form"));
        toolbar.append(close);
        dialog.append(toolbar);
        section.before(dialog);
        dialog.append(section);
        let opener;
        for (const trigger of triggers) {
            trigger.setAttribute("aria-haspopup", "dialog");
            trigger.addEventListener("click", event => {
                event.preventDefault();
                opener = trigger;
                if (!dialog.open) dialog.showModal();
            });
        }
        close.addEventListener("click", () => dialog.close());
        dialog.addEventListener("close", () => opener?.focus());
        if (section.querySelector('[aria-invalid="true"],.input-validation-error')) dialog.showModal();
    }
    const revealHash = () => {
        if (!window.location?.hash) return;
        let id;
        try { id = decodeURIComponent(window.location.hash.slice(1)); } catch { return; }
        const target = document.getElementById(id);
        if (!target) return;
        revealConcealedTarget(target);
    };
    window.addEventListener?.("hashchange", revealHash);
    revealHash();
})();
