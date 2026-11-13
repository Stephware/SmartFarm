# Implement AniGo farmer-first MVP simplification

**Status: PLANNED; no application changes from this plan.** Target: current \`dev\` from 2026-11-13 (`29c55166`). Keep each stage as a separately reviewable PR into \`dev\`, with tests before merge. The goal is fewer screens, **not** wholesale deletion of working infrastructure. Read [PRODUCT.md](../../../PRODUCT.md) and [AGENTS.md](../../../AGENTS.md) first.

## Scope and decision rules
- MVC four primary farm destinations: Today, Crops & Calendar, Supplies, Harvests. Farm/Team and Account secondary.
- Android three primary destinations: Today, Record, My Farm. Profile and sync error recovery secondary.
- Keep all assignment-required features: MVC farm/plot/crops/calendar/harvest/inventory, API activities/observations/harvest/supplies, Hangfire reminders/weather, Kotlin + Room offline/photos, external weather.
- UI removes finance, advanced analytics, decorative crop artwork, duplicative plot/timeline screens, platform news/ticket complexity; **back-end retirement requires a call-site and data-retention audit**. Never blindly delete tables, migrations, or old records.
- If code conflicts with this plan, inspect the actual implementation and adjust safely; record deviations in PR. No new dependencies/design system.

## Stage 0 — audit and safety inventory
- Check git status, active branch, current \`dev\` SHA, open PRs and tests. Create feature branches; don't touch dirty or unrelated files.
- Identify routes/components/services/DTOs/Room operations referencing finance, insights, plot layout, crop timeline, announcements, support, admin reporting and old navigation. Search scoped paths (including JS, CSS, tests, deep links).
- Inventory SQL schema/EF snapshots, migrations, API clients, Android navigation, automated CI, existing user data and operational dependencies. Classify each feature: *keep*, *merge*, *hide*, *retire after dependency review*.
- Keep historical evidence; no DB DDL or data removal. Establish representative role/empty/offline/crop/stock/harvest test cases.
- Deliverable: brief PR description or checklist in this plan; **do not produce multiple repetitive spec documents**.

## Stage 1 — MVC farmer workspace
- Change \`src/SmartFarm.Web/Views/Shared/_FarmNavigation.cshtml\`, \`_Layout.cshtml\`, related \`wwwroot/js/farm-navigation.js\` and active styles. Primary links: Today / Crops & Calendar / Supplies / Harvests. Preserve active farm context, no-JS fallback, mobile menus and focus behavior.
- Today: use \`Dashboard\` and \`Tasks\` shared models, show due/overdue/assigned, relevant crop concerns and compact Weather (stored timestamp + stale caution). Task action goes to correctly scoped activity/inspection/harvest. Manual task management secondary for Owner/Manager.
- Crops & Calendar: merge \`Crops\`, plot directory currently under \`PlotLayout\`, and crop history under \`CropTimeline\`. Keep farm/plot setup, actual vs planned planting, lifecycle, farm-local dates and safe statuses. No field map requirement.
- Supplies: reuse \`Inventory\` and ledger services; default simple available/low/received/used, management/adjustment/reversal behind authorized details. Do not bypass required reason/rowversion.
- Harvests: retain \`Harvests\` and \`Reports/Harvests\` data and authorized CSV, reduce chart/compare UI and filters, do not truncate aggregates or combine units.
- Role-dependent navigation: fieldworkers do not see manager forms; retain server checks. Update all links/test expectations.
- Verify: MVC route/authorization/antiforgery, actual Web layout at 320/375/768/1440, keyboard, no-JS, light/dark, date/unit tests and API parity.

## Stage 2 — Android navigation
- Refactor \`navigation/FarmWorkspaceBar.kt\`, \`FarmMoreScreen.kt\`, \`SmartFarmNavHost.kt\`, \`features/farms/FarmHomeOverview.kt\` without breaking saved state, farm switching or Back behavior.
- Three destinations only: Today (task actions, weather/attention), Record (four clear actions + Other), My Farm (crops/plots, supplies, crop/harvest history). Move profile, manager settings and sync recovery into secondary settings/context menus.
- Keep existing \`FarmQuickRecordSheet\`/ViewModels and farm-scoped routes as implementation details when useful; remove duplicate public entry points only after updating all callers/deep links.
- Fieldworkers see authorized tasks and recordings, not role management/finance/admin. Support 320dp screens, dynamic text, TalkBack, large targets, offline/loading/empty errors.
- Verify: JVM Compose/nav tests and compiled device UI tests; on emulator/device validate Back, farm switch, login/logout and record drafts.

## Stage 3 — Short recording workflows
- Inspect \`features/monitoring/MonitoringScreens.kt\`, \`features/harvests/HarvestScreen.kt\`, \`features/inventory/InventoryScreens.kt\`, MVC Monitoring and shared services.
- Water: crop/task auto-selected when arriving from Today; date default only with correct farm time zone. One save produces linked activity, resolves eligible task once.
- Inspect: healthy/not sure/needs attention; show issue/severity and follow-up only if needed, optional notes/photo. Preserve structured API validations.
- Fertilizer/supplies: one atomic activity + inventory consumption, valid category/quantity, no negative stock, no duplicate task close. Optional general stock-usage route remains for unrelated supplies.
- Harvest: quantity/unit/date defaults and optional note; repeat partial harvests; correct void handling; show server-confirmed totals only.
- Do not change DTO numeric codes or introduce fake persisted state. Test API/Room operation envelopes, service transaction rollbacks, preview/refetch conflict workflow, photo queue.

## Stage 4 — Offline and automation clarity
- Keep \`Room\`, WorkManager, stable operation IDs, user-scoped data, photo queue and cancellation/session guards.
- Show **Saved on phone / Sending / Sent / Needs review**; auto-retry transport failures; only manual review for conflicts/permanent rejections. Never silently discard/change signed operations or imply queued harvest/stock is server-confirmed.
- On successful sync, refresh/invalidate affected displayed inventories, monitoring/harvest and task cards without cross-account leaks; prevent duplicate server writes.
- Preserve Hangfire task/reminder/weather jobs; do not turn off jobs to make UX simpler. Do not present saved forecasts as live warnings; show freshness and official-source caution.
- Test offline save/reconnect, duplicate replay, conflict resolution, expired sessions, per-user cache, partial photo success, Worker scheduled behavior.

## Stage 5 — Retire non-MVP presentation and unused code
- Only after replacement links/flows have been verified: retire finance UI/API/service logic *if no active dependency remains*; keep financial tables and historical records unless a separate authorized migration is approved.
- Merge away dedicated \`PlotLayout\`/\`CropTimeline\`; remove now-unreferenced UI components and assets only after checking browsers, Android, tests and CSS. Actual authorized photos stay.
- Retire combined Android cash/harvest insights, finance charts, oversized analytics; preserve basic harvest history/totals and safe CSV.
- Retire announcement/support-ticket farmer UI and nonessential Super Admin analytics if there is a safe replacement/help contact. Retain Super Admin MFA, account security, audit trail, diagnostics for operations, and existing support records. Keep a route redirect/410 strategy when appropriate.
- Remove related stale links, tests that assert removed UI, assets and dead code; replace with MVP behavior tests rather than weakening coverage.
- **No destructive database SQL** in this stage. Any proposed schema contraction is separate, backed up, explicitly approved and forward-only.

## Stage 6 — release acceptance
- Demonstrate owner setup → plot → plan/start active crop → reminder → water → inspect/photo → fertilizer/stock → multiple harvests → accurate report; repeat Android offline/reconnect.
- Validate Owner/Manager/Field Worker boundaries; no unauthorized farm joins; one active crop per plot; no negative stock; no wrong-unit totals; server-confirmed vs pending counts never confused.
- Run targeted + full relevant .NET tests; Android assemble/JVM/device where available; Node/UI/cross-browser visual checks; SQL scripts only on explicitly disposable databases. Real Worker/weather/backup restore/deployment require separate authorization and evidence.
- Observe **3–5 real farmers** on phone without coaching. Target: complete primary tasks with no unclear screens and few required inputs; fix blocking usability failures. Do not declare farmer acceptance or production readiness from CI alone.

## Working agreement for Codex context efficiency
Read only AGENTS/PRODUCT/this plan and relevant code for the current stage. Do not paste all wiki/raw, run all agents, generate duplicate research summaries, or rebuild already accepted UI. Record a short check result and changed files for each PR. Tokens consumed by Codex cannot be made zero; this discipline **reduces avoidable context**. API bearer/refresh auth tokens are unrelated and must remain secure.

## Completion checklist (all initially unchecked)
- [ ] Stage 0 — current dependencies/coverage inventoried
- [ ] Stage 1 — MVC four-section farmer workspace implemented/tested
- [ ] Stage 2 — Android three-destination navigation implemented/tested
- [ ] Stage 3 — short record flows and atomic business rules verified
- [ ] Stage 4 — offline confirmation/refresh and Worker/weather verified
- [ ] Stage 5 — obsolete UI/code retired without lost data
- [ ] Stage 6 — end-to-end and farmer usability checks documented
