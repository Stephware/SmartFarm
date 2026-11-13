# Farmer-first MVP simplification — target state

> Sources: approved MVP brief and SmartFarm dev source, 2026-11-13
> Raw: [Stage 0 source and regression evidence](../../raw/project/2026-11-13-mvp-stage-0.md)
> Updated: 2026-11-13

**Stage 0 verified locally; application simplification remains planned.** Source baseline: `29c55166f390efacfd5f717af98f506d50873dd5`. No functionality has been retired by this stage.

## Why
The existing SmartFarm/AniGo implements more features than the course MVP. MVC has nine primary farm links; Android has five primary tabs plus additional More entries. Separate plot, timeline, finance, insight and support screens make the daily grower workflow hard to navigate. Simplify the **farmer-facing product**, not the essential transaction/offline architecture.

## Required MVP components
MVC farms/plots/crop calendar/harvest reports/inventory; API farm activities/observations/photos/harvests/supplies; Hangfire planting/watering/harvest reminders and weather checks; Kotlin/Room offline inspection/photos/activity logging; weather external API. See the canonical [PRODUCT.md](../../../../PRODUCT.md).

## Navigation target (not current)
| Client | Target primary destinations | Secondary |
| --- | --- | --- |
| Web MVC | Today, Crops & Calendar, Supplies, Harvests | Farm/team administration, account, basic Help |
| Android | Today, Record, My Farm | Account/settings, owner/manager tools, sync exception recovery |

Roles: Owner/Manager manage farm setup, crops, tasks, stock setup and members subject to existing server permissions; Field Worker records authorized crop activities, photos, stock use and partial harvests. Do **not** remove essential owner approval or bypass farm access.

## Keep and merge
- **Keep:** farms, plots, current crop and lifecycle, crop schedule, reminders, inspections, photos, history, ledger supplies, multi/partial harvest, simple safe reports and weather.
- **Merge:** MVC Plot Layout into crop/plot list; Crop Timeline into crop history; tasks and notifications into Today; weather as Today preview; Android old Tasks/Crops/More flows into three destinations.
- **Short forms:** Record Water / Inspect / Use Supplies / Harvest / Other with appropriate defaults, conditional issue fields, preserved validations, safe timezone and correct task linking.
- **Offline:** hide routine sync plumbing from navigation but show truthful queued/sent/review states. Keep Room/WorkManager, user scoping, retry and explicit conflict review.

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
