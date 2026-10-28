const errorSummary = document.querySelector("[data-error-summary]");
if (errorSummary) {
    for (const link of errorSummary.querySelectorAll('a[href^="#"]')) {
        const field = document.getElementById(link.hash.slice(1));
        if (field && field.type !== "hidden") {
            field.setAttribute("aria-invalid", "true");
            link.addEventListener("click", () => field.focus());
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
