// Native POST safety is shared by farmer, account and administrative pages.
// Business validation, antiforgery and named submitter values remain server-owned.
(() => {
    "use strict";
    const text = english => globalThis.anigoText?.[english] ?? english;
    const pending = new Set();
    const controls = form => [...form.querySelectorAll('button[type=submit],button:not([type]),input[type=submit]')];
    const restore = form => {
        pending.delete(form);
        form.removeAttribute("aria-busy");
        for (const button of controls(form)) button.removeAttribute("aria-disabled");
    };
    // Keep drafts only in the current document. Never copy credentials, notes or photos to storage.
    const forms = [...document.querySelectorAll('main form[method="post" i]')];
    const fields = form => [...form.elements].filter(field =>
        field.matches('input:not([type=hidden]):not([type=submit]):not([type=button]),select,textarea') &&
        !field.disabled && !field.readOnly);
    const snapshot = form => JSON.stringify(fields(form).map(field =>
        [field.name, /^(checkbox|radio)$/.test(field.type) ? field.checked :
            field.type === "file" ? [...field.files].map(file => [file.name, file.size, file.lastModified]) : field.value]));
    // Search/preferences and login challenges do not represent unfinished farm records.
    const drafts = new Map(forms.filter(form => fields(form).length &&
        !form.querySelector('input[type=password],input[autocomplete=one-time-code]') &&
        !form.closest('[data-language-picker]') &&
        !/\/(?:SetLanguage|Login|ForgotPassword|TwoFactor|RecoveryCode)(?:\?|$)/i.test(form.action))
        .map(form => [form, snapshot(form)]));
    const dirty = form => drafts.has(form) && snapshot(form) !== drafts.get(form);

    // Bubble after field-specific validation and AJAX handlers have had a chance to cancel.
    document.addEventListener("submit", event => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || event.defaultPrevented || form.method !== "post" ||
            form.getAttribute("method")?.toLowerCase() === "dialog") return;
        if (pending.has(form)) { event.preventDefault(); return; }
        if ([...drafts.keys()].some(other => other !== form && dirty(other)) &&
            !window.confirm(text("Leave this page and discard your other unfinished changes?"))) {
            event.preventDefault();
            // Phase-5 forms may already have set their local pending state.
            delete form.dataset.aniP5Submitting;
            restore(form);
            return;
        }
        pending.add(form);
        form.setAttribute("aria-busy", "true");
        // aria-disabled keeps the submitter in the POST payload. The submit guard enforces it.
        for (const button of controls(form)) button.setAttribute("aria-disabled", "true");
    });
    window.addEventListener("beforeunload", event => {
        if (pending.size || ![...drafts.keys()].some(dirty)) return;
        event.preventDefault();
        event.returnValue = "";
    });
    window.addEventListener("pageshow", () => { for (const form of [...pending]) restore(form); });
})();
