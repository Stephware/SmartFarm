/* Optional password visibility and a clearer invalid-form focus.
   Native input validation and server antiforgery remain unchanged without JS. */
(() => {
    document.querySelectorAll('[data-password-toggle]').forEach(button => {
        const id = button.getAttribute('aria-controls');
        const field = id ? document.getElementById(id) : null;
        if (!(field instanceof HTMLInputElement) || field.type !== 'password') return;
        button.addEventListener('click', () => {
            const visible = field.type === 'password';
            field.type = visible ? 'text' : 'password';
            button.textContent = visible ? 'Hide' : 'Show';
            button.setAttribute('aria-pressed', String(visible));
            field.focus({ preventScroll: true });
        });
    });
    const summary = document.querySelector('[data-error-summary]');
    if (summary && document.querySelector('.auth-form')) {
        summary.focus({ preventScroll: true });
    }
})();
