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
