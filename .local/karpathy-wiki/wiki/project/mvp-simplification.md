# Farmer-first MVP simplification — target state

> Sources: approved MVP brief and SmartFarm dev source, 2026-11-13
> Raw: [Stage 0 source and regression evidence](../../raw/project/2026-11-13-mvp-stage-0.md), [Stage 1 MVC evidence](../../raw/project/2026-11-13-mvp-stage-1.md), [Stage 2 Android evidence](../../raw/project/2026-11-13-mvp-stage-2.md), [Stage 3 recording evidence](../../raw/project/2026-11-13-mvp-stage-3.md), [CI test isolation](../../raw/project/2026-11-13-mvp-stage-3-ci.md), [Stage 4 offline evidence](../../raw/project/2026-11-14-mvp-stage-4.md); [MVP retirement](../../raw/project/2026-11-14-mvp-stage-5.md); [Public baseline review](../../raw/project/2026-11-14-mvp-stage-5-ci.md); [Stage 6 technical acceptance](../../raw/project/2026-11-14-mvp-stage-6.md)
> Updated: 2026-11-14

**Stages 0–5 merged; Stage 6 technical acceptance recorded.** PR #128 merged at `9d5a44c` after all seven checks passed. Real-farmer sessions were waived, not conducted. Provider/release limits below remain; Stage 6 PR must not merge without fresh explicit approval.

## Why
The existing SmartFarm/AniGo implements more features than the course MVP. Before this refactor MVC had nine primary farm links. MVC now has four and Android has three primary entries. Separate plot, timeline, finance, insight and support screens make the daily grower workflow hard to navigate. Simplify the **farmer-facing product**, not the essential transaction/offline architecture.

## Required MVP components
MVC farms/plots/crop calendar/harvest reports/inventory; API farm activities/observations/photos/harvests/supplies; Hangfire planting/watering/harvest reminders and weather checks; Kotlin/Room offline inspection/photos/activity logging; weather external API. See the canonical [PRODUCT.md](../../../../PRODUCT.md).

## Navigation status
| Client | Target primary destinations | Secondary |
| --- | --- | --- |
| Web MVC — implemented in Stage 1 | Today, Crops & Calendar, Supplies, Harvests | Farm/team administration, account, basic Help |
| Android — implemented in Stage 2 | Today, Record, My Farm | Account/settings, owner/manager tools, sync exception recovery |

Roles: Owner/Manager manage farm setup, crops, tasks, stock setup and members subject to existing server permissions; Field Worker records authorized crop activities, photos, stock use and partial harvests. Do **not** remove essential owner approval or bypass farm access.

## Keep and merge
- **Keep:** farms, plots, current crop and lifecycle, crop schedule, reminders, inspections, photos, history, ledger supplies, multi/partial harvest, simple safe reports and weather.
- **Merge:** MVC Plot Layout into crop/plot list; Crop Timeline into crop history; tasks and notifications into Today; weather as Today preview; Android old Tasks/Crops/More flows into three destinations.
- **Short forms:** Record Water / Inspect / Use Supplies / Harvest / Other with appropriate defaults, conditional issue fields, preserved validations, safe timezone and correct task linking.
- **Offline:** hide routine sync plumbing from navigation but show truthful queued/sent/review states. Keep Room/WorkManager, user scoping, retry and explicit conflict review.

## Stage 1 MVC behavior
Today prioritizes authorized due work, saved crop concerns and timestamped weather; membership approval remains secondary owner-only Farm & team. Water/fertilizer task links preselect the exact eligible pending task; inspection and harvest links choose the correct crop, with short forms implemented in Stage 3. Inspection/harvest contracts do not carry task-fulfillment fields.

Crops & Calendar contains all active plots, crop records and selected authorized history, including terminal cycles and private photo links. Native details/fragment navigation works without JavaScript. Supplies retains ledger and management safeguards. Harvests retains full filtered per-unit totals, bounded recent history, and both safe CSV exports; charts and finance links are removed from this page. Stage 5 replaces their standalone pages with redirects into this merged workspace.

Local Stage 1 checks: solution build, 565 SQL-enabled .NET tests without skips, 60 Node tests, 104 live disposable-SQL variants and 220 Linux synthetic authenticated screens across roles/themes/mobile sizes; 30 public baselines unchanged. No overflow or reported axe violations. These checks do not establish real-farmer, device, provider/job or production acceptance.

## Stage 2 Android behavior
Today / Record / My Farm use the existing native Material bar/rail. Record opens the existing scoped sheet: Water, Inspect, Use Supplies, Harvest; Other reveals uncommon activities/general stock use. The Other shortcut selects activity code 5 rather than watering. Only active cycles can receive new records. Deep forms retain Back and farm-switch draft protection; More is a compatibility redirect. Today links eligible active-crop tasks to their recording workflow; exact task-ID propagation was added in Stage 3.

My Farm groups crop/plot/history, supplies, harvest summary and weather; account/team/management/sync are secondary. Cash presentation is removed from the harvest report, but finance client/API dependencies remain for Stage 5 cleanup. Room, stable IDs, session guards, API and business services are unchanged.

Debug/device builds, 65 JVM tests and 103 emulator tests passed without skips. The full Activity journey verifies login, task completion, Back, farm switching, keeping a draft and logout. Native light/dark captures reviewed; 320dp/1.6 text and rail tested. A native session against disposable SQL-backed API data verified workspace/chooser/Other/Back without saving business records. Physical-device, TalkBack, production and real-farmer acceptance remain unverified.

## Stage 3 recording behavior
Water/fertilizer quick forms fix the activity type; Inspect shows Not sure for existing health code 0 and expands issue details conditionally. MVC supply use calls the existing atomic shared service with the displayed rowversion; stale stock/tasks require explicit review. Android passes exact water/fertilizer task IDs into the existing ordinary/combined Room payloads and displays crop/plot context. Failed forms retain values/errors; a closed crop exposes an escaped read-only unsubmitted draft. Harvest totals remain confirmed and unit-safe. Dates stay server assigned; API codes, Room schema, receipts, private photos and business rules remain unchanged.

Verification: 578 SQL-enabled .NET tests, 62 Node tests, 66 JVM tests and 105 Android emulator tests passed without skips. Live disposable MVC/API acceptance confirmed atomic save, duplicate rejection, rollback, partial harvests, private photos, access denial and closed-crop recovery using only newly created test resources. Reviewed 100 live Web variants across five widths/two themes and 10 no-JS variants; 40 all-rule axe scans had no detected violations. Linux fixtures passed 210 operation and 220 authenticated farm screenshots; 30 public goldens are unchanged. Native forms inspected at 320dp/1.6 text. Review's hidden-draft issue was fixed with failing-then-passing regressions. CI exposed a missing inventory read stub in the synthetic form-recovery factory; explicit no-database isolation fixed it. Full .NET checks then passed both without a database and with strict disposable SQL. Remote checks/merge status belong to the stage PR; physical-device, TalkBack, production/provider/Worker and real-farmer acceptance remain unverified.

## Stage 4 offline and automation behavior
Saved on phone / Sending / Sent / Needs review reflect actual queue/transfer/receipt evidence. Sent is a session acknowledgment; durable confirmed work remains in server history. Routine upload is automatic; rejected stock still requires explicit current-version review. Expired sign-in preserves local work and offers account recovery. Confirmed writes invalidate scoped caches and refresh current monitoring, supplies, task cards and harvests; drafts, receipts, period filters and confirmed values survive reload. Explicit access denial clears retained private data/permissions. User/resource revisions prevent older in-flight reads restoring invalidated cache, including prefixes; unrelated reads remain cacheable. Worker/weather implementation and stored-forecast cautions are unchanged.

Verification: 67 JVM and 117 emulator tests, 578 strict SQL-enabled .NET tests and 62 Node tests passed without skips. Review's two Important issues were reproduced and fixed. Real native offline fertilizer/harvest records against new disposable SQL-backed resources stayed unconfirmed until automatic worker acceptance; final stock/task/history and 2.500 kg totals matched. Native themes and 320dp/1.6 text were inspected and filter wrapping corrected. The loopback emulator used a temporary validation setting, restored afterward; physical-network, TalkBack, live Worker/provider, production and farmer acceptance remain unverified. No API/Room/schema/business-service changes.

## Stage 5 retirement and retention
Legacy plot/timeline URLs now redirect into Crops with farm/cycle identity and the relevant fragment. Cash UI/CSV, public bulletins, new tickets/replies and platform analytics return protected 410 responses. Help replaces farmer entry points; author-scoped support history excludes internal notes, and MFA-gated admin support/announcement archives retain read-only records/audits. Native reports fetch harvests only; advanced charts, decorative crop art and unused presentation helpers are removed. Report filters/confirmed unit totals, harvest CSV and all storage/history remain. Same-account Activity recreation preserves farm context; account changes/logout still clear it.

Finance API/shared services stay for installed/external compatibility. Mixed legacy CSS retains active shared styles. No Core/Infrastructure/API/Worker/database changes or data deletion. Verification: 577 strict SQL-enabled .NET, 64 JVM, 118 device and 62 Node tests pass without skips; 220 farm + 210 field-operation + 120 admin browser combinations and 24 live Help/history renders pass. Native light/dark and 320dp/1.6 text inspected; read-only admin-write and report-recreation regressions passed after failing first. The review's old empty-announcement draft wording was deferred at that stage and corrected during Stage 6. Production, physical-device, TalkBack, real-network/provider/Worker and farmer acceptance remain unverified.

Public-page follow-up: 36 same-browser base/candidate landing/login/registration pairs at six widths in both themes had zero changed pixels. Reviewed metadata now records this evidence; CI checks its tracked state before the unchanged Linux golden/pixel gate. The shared layout changed authenticated navigation; its reviewed fingerprint now comes directly from source after public equivalence was checked. Remote acceptance remains in the stage PR.

## Code entry points
Web: `Views/Shared/_FarmNavigation.cshtml`, `_Layout.cshtml`, `Views/Dashboard`, `Views/Crops`, `Views/Monitoring`, `Views/Inventory`, `Views/Reports`, `Views/Farms`, controllers and shared services.
Android: `navigation/FarmWorkspaceBar.kt`, `FarmMoreScreen.kt`, `FarmQuickRecordSheet.kt`, `SmartFarmNavHost.kt`, `features/farms/FarmHomeOverview.kt`, `features/monitoring/MonitoringScreens.kt`, `features/harvests/HarvestScreen.kt`, `features/inventory/InventoryScreens.kt`, `features/syncstatus/SyncStatusScreen.kt`.
Backend: `Infrastructure/Services/Monitoring/MonitoringService.cs` handles matching task fulfillment; API `SyncOperationsController` plus Room/WorkManager maintain idempotent writes; Worker `FarmAutomationService` supplies reminders/weather. Source remains authoritative for exact methods and paths.

## Stage 6 technical acceptance

Fresh guarded MVC/API/Worker and native offline journeys cover setup, approved joining/roles, generated-task watering, atomic stock use/rollback, inspections/private photos, repeated partial harvests, unit-separated reports/CSV and stable-ID conflicts. Final local gates: 578 strict SQL-enabled .NET, 64 JVM, 118 emulator, 62 Node and 7+15 recovery-tool tests. Live Chrome/Firefox/WebKit: 324 page variants, 54 axe scans and 18 enlarged-text checks with no detected failures; no-JS/keyboard/Escape also checked. Native queued fertilizer, two harvests and inspection/photo automatically confirmed on reconnect; controlled loopback is not ordinary network acceptance. See the plan for repeat commands and PR gates.

Real Hangfire processed tasks/reminders and registered recurring jobs in separate new storage, then stopped. Both external weather attempts timed out: no live forecast/advisory acceptance. No real-farmer sessions, physical-device/TalkBack, production/deployment, real restore or ordinary mobile-network validation. Farmer sessions remain a recommendation, not a fabricated completion. The empty announcement-history publishing prompt was corrected with a rendered RED→GREEN regression. No business/API/Room/schema changes.

## Guardrails and verification

Stage 0 checked active navigation/API/Android callers and recorded retention boundaries in the execution plan. The current baseline builds; SQL-enabled regression and Node checks pass. This establishes a starting point, not MVP delivery or farmer acceptance. Finance still has an Android/API dependency; database tables and history remain protected.

Maintain live authorization; opaque Identity API bearer (not JWT); Android session credentials memory-only; crop/stock/harvest rowversions, farm-local dates, UTC storage, atomic combined activity+inventory, private photo storage, replay identities, nonnegative stock, per-unit totals and reasons on void/reversal. No automatic schema deletes/migrations. A queued record must never appear as confirmed.

Plan for implementation and acceptance: [staged execution plan](../../../../docs/superpowers/plans/2026-11-13-mvp-simplification.md). Historical documentation pages describe implemented **old** behavior until stage-specific replacements land. Update article status only after verified PRs.
