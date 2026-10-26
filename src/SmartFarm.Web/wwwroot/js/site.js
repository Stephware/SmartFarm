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
