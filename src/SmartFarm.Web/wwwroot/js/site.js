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
        if (section.id === "create-farm" || section.id === "join-farm") {
            dialog.classList.add("ani-farm-entry-dialog");
        }
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
        const form = section.querySelector("form");
        const snapshot = () => JSON.stringify(Array.from(form.querySelectorAll("input,select,textarea"))
            .filter(field => field.type !== "hidden" && field.type !== "submit" && field.type !== "button")
            .map(field => [field.name || field.id, field.type === "checkbox" ? field.checked : field.value]));
        let initial = snapshot();
        const isDirty = () => snapshot() !== initial;
        const askDiscard = () => !isDirty() || window.confirm("Discard the unfinished changes in this form?");
        const dismiss = () => { if (askDiscard()) dialog.close(); };
        let opener;
        for (const trigger of triggers) {
            trigger.setAttribute("aria-haspopup", "dialog");
            trigger.addEventListener("click", event => {
                event.preventDefault();
                opener = trigger;
                if (!dialog.open) {
                    initial = snapshot();
                    dialog.showModal();
                    // Focus the form field rather than the Close icon.
                    const first = form.querySelector("input:not([type=hidden]),select,textarea");
                    if (first) first.focus({ preventScroll: true });
                }
            });
        }
        close.addEventListener("click", dismiss);
        dialog.addEventListener("cancel", event => { if (!askDiscard()) event.preventDefault(); });
        dialog.addEventListener("click", event => {
            if (event.target === dialog) dismiss();
        });
        dialog.addEventListener("close", () => opener?.focus());
        if (section.querySelector('[aria-invalid="true"],.input-validation-error,[data-form-error]')) {
            dialog.showModal();
            form.querySelector("[aria-invalid='true'],.input-validation-error,input:not([type=hidden])")?.focus({ preventScroll: true });
        }
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

/* Phase 5: progressive form improvements, no changes to server validation.
   Only forms opting in with data-ani-p5-form are enhanced. */
(() => {
    const forms = Array.from(document.querySelectorAll('form[data-ani-p5-form]'));
    const recover = () => {
        forms.forEach(form => {
            delete form.dataset.aniP5Submitting;
            form.removeAttribute('aria-busy');
            const progress = form.querySelector('[data-ani-form-progress]');
            if (progress) progress.textContent = '';
        });
    };
    window.addEventListener('pageshow', recover);
    forms.forEach(form => {
        form.addEventListener('invalid', event => revealConcealedTarget(event.target), true);
        const planting = form.querySelector('[data-ani-p5-start-date]');
        const harvest = form.querySelector('[data-ani-p5-end-date]');
        const dateError = form.querySelector('[data-ani-p5-date-error]');
        const checkDates = () => {
            if (!planting || !harvest) return;
            const invalid = Boolean(planting.value && harvest.value && harvest.value < planting.value);
            harvest.setCustomValidity(invalid ? 'Expected harvest must not be before planting.' : '');
            if (dateError) {
                dateError.textContent = invalid ? harvest.validationMessage : '';
                dateError.hidden = !invalid;
            }
        };
        planting?.addEventListener('change', checkDates);
        harvest?.addEventListener('change', checkDates);
        planting?.addEventListener('input', checkDates);
        harvest?.addEventListener('input', checkDates);
        checkDates();
        form.addEventListener('submit', event => {
            if (event.defaultPrevented) return; // Respect confirmation cancellations.
            checkDates();
            if (!form.checkValidity()) {
                event.preventDefault();
                form.reportValidity();
                return;
            }
            if (form.dataset.aniP5Submitting === 'true') {
                event.preventDefault();
                return;
            }
            form.dataset.aniP5Submitting = 'true';
            form.setAttribute('aria-busy', 'true');
            const progress = form.querySelector('[data-ani-form-progress]');
            if (progress) progress.textContent = 'Saving your changes…';
            // Do not disable named submitters: their name/value may be required by ASP.NET MVC.
        });
    });
})();
