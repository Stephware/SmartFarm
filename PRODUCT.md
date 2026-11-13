# AniGo — farmer-first SmartFarm MVP

**Status:** approved product direction; Stages 1–3 MVC/Android navigation and short recording flows implemented, later stages planned. See the execution plan for verification and merge status. This document supersedes older UI phase briefs about retaining every feature. Source code is still the authority for current behavior.

## Purpose
Help nontechnical farmers manage planting schedules, crop condition, fieldwork, supplies, harvests and weather-related tasks. Less navigation and data entry are the goal. This is **not** a finance/ERP/social platform.

## Mandatory technical scope
- **MVC (.NET 10 Razor):** farms, plots, crop calendar, simple harvest reports and inventory.
- **API:** crop activities, observations/photos, harvests, supplies, synchronization.
- **Hangfire Worker:** scheduled planting/watering/harvest reminders and saved weather checks.
- **Kotlin Android:** inspections, photos, field activities and offline Room/WorkManager recording.
- **External API:** weather forecasts/advisories. Label last updated time and avoid treating saved forecasts as live warnings.

## Target user experience
- **Web:** exactly four primary farm sections: **Today**, **Crops & Calendar**, **Supplies**, **Harvests**. Farm/Team settings and Account are secondary. Owner/Manager can set up/plan/manage; Field Worker sees authorized work and recording.
- **Android:** exactly three primary destinations: **Today**, **Record**, **My Farm**. No top-level More/Tasks/Crops tabs. Profile/settings and exceptional sync recovery remain accessible secondarily.
- **Today:** due work, overdue work, crop concerns, brief weather, clear action buttons.
- **Record:** Water, Inspect (optional photo/issue details), Use Supplies / Apply Fertilizer, Harvest; uncommon activities behind Other. Preselect active farm, linked task/crop and local date when safely known.
- **My Farm:** current plots/crops, simple stock, history, basic harvest summary. Deep management is role-gated.
- Forms ask only required fields; expand conditionally. Human language, large touch targets, readable contrast, explicit loading/error/empty states and offline acknowledgments. Use pine/gold restrained brand and real authorized crop photos, not decorative SVG complexity.

## Functional decisions
| Feature | Decision |
| --- | --- |
| Farms, named plots, crop planning/status, crop dates | KEEP; one guided farm→plot→crop setup; no separate plot directory |
| Crop calendar and tasks | KEEP; present in Crops & Calendar/Today; Hangfire auto scheduling remains |
| Crop activities, inspections, photo capture | KEEP; single consistent Record entry with type-specific short forms |
| Inventory and stock movements | KEEP; rename user-facing section to Supplies; never bypass stock safeguards |
| Partial harvest and harvest reports | KEEP; accurate per-unit/cycle totals, history and simple safe CSV |
| Weather | KEEP; brief Today summary, fresh/stale labels and actionable warnings |
| Android offline queue and sync recovery | KEEP; routine sync automatic, only unresolved issues lead to review |
| Multi-farm users, authorization, essential admin auth/audit | KEEP; UI role-aware and secondary, not daily farmer navigation |
| Finance, income/expense, cash reports | RETIRE from MVP after dependency review; preserve old data |
| Separate Plot Layout, Crop Timeline | MERGE into Crops/history, then retire duplicate page/routes |
| Advanced analytics and visual charts | RETIRE; basic harvest totals/report remain |
| Platform announcements, help-desk ticket workflow, nonessential Super Admin analytics | RETIRE from MVP after review; retain security and existing history |
| FarmVille visuals / decorative crop art | RETIRE prominence and unused assets after reference audit |

## Invariants that simplification must not break
One Active crop per plot; authorized farm membership; terminal crop records stay readable; partial harvests can repeat; harvest units never combine incorrectly; voids/reversals preserve reason/history; no negative stock; combined activity+stock change and task fulfillment remain atomic; queued writes have stable IDs and checked replay; photos remain private; errors/conflicts never silently discard offline records; local save is **not** server success; stale weather must not imply an all-clear; accounts/API still authenticate securely.

## Acceptance: farmer journeys
1. Owner creates farm → first plot → active crop with planned/actual dates.
2. Worker opens Today → completes watering via a short Record workflow; matching due task resolves correctly.
3. Worker inspects crop, optionally photographs a concern, then sees saved/sync-confirmed status.
4. Worker records fertilizer with supply use **once**; balances remain correct and no duplicate deductions occur.
5. Worker records multiple partial harvests; owner sees correct totals per unit and valid CSV.
6. Android offline save → reconnect → retry/identity checks → confirmed display refresh; conflicts lead to explicit review.
7. Weather/reminders remain operative; Owner/Manager actions never become available to Field Worker.

## Implementation and evidence
Follow [the staged execution plan](docs/superpowers/plans/2026-11-13-mvp-simplification.md). Do not call this delivered until implementation, tests and farmer validation. **Codex's model tokens cannot be eliminated by documentation**; this file exists to avoid repetitive prompt context. This is unrelated to security bearer/refresh tokens, which remain required.
