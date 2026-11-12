// Helps farmers understand which stock movement fields are required.
// The server remains authoritative for roles, linked crop activity, and balances.
(() => {
    const form = document.querySelector("[data-inventory-movement-form]");
    if (!form) return;
    const kind = form.querySelector("[data-movement-kind]");
    const quantity = form.querySelector("[data-movement-quantity]");
    const activityField = form.querySelector("[data-activity-field]");
    const activity = form.querySelector("[data-movement-activity]");
    const reasonField = form.querySelector("[data-reason-field]");
    const reason = form.querySelector("[data-movement-reason]");
    const hint = form.querySelector("[data-movement-hint]");
    const submit = form.querySelector("[data-movement-submit]");
    if (!kind || !quantity || !activityField || !activity || !reasonField || !reason || !hint || !submit) return;

    const update = () => {
        const used = kind.value === "3";
        const correction = kind.value === "4";
        activityField.hidden = !used;
        reasonField.hidden = !correction;
        activity.required = used;
        reason.required = correction;
        quantity.min = correction ? "" : "0.001";
        hint.textContent = used
            ? "Enter a positive amount actually used. It is subtracted from stock and must match a recorded crop activity."
            : correction
                ? "Enter a positive or negative correction (not the final stock balance). Add a reason for the change."
                : "Enter the positive quantity received. It will be added to the current stock balance.";
        const noRecordedActivity = used && activity.options.length < 2;
        submit.disabled = noRecordedActivity;
        if (noRecordedActivity) {
            hint.textContent += " Record fieldwork first to link the stock usage.";
        }
    };
    kind.addEventListener("change", update);
    update();
})();
