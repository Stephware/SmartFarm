/* Phase 4 — optional, progressive preview for authorized crop photos.
   Server MIME and size validation remains the source of truth. */
(() => {
    const types = new Set(["image/jpeg","image/png","image/webp"]);
    const maxBytes = 5 * 1024 * 1024;
    document.querySelectorAll('input[data-field-photo]').forEach(input => {
        const wrapper = input.closest('.ani-photo-attach');
        const preview = wrapper?.querySelector('.ani-photo-preview');
        const error = wrapper?.querySelector('.ani-photo-error');
        const form = input.closest('form');
        if (!wrapper || !preview || !error || !form) return;
        let activeUrl = null;
        const release = () => {
            if (activeUrl) URL.revokeObjectURL(activeUrl);
            activeUrl = null;
            preview.hidden = true;
            preview.removeAttribute('src');
        };
        const report = message => {
            error.textContent = message || '';
            error.hidden = !message;
        };
        const inspect = () => {
            release();
            report('');
            const file = input.files?.[0];
            if (!file) return false;
            if (!types.has(file.type)) {
                report('Choose a JPEG, PNG or WebP image.');
                return false;
            }
            if (file.size > maxBytes) {
                report('Photo is larger than 5 MB. Choose a smaller image.');
                return false;
            }
            if (file.size === 0) {
                report('This photo is empty. Choose a different image.');
                return false;
            }
            activeUrl = URL.createObjectURL(file);
            preview.src = activeUrl;
            preview.hidden = false;
            return true;
        };
        input.addEventListener('change', inspect);
        form.addEventListener('submit', event => {
            if (!inspect()) {
                event.preventDefault();
                input.focus();
            }
        });
        window.addEventListener('pagehide', release, { once: true });
    });
})();

// Enhance the same server-validated forms; without JavaScript their details remain available.
(() => {
    document.querySelectorAll('[data-inspection-form]').forEach(form => {
        const health = form.querySelector('[name="Observation.HealthStatus"]');
        const details = form.querySelector('[data-inspection-issues]');
        if (!health || !details) return;
        const fields = [...details.querySelectorAll('select')];
        const update = () => {
            const relevant = Number(health.value) >= 2 || fields.some(field => Number(field.value) !== 0);
            details.hidden = !relevant;
            if (relevant) details.open = true;
            fields.forEach(field => { field.disabled = !relevant; });
        };
        health.addEventListener('change', update);
        fields.forEach(field => field.addEventListener('change', update));
        update();
    });
    document.querySelectorAll('[data-activity-form]').forEach(form => {
        const selection = form.querySelector('[name="Supply.Selection"]');
        const fields = form.querySelector('[data-supply-fields]');
        const quantity = form.querySelector('[name="Supply.QuantityUsed"]');
        if (!selection || !fields || !quantity) return;
        const update = () => {
            const used = !!selection.value;
            fields.hidden = !used;
            fields.querySelectorAll('input').forEach(field => { field.disabled = !used; });
            quantity.required = used;
        };
        selection.addEventListener('change', update);
        update();
    });
})();
