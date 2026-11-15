# Implement AniGo farmer-first MVP simplification

**Status: Stages 0–5 merged; Stage 6 technical checks recorded with provider/release limits. Its CI follow-up requires explicit approval before merge.** Baseline: `dev` from 2026-11-13 (`29c55166`). Keep each stage as a separately reviewable PR into `dev`, with tests before merge. The goal is fewer screens, **not** wholesale deletion of working infrastructure. Read [PRODUCT.md](../../../PRODUCT.md) and [AGENTS.md](../../../AGENTS.md) first.

## Scope and decision rules
- MVC four primary farm destinations: Today, Crops & Calendar, Supplies, Harvests. Farm/Team and Account secondary.
- Android three primary destinations: Today, Record, My Farm. Profile and sync error recovery secondary.
- Keep all assignment-required features: MVC farm/plot/crops/calendar/harvest/inventory, API activities/observations/harvest/supplies, Hangfire reminders/weather, Kotlin + Room offline/photos, external weather.
- UI removes finance, advanced analytics, decorative crop artwork, duplicative plot/timeline screens, platform news/ticket complexity; **back-end retirement requires a call-site and data-retention audit**. Never blindly delete tables, migrations, or old records.
- If code conflicts with this plan, inspect the actual implementation and adjust safely; record deviations in PR. No new dependencies/design system.

## Stage 0 — audit and safety inventory
- Check git status, active branch, current `dev` SHA, open PRs and tests. Create feature branches; don't touch dirty or unrelated files.
- Identify routes/components/services/DTOs/Room operations referencing finance, insights, plot layout, crop timeline, announcements, support, admin reporting and old navigation. Search scoped paths (including JS, CSS, tests, deep links).
- Inventory SQL schema/EF snapshots, migrations, API clients, Android navigation, automated CI, existing user data and operational dependencies. Classify each feature: *keep*, *merge*, *hide*, *retire after dependency review*.
- Keep historical evidence; no DB DDL or data removal. Establish representative role/empty/offline/crop/stock/harvest test cases.
- Deliverable: brief PR description or checklist in this plan; **do not produce multiple repetitive spec documents**.

### Stage 0 findings

Source/retention review at `29c55166`; no open PRs found when checked. The older dirty frontend worktree and unrelated local Gradle file remain untouched.

| Area / active callers | Decision and safety boundary |
| --- | --- |
| MVC `_FarmNavigation`, `_Layout`, `farm-navigation.js`; Android workspace bar/NavHost/More | Merge navigation in stages 1–2; preserve scoped links, no-JS access, Back and drafts. |
| `PlotLayout`, `CropTimeline` controllers/views and crop/task links | Merge actual plot/history content before redirecting duplicate routes; lifecycle/history services stay. |
| `FarmFinanceController` in MVC/API; `IFarmFinanceService`; Android `FarmInsightsApi`/repository/ViewModel/AppContainer | Hide daily UI first. Finance API is an active dependency; do not retire its service until client/compatibility review in stage 5. |
| Announcement/support controllers/views, shared layout and Admin handlers | Hide farmer entry points only after a safe help/history replacement; preserve records and protected Admin access. |
| Admin reports, diagnostics, audit and MFA | Hide nonessential analytics later; keep operational diagnostics, security and audit. |
| Crop artwork, charts, styles and browser assertions | Remove only proven-unused presentation after replacements; retain actual private photos and replace obsolete UI assertions with MVP checks. |
| SQL scripts, EF configuration/snapshot and migration history | Keep tables, records and history, including finance/support/announcements. No DDL or production-record inspection/mutation performed. |
| Sync API envelopes, Room queue/receipts/photos; Worker jobs | Keep activity/activity_stock/observation/inventory/harvest contracts, session guards, atomicity and scheduled work. Finance is not a sync operation type. |

Baseline verification: solution build passes with existing analyzer warnings; **556 .NET tests pass with both disposable SQL connections required**, and **59 Node tests pass**. Existing tests cover role/enrollment isolation, lifecycle, stock rollback/replay, partial harvest/unit-safe CSV, private photos, weather and Worker scheduling; Android JVM/device compilation and browser checks remain CI gates. Representative empty/loaded farms, multiple crops, low stock, mixed-unit partial harvests, photos and pending membership fixtures are available in an isolated test environment. Live production data, external weather/Worker execution, physical devices and real-farmer acceptance are not verified by this inventory.

## Stage 1 — MVC farmer workspace
- Change `src/SmartFarm.Web/Views/Shared/_FarmNavigation.cshtml`, `_Layout.cshtml`, related `wwwroot/js/farm-navigation.js` and active styles. Primary links: Today / Crops & Calendar / Supplies / Harvests. Preserve active farm context, no-JS fallback, mobile menus and focus behavior.
- Today: use `Dashboard` and `Tasks` shared models, show due/overdue/assigned, relevant crop concerns and compact Weather (stored timestamp + stale caution). Task action goes to correctly scoped activity/inspection/harvest. Manual task management secondary for Owner/Manager.
- Crops & Calendar: merge `Crops`, plot directory currently under `PlotLayout`, and crop history under `CropTimeline`. Keep farm/plot setup, actual vs planned planting, lifecycle, farm-local dates and safe statuses. No field map requirement.
- Supplies: reuse `Inventory` and ledger services; default simple available/low/received/used, management/adjustment/reversal behind authorized details. Do not bypass required reason/rowversion.
- Harvests: retain `Harvests` and `Reports/Harvests` data and authorized CSV, reduce chart/compare UI and filters, do not truncate aggregates or combine units.
- Role-dependent navigation: fieldworkers do not see manager forms; retain server checks. Update all links/test expectations.
- Verify: MVC route/authorization/antiforgery, actual Web layout at 320/375/768/1440, keyboard, no-JS, light/dark, date/unit tests and API parity.

### Stage 1 evidence

Four primary MVC links, secondary Farm & team, integrated plot directory and selected crop history, eligible exact-task preselection for water/fertilizer, simpler supplies and per-unit harvest reports/CSV are implemented. Duplicate legacy routes remain until Stage 5. Inspection/harvest task fulfillment and short forms remain Stage 3 work.

Local checks: solution build; 565 SQL-enabled .NET tests (zero skipped); 60 Node tests; 104 live disposable-SQL page variants at 320/375/768/1440 in both themes, including role/empty/stale/history states; native no-JS crop navigation; 220 Linux authenticated fixture screens; 30 unchanged public visual baselines. No overflow or reported axe violations. No production data, schema, shared business-service or API changes. Android, real farmer acceptance and live Worker/provider execution are not validated by this stage.

## Stage 2 — Android navigation
- Refactor `navigation/FarmWorkspaceBar.kt`, `FarmMoreScreen.kt`, `SmartFarmNavHost.kt`, `features/farms/FarmHomeOverview.kt` without breaking saved state, farm switching or Back behavior.
- Three destinations only: Today (task actions, weather/attention), Record (four clear actions + Other), My Farm (crops/plots, supplies, crop/harvest history). Move profile, manager settings and sync recovery into secondary settings/context menus.
- Keep existing `FarmQuickRecordSheet`/ViewModels and farm-scoped routes as implementation details when useful; remove duplicate public entry points only after updating all callers/deep links.
- Fieldworkers see authorized tasks and recordings, not role management/finance/admin. Support 320dp screens, dynamic text, TalkBack, large targets, offline/loading/empty errors.
- Verify: JVM Compose/nav tests and compiled device UI tests; on emulator/device validate Back, farm switch, login/logout and record drafts.

### Stage 2 evidence

Android bar/rail now exposes Today / Record / My Farm. Record reuses the native sheet and scoped forms, with Water / Inspect / Use Supplies / Harvest and expandable Other. My Farm groups crop/plot/history, supplies, harvest summary and weather; management/account/sync stay secondary. Deep forms retain Back and farm-switch draft confirmation; the legacy More route redirects to My Farm. Today opens eligible active-crop task workflows. Exact task-ID propagation and shorter save forms remain Stage 3.

Cash presentation was removed now so the My Farm harvest link cannot expose finance; its API/repository/ViewModel dependencies remain for Stage 5 review. No API, Room, sync, shared-service or schema changes. Existing business data is retained.

Verification: debug/device-test builds; 65 JVM tests and all 103 tests on the Pixel 9 Android 17 emulator passed without skips, including the loopback Activity login/task/Back/farm-switch/draft-preservation/Record/logout journey. Native Today/My Farm light/dark captures reviewed; 320dp at 1.6 font scale and three-entry rail tested. A separate native login against the guarded disposable SQL-backed API verified loaded Today/My Farm, the common/Other chooser and Material Back collapse/dismissal without submitting business records. Focused independent review found no blocking or minor issues. Physical devices, TalkBack execution, production and real-farmer acceptance remain unverified.

Stage 2 PR #125 merged to dev (`c7412b6`) after all seven CI checks passed.

## Stage 3 — Short recording workflows
- Inspect `features/monitoring/MonitoringScreens.kt`, `features/harvests/HarvestScreen.kt`, `features/inventory/InventoryScreens.kt`, MVC Monitoring and shared services.
- Water: crop/task auto-selected when arriving from Today; date default only with correct farm time zone. One save produces linked activity, resolves eligible task once.
- Inspect: healthy/not sure/needs attention; show issue/severity and follow-up only if needed, optional notes/photo. Preserve structured API validations.
- Fertilizer/supplies: one atomic activity + inventory consumption, valid category/quantity, no negative stock, no duplicate task close. Optional general stock-usage route remains for unrelated supplies.
- Harvest: quantity/unit/date defaults and optional note; repeat partial harvests; correct void handling; show server-confirmed totals only.
- Do not change DTO numeric codes or introduce fake persisted state. Test API/Room operation envelopes, service transaction rollbacks, preview/refetch conflict workflow, photo queue.

### Stage 3 evidence

MVC quick forms focus Water / Inspect / Fertilizer / Other, with conditional issue/supply fields and optional notes/photos. Water/fertilizer use the exact selected task; MVC supply use calls the existing atomic service with the displayed rowversion. Android carries the same task identity into ordinary/combined Room payloads and shows scoped crop/plot context. Failed saves retain drafts and visible errors, including escaped read-only recovery after crop closure. Partial harvests, fixed units, voids and server-confirmed totals remain intact.

Contract rulings: existing server-assigned timestamps stay unchanged; no editable/backdated client date added. Only existing water/fertilizer activity contracts carry task IDs; inspection/harvest links retain their current contracts. No API/DTO codes, Room schema, sync guards, shared business logic or SQL changes.

Verified: solution build; 578 SQL-enabled .NET tests, 62 Node tests, 66 Android JVM tests and 105 Pixel 9 Android 17 tests, zero skips. Full Activity journey verifies exact ordinary/combined task envelopes. Live disposable MVC/API acceptance confirmed atomic stock/task save, duplicate rejection, insufficient-stock rollback, two partial harvests (3.750 kg), validation recovery, private photo read, unrelated-user denial and closed-crop draft safety. Only new test resources were written. Web: 100 live short-form variants at 320/375/390/768/1440 in both themes, 10 no-JS variants and 40 all-rule axe scans without detected errors/overflow/violations; 210 Linux operation fixture screenshots, 220 authenticated farm screenshots and 30 unchanged public goldens passed. Native forms inspected against actual disposable API data and at 320dp/1.6 text. Focused review's Important hidden-draft issue was fixed with RED→GREEN regressions and full .NET rerun. Remote checks and merge status are tracked by the stage PR; physical-device/TalkBack, production, live Worker/provider and real-farmer acceptance remain unverified.

## Stage 4 — Offline and automation clarity
- Keep `Room`, WorkManager, stable operation IDs, user-scoped data, photo queue and cancellation/session guards.
- Show **Saved on phone / Sending / Sent / Needs review**; auto-retry transport failures; only manual review for conflicts/permanent rejections. Never silently discard/change signed operations or imply queued harvest/stock is server-confirmed.
- On successful sync, refresh/invalidate affected displayed inventories, monitoring/harvest and task cards without cross-account leaks; prevent duplicate server writes.
- Preserve Hangfire task/reminder/weather jobs; do not turn off jobs to make UX simpler. Do not present saved forecasts as live warnings; show freshness and official-source caution.
- Test offline save/reconnect, duplicate replay, conflict resolution, expired sessions, per-user cache, partial photo success, Worker scheduled behavior.

### Stage 4 evidence

Stage 3 PR #126 merged at `292dca8` after all seven checks passed. Stage 4 adds truthful local/sending/confirmed/review status and sign-in guidance, scoped cache invalidation and automatic display/task refresh. Stable IDs, explicit stock review, photo receipts, queued payloads and Room/API/schema compatibility remain intact. Drafts and previous confirmed values survive refresh; explicit access denial clears private snapshots/permissions. Resource revisions prevent older reads undoing exact/prefix invalidation. Worker/weather source and processing defaults are unchanged.

Verified: Android build, 67 JVM and 117 emulator tests without skips; 578 strict disposable-SQL .NET and 62 Node tests; independent review's two Important findings fixed with RED→GREEN regressions and full rerun. The real native app queued fertilizer/harvest offline against newly created disposable resources: server values stayed unchanged until automatic WorkManager acceptance, then exact task/stock/history and 2.500 kg confirmed totals matched. Light/dark and 320dp/1.6 text inspected; narrow filters now wrap. The loopback emulator needed a temporary platform validation setting, restored afterward; this is not physical-network/farmer acceptance. No live Worker/provider or production run. See [dated evidence](../../../.local/karpathy-wiki/raw/project/2026-11-14-mvp-stage-4.md); the stage PR records remote checks/merge status.

## Stage 5 — Retire non-MVP presentation and unused code
- Only after replacement links/flows have been verified: retire finance UI/API/service logic *if no active dependency remains*; keep financial tables and historical records unless a separate authorized migration is approved.
- Merge away dedicated `PlotLayout`/`CropTimeline`; remove now-unreferenced UI components and assets only after checking browsers, Android, tests and CSS. Actual authorized photos stay.
- Retire combined Android cash/harvest insights, finance charts, oversized analytics; preserve basic harvest history/totals and safe CSV.
- Retire announcement/support-ticket farmer UI and nonessential Super Admin analytics if there is a safe replacement/help contact. Retain Super Admin MFA, account security, audit trail, diagnostics for operations, and existing support records. Keep a route redirect/410 strategy when appropriate.
- Remove related stale links, tests that assert removed UI, assets and dead code; replace with MVP behavior tests rather than weakening coverage.
- **No destructive database SQL** in this stage. Any proposed schema contraction is separate, backed up, explicitly approved and forward-only.

### Stage 5 evidence

Stage 4 PR #127 merged at `9c9adce` with all seven checks green. Legacy plot/timeline URLs redirect into Crops. Cash/bulletin/new-ticket/analytics tools return protected 410 responses; Help and author-scoped/MFA-gated read-only histories retain records. Android fetches harvest reports only; chart/art/unused UI code is removed. Finance API/shared services, all schemas/data, harvest CSV, private photos, offline contracts, Worker/weather and security remain. Report recreation restores farm context and large-text counters stay readable.

Verified: 577 strict disposable-SQL .NET, 64 JVM, 118 device and 62 Node tests without skips; 220 farm + 210 field-operation + 120 admin browser combinations; 24 live Help/history renders and native themes/320dp/1.6 text. Review's writable admin archive was fixed RED→GREEN. Old empty-announcement draft wording is a deferred Minor. Existing 100-row support list bound and mixed shared CSS remain. Stage 6 records technical acceptance and remaining physical/TalkBack/real-network/farmer/production/provider limits. [Dated evidence](../../../.local/karpathy-wiki/raw/project/2026-11-14-mvp-stage-5.md); stage PR records remote gates.

## Stage 6 — release acceptance
- Demonstrate owner setup → plot → plan/start active crop → reminder → water → inspect/photo → fertilizer/stock → multiple harvests → accurate report; repeat Android offline/reconnect.
- Validate Owner/Manager/Field Worker boundaries; no unauthorized farm joins; one active crop per plot; no negative stock; no wrong-unit totals; server-confirmed vs pending counts never confused.
- Run targeted + full relevant .NET tests; Android assemble/JVM/device where available; Node/UI/cross-browser visual checks; SQL scripts only on explicitly disposable databases. Real Worker/weather/backup restore/deployment require separate authorization and evidence.
- Observe **3–5 real farmers** on phone without coaching. Target: complete primary tasks with no unclear screens and few required inputs; fix blocking usability failures. Do not declare farmer acceptance or production readiness from CI alone.

### Stage 6 technical acceptance evidence

PR #129 was externally merged by `Stephware` at `6982780` while the emulator gate was failing; this agent did not merge. The test-only follow-up waits for native picker readiness and actual sheet dismissal, preserving all assertions. Local build/64 JVM/118 device checks pass; check all seven jobs on the follow-up PR head before acceptance. [CI evidence](../../../.local/karpathy-wiki/raw/project/2026-11-14-mvp-stage-6-ci.md). The external merge does not grant approval for another merge.

Stage 5 PR #128 merged at `9d5a44c` after all seven checks passed. The user waived real-farmer sessions for this technical stage: **not conducted**, recommended later with 3–5 farmers. Do not merge this stage's PR without fresh explicit approval; leave `main` untouched.

Verified on new disposable application/jobs databases: normal signup with no farm; MVC farm→plot→plan→start; owner-approved joining and role boundaries; one Active crop/plot; real Hangfire task/reminder generation and exact generated-task watering; atomic fertilizer/stock/task save, duplicate rejection and negative-stock rollback; inspection/private photo; repeat partial harvests, void history, unit-separated/filter-correct reports and formula-safe CSV; stable-ID replay/conflict/outsider protection. Native real-API fertilizer, two harvests and inspection/photo were queued offline once and automatically confirmed on reconnect; pending records did not inflate server values. Controlled loopback validation is not ordinary mobile-network acceptance.

Final local gates: solution build and 578 strict SQL-enabled .NET tests; Android debug build, 64 JVM and 118 emulator tests; 62 Node and 7+15 recovery-tool tests, zero failures/skips. Live Chrome/Firefox/WebKit: 324 role/page/size/theme variants, 54 axe scans, 18 enlarged-text checks, no detected overflow/errors/violations; no-JS/keyboard/Escape checked (macOS WebKit uses Option-Tab for links). A misleading retired-publishing prompt was fixed RED→GREEN. Native themes/320dp/1.6 text results and final remote gates are recorded in the stage PR.

**Remaining limits:** both real OpenMeteo attempts timed out; weather mapping/failure tests pass but provider forecast/advisory acceptance remains unverified. Production/deployment, real restore, physical devices, TalkBack and ordinary network wakeup are unverified. No farmer results or production readiness claimed. See [sanitized evidence](../../../.local/karpathy-wiki/raw/project/2026-11-14-mvp-stage-6.md).

Repeat read-only live browser checks using `node tests/browser/mvp-acceptance.cjs` with installed Playwright/axe resolvable through `NODE_PATH` when needed. Set `SMARTFARM_ACCEPTANCE_WEB` to the local lab Web URL, `SMARTFARM_ACCEPTANCE_CONFIG` to a protected JSON containing disposable `Database`, `Password`, `Owner`, `Manager`, `Worker`, `SMARTFARM_ACCEPTANCE_RESOURCES` to protected `{farmId,cycleId}` JSON, and `SMARTFARM_ACCEPTANCE_OUTPUT` to a fresh private directory. The accounts must already have the stated roles on that farm. Never commit credentials, sessions or captures; this runner logs in and reads existing data only. Linux visual golden checks remain separate CI gates.

## Working agreement for Codex context efficiency
Read only AGENTS/PRODUCT/this plan and relevant code for the current stage. Do not paste all wiki/raw, run all agents, generate duplicate research summaries, or rebuild already accepted UI. Record a short check result and changed files for each PR. Tokens consumed by Codex cannot be made zero; this discipline **reduces avoidable context**. API bearer/refresh auth tokens are unrelated and must remain secure.

## Completion checklist (all initially unchecked)
- [x] Stage 0 — current dependencies/coverage inventoried
- [x] Stage 1 — MVC four-section farmer workspace implemented/tested; PR #124 merged after all six CI checks passed
- [x] Stage 2 — Android three-destination navigation implemented/tested; PR #125 merged after all seven CI checks passed
- [x] Stage 3 — short record flows verified; PR #126 merged after all seven checks passed
- [x] Stage 4 — offline confirmation/refresh verified; Worker/weather automated checks retained; stage PR records CI/merge status
- [x] Stage 5 — non-MVP presentation retired; records/contracts retained; local verification complete, stage PR records remote gates
- [x] Stage 6 — technical checks and limitations recorded; farmer sessions waived/not conducted; PR requires explicit merge approval
