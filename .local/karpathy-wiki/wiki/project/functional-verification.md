# Functional workflow and role verification

> Sources: SmartFarm live MVC/API, disposable SQL Server, Android MainActivity instrumentation, independent SQL checks
> Raw: [Functional E2E evidence](../../raw/project/2026-10-27-functional-e2e.md); [Current audit](../../raw/project/2026-11-11-documentation-audit.md)
> Updated: 2026-11-11


## Current interpretation

This is a historical verification record for the named October revision, not a
fresh acceptance run. Preserve its counts and limits. Current Android now has
farm home/report/finance insights and manager task forms; registration is simple
account signup followed by signed-in farm joining. The post-sync freshness and
memory-only session limits remain supported by current source. See
[Android](android.md), [progress](progress.md) and [known issues](known-issues.md).

> **Status: Outdated** (2026-11-11)
> Statements below about absent Android reports, then-current Git/branch state
> or keeping wiki/docs ignored describe historical context. Documentation is now
> Git-visible by explicit user instruction; old runtime results are not refreshed.


## Result

The implemented data workflow and tested permission boundaries passed. **Full UI
sign-off is incomplete:** Android inventory remains stale on the open screen after
sync, Android has no reports screen, and browser click-through was blocked by OS
Computer Use permissions. No application fixes were made during this test task.

Reviewed revision: `53d4b389b1c14a4d09f1a38c502c54a8b83e89f7`. Tests used a fresh
disposable SQL database and isolated Web/API hosts. The test database and hosts
were removed afterward; Git remained clean. Existing application data and schema
were not changed.

## Evidence and scope

| Surface | Checks actually executed | Result |
|---------|--------------------------|--------|
| Backend | Full suite with both SQL test connections configured; owner workflow plus enrollment/concurrency cases really ran | 307 tests passed, including five SQL cases |
| Web | Cookie sessions, actual MVC forms and anti-forgery tokens, rendered Razor pages, API reads independently checking persistence | 77 assertions passed across 308 HTTP requests |
| Android | MainActivity registration → farm → plot → crop type/plan/activation → monitoring → inventory → harvest; normal touch/text input, real Retrofit/Room/SQL | Live instrumentation passed; server totals verified |
| Android offline | Connectivity disabled; activity and observation saved locally; explicit upload after reconnect | Two queued writes appeared once on server; local queue emptied |
| Permission probes | Web forged requests with unchanged resources; Android UI gates and ten exact server rejections | Passed; denied farm access returns privacy-preserving HTTP404 |
| Web UI scripts | Existing Node tests | Two tests passed; these do not replace browser interaction |

Android Manager/Field Worker registration, approval, and promotion used real API
fixture setup; subsequent role navigation and field writes used the UI. The final
test used one Pixel_9 emulator with an Android beta image. Explicit
`uploadPending()` verified synchronization, not long-lived automatic scheduling.

## Permissions in the tested farm

| Action | Owner | Manager | Field Worker |
|--------|-------|---------|--------------|
| Farm/plot/crop management | Allowed | Allowed; Android management controls present | Blocked; Android controls absent |
| Activities, linked stock consumption, partial harvest | Allowed | Allowed | Allowed |
| Stock receipts/adjustments and harvest void | Allowed | Allowed; Android management gates checked | Blocked; forged requests rejected |
| Membership roles and enrollment decisions/code | Allowed | Blocked | Blocked |
| Web reports/dashboard/CSV | Allowed | Allowed | Allowed |
| Android reports/dashboard screen | Unavailable | Unavailable | Unavailable |

Web also rejected nonmember private reads/writes, missing anti-forgery tokens,
stale stock versions, insufficient stock, invalid consumption links, and writes
after crop completion. Readable history and void audit were retained.

## Data cross-check and observed issue

Independent SQL confirmed Web harvest totals of **14.625 kg**, with two valid and
two voided records. Android finished with **23.000 kg stock** and **5.000 kg
harvested across three valid entries**, with the crop still active. Membership
roles remained correct and forbidden Android setup writes were absent.

**Observed Android inventory display issue:** the screenshot taken after successful
upload and the empty-queue assertion still shows **24.000 kg**, while API/SQL
contains **23.000 kg**. `InventoryViewModel.movement()` queues the change and
updates its notice; sync completion removes pending rows without refreshing
`state.item` or invalidating its read cache. Online re-entry should fetch current
data according to source, but that display refresh was not runtime-asserted.
Follow-up: refresh the affected inventory state/cache when sync completes, then
verify both displayed stock and rowversion. Do not change stock accounting to fix
this display problem.

## Remaining limits and artifacts

Web checks did not execute browser JavaScript or visual interactions because
Computer Use permission was unavailable. Android reports are a feature gap.
Camera/photo selection, process restart, physical devices, long-lived background
jobs, load, and production deployment were outside this run.

Sanitized evidence: backend summary (`../../verification/2026-10-27-e2e/backend-summary.json`; local-only artifact),
Web report (`../../verification/2026-10-27-e2e/web-report.md`; local-only artifact),
Web assertions (`../../verification/2026-10-27-e2e/web-checks.json`; local-only artifact),
Android report (`../../verification/2026-10-27-e2e/android-report.md`; local-only artifact),
Android steps (`../../verification/2026-10-27-e2e/android-steps.txt`; local-only artifact),
post-sync Worker screenshot (`../../verification/2026-10-27-e2e/role-1-inventory.png`; local-only artifact).
Private test passwords, bearer/cookie tokens, and signing keys were excluded.

## See Also

[Known issues](known-issues.md), [Android](android.md), [Development](development.md),
[Features](features.md), [Progress](progress.md).
