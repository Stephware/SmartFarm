# AniGo / SmartFarm — Codex instructions

## Current goal and source of truth
Build the **farmer-first MVP simplification**. Stages 1–2 MVC/Android navigation changes are implemented; later stages remain planned. Check the execution plan for verification and merge status.
Read, in order:
1. [PRODUCT.md](PRODUCT.md) — product requirements and keep/remove boundaries.
2. [Execution plan](docs/superpowers/plans/2026-11-13-mvp-simplification.md) — implementation stages and acceptance.
3. Only the relevant section of [wiki](.local/karpathy-wiki/wiki/index.md), then the affected source/tests.

Do **not** load the entire wiki, raw snapshots, old phase plans, or all skills by default. Current source overrides historical wiki descriptions. Earlier instructions to preserve finance, advanced insights, dedicated Plot Layout, timeline, platform announcements, and support tickets describe the **old implementation**, not the desired MVP. Do not mistake the plan for completed work.

## Nonnegotiable MVP
- MVC: farm and plot setup; crop plans/calendar; supplies and stock history; partial harvests and simple reports.
- API: authorized crop activity, crop observations, photo, harvest and inventory records, plus offline sync endpoints.
- Worker: Hangfire planting/watering/harvest reminders and weather checks.
- Android: simple daily work and field recording, photos, Room/WorkManager offline queue.
- External weather: saved forecasts and advisories with freshness and appropriate cautions.
- **Target navigation:** Web farm workspace = Today / Crops & Calendar / Supplies / Harvests; Android = Today / Record / My Farm. Keep Farm Settings/Team and Account secondary. UI roles are distinct: Owner/Manager manage; Field Worker records authorized work.

Keep: private photos, permission checks, one-active-crop-per-plot, partial harvests, unit-separated totals, stock ledger/atomic activity+stock updates, rowversions, idempotent sync, explicit conflict review, session boundaries, antiforgery, safe CSV, truthful offline confirmation. Simplifying the UI **must not** silently lose business records or weaken security.

Retire after dependency review: farm finance/cash insights, independent Plot Layout and Crop Timeline screens (merge content), advanced charts, Android combined cash/harvest insights, crop decorative SVG emphasis, public-facing announcements and support-ticket workflow, unnecessary Super Admin analytics. **Retire UI first; never drop SQL tables, migration history, audit records, or user data without a separate reviewed migration/retention decision.** Keep operational MFA, auth and basic audit. Existing farm-joining approval may be simplified visually, not bypassed.

## Engineering boundaries
- .NET 10; Core pure models/rules, Infrastructure shared services/EF/Identity, Web independent cookie MVC, API independent opaque Identity bearer (not JWT), Worker Hangfire with **separate jobs storage**, Android Kotlin Compose + Retrofit + Room/WorkManager.
- MVC and API use shared services/database; do not add Web-to-API HTTP calls for business logic.
- API auth/session tokens are **security credentials**: do not remove/change them as part of LLM token-efficiency work. Android tokens stay memory-only.
- SQL Server SQL-first; no startup migrations, destructive cleanup, or production data mutations. Preserve numeric DTO codes, date/time zone and rowversion semantics, API compatibility for queued Android operations.
- Use existing components, dependencies, accessibility and AniGo pine/gold theme. No new framework, no finance replacement, no speculative features.

## Execution with less Codex context
- Work **one stage at a time** following the execution plan; inspect callers before deleting any route/service/file.
- Prefer one coding agent. Invoke relevant specialized skills only if needed; avoid repeating the same analysis in subagents, duplicate plans, and giant context dumps.
- Keep prompts, commits and PR summaries short. Scope searches to affected paths; summarize findings and decisions rather than pasting files/logs. Never sacrifice tests or correctness to save model tokens.
- Preserve unrelated changes. Work on a focused branch off `dev`; PR to `dev`; promote `dev` to `main` only after review. Never assert CI success without checking it.
- Validate the affected .NET, Android, JS/visual and SQL-contract tests as applicable; real Android/device and live SQL tests only in authorized disposable environments. Do not run Worker against populated data casually.
- After each completed and verified stage, update only the relevant wiki article and the plan's completion status. Historical raw snapshots and manifest entries remain immutable. See [development checks](.local/karpathy-wiki/wiki/project/development.md).

**Stop condition:** a farmer can create a farm and crop, find today's work, record watering/inspection/photos/stock use/partial harvest, recover offline, and view a simple report without learning internal architecture.
