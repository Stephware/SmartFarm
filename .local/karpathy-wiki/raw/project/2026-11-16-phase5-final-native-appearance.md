# Phase 5 final native appearance and test strengthening

> Source: final native form source, rendered captures and emulator verification
> Collected: 2026-11-16
> Published: 2026-11-16

This supersedes only the final date-layout implementation and corresponding source
hashes in the earlier Phase 5 review snapshot; that immutable snapshot remains a
correct record of its earlier checks. No application capability or contract changed.

The first weighted-date fix separated labels but left the date on three lines in
the actual 320dp/160% capture. A stronger text-layout assertion reproduced that
failure. DateInput now uses the existing FlowRow layout to wrap the action below
the date when needed, with an 8dp horizontal or 4dp vertical gap. The selected date
fits one readable line in the regression; both texts stay within the button.
Formatting, ISO picker callback and content descriptions remain unchanged.

The overlong farm-name test now fills a valid location first, so its disabled-button
assertion cannot pass because another required field is empty. Existing scroll,
error-message and zero-submission assertions remain. Focused checks passed.

Final debug/release/device builds and 65 JVM tests passed. The complete final local
emulator suite passed all 150 tests without failures or skips after both changes.
The source for .NET/Node/Web is unchanged from the prior successful 600/74/806
checks. The final native capture was inspected; no screenshot/log/private artifact
is committed. Exact pushed-head CI/status belongs to PR #135. No merge is authorized;
UI Phase 6 landing and UI Phase 7 dark-palette plans remain reserved.
