/* Phase 5: progressive form improvements, no changes to server validation.
   Only forms opting in with data-ani-p5-form are enhanced. */
(() => {
    const forms = Array.from(document.querySelectorAll('form[data-ani-p5-form]'));
    const recover = () => {
        forms.forEach(form => {
            delete form.dataset.aniP5Submitting;
            form.removeAttribute('aria-busy');
        });
    };
    window.addEventListener('pageshow', recover);
    forms.forEach(form => {
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
            if (!form.checkValidity()) return; // Native/server validation remain authoritative.
            if (form.dataset.aniP5Submitting === 'true') {
                event.preventDefault();
                return;
            }
            form.dataset.aniP5Submitting = 'true';
            form.setAttribute('aria-busy', 'true');
            // Do not disable named submitters: their name/value may be required by ASP.NET MVC.
        });
    });
})();
