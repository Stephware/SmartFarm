# Farmer-first MVP simplification — target state

> Sources: approved MVP brief and SmartFarm dev source, 2026-11-13
> Raw: [Stage 0 source and regression evidence](../../raw/project/2026-11-13-mvp-stage-0.md), [Stage 1 MVC evidence](../../raw/project/2026-11-13-mvp-stage-1.md), [Stage 2 Android evidence](../../raw/project/2026-11-13-mvp-stage-2.md)
> Updated: 2026-11-13

**Stages 0–1 merged; Stage 2 locally verified and reviewed, CI pending.** Stage 2 is based on `c33ed6e` (Stage 1 PR #124 passed all six CI checks). Shorter recording flows, offline clarity and obsolete-route retirement remain planned. No data or shared business-service functionality has been removed.

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
Today prioritizes authorized due work, saved crop concerns and timestamped weather; membership approval remains secondary owner-only Farm & team. Water/fertilizer task links preselect the exact eligible pending task; inspection and harvest links choose the correct crop, with short-form/task completion work still in Stage 3.

Crops & Calendar contains all active plots, crop records and selected authorized history, including terminal cycles and private photo links. Native details/fragment navigation works without JavaScript. Supplies retains ledger and management safeguards. Harvests retains full filtered per-unit totals, bounded recent history, and both safe CSV exports; charts and finance links are removed from this page. Legacy plot/timeline routes still exist until Stage 5.

Local Stage 1 checks: solution build, 565 SQL-enabled .NET tests without skips, 60 Node tests, 104 live disposable-SQL variants and 220 Linux synthetic authenticated screens across roles/themes/mobile sizes; 30 public baselines unchanged. No overflow or reported axe violations. These checks do not establish real-farmer, device, provider/job or production acceptance.

## Stage 2 Android behavior
Today / Record / My Farm use the existing native Material bar/rail. Record opens the existing scoped sheet: Water, Inspect, Use Supplies, Harvest; Other reveals uncommon activities/general stock use. The Other shortcut selects activity code 5 rather than watering. Only active cycles can receive new records. Deep forms retain Back and farm-switch draft protection; More is a compatibility redirect. Today links eligible active-crop tasks to their recording workflow; exact task-ID propagation remains Stage 3.

My Farm groups crop/plot/history, supplies, harvest summary and weather; account/team/management/sync are secondary. Cash presentation is removed from the harvest report, but finance client/API dependencies remain for Stage 5 cleanup. Room, stable IDs, session guards, API and business services are unchanged.

Debug/device builds, 65 JVM tests and 103 emulator tests passed without skips. The full Activity journey verifies login, task completion, Back, farm switching, keeping a draft and logout. Native light/dark captures reviewed; 320dp/1.6 text and rail tested. A native session against disposable SQL-backed API data verified workspace/chooser/Other/Back without saving business records. Physical-device, TalkBack, production and real-farmer acceptance remain unverified.

## Retire only after auditing dependencies
- Farm finance manual cash ledger/analytics from active product UI and code.
- Dedicated plot layout and crop timeline routes after moving their useful content.
- Analytics-only yield/cash comparisons, Android standalone insights and unused decorative crop artwork.
- Public announcements and support-ticket UI; nonessential platform analytics. Preserve any existing records/audits and core admin MFA/diagnostics.
- Do not delete database history or quietly change REST endpoints referenced by offline queues.

## Code entry points
Web: `Views/Shared/_FarmNavigation.cshtml`, `_Layout.cshtml`, `Views/Dashboard`, `Views/Crops`, `Views/Monitoring`, `Views/Inventory`, `Views/Reports`, `Views/Farms`, controllers and shared services.
Android: `navigation/FarmWorkspaceBar.kt`, `FarmMoreScreen.kt`, `FarmQuickRecordSheet.kt`, `SmartFarmNavHost.kt`, `features/farms/FarmHomeOverview.kt`, `features/monitoring/MonitoringScreens.kt`, `features/harvests/HarvestScreen.kt`, `features/inventory/InventoryScreens.kt`, `features/syncstatus/SyncStatusScreen.kt`.
Backend: `Infrastructure/Services/Monitoring/MonitoringService.cs` handles matching task fulfillment; API `SyncOperationsController` plus Room/WorkManager maintain idempotent writes; Worker `FarmAutomationService` supplies reminders/weather. Source remains authoritative for exact methods and paths.

## Guardrails and verification

Stage 0 checked active navigation/API/Android callers and recorded retention boundaries in the execution plan. The current baseline builds; SQL-enabled regression and Node checks pass. This establishes a starting point, not MVP delivery or farmer acceptance. Finance still has an Android/API dependency; database tables and history remain protected.

Maintain live authorization; opaque Identity API bearer (not JWT); Android session credentials memory-only; crop/stock/harvest rowversions, farm-local dates, UTC storage, atomic combined activity+inventory, private photo storage, replay identities, nonnegative stock, per-unit totals and reasons on void/reversal. No automatic schema deletes/migrations. A queued record must never appear as confirmed.

Plan for implementation and acceptance: [staged execution plan](../../../../docs/superpowers/plans/2026-11-13-mvp-simplification.md). Historical documentation pages describe implemented **old** behavior until stage-specific replacements land. Update article status only after verified PRs.
