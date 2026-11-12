# SmartFarm agent operating guide

## Start with context and source

Read [.local/karpathy-wiki/wiki/index.md](.local/karpathy-wiki/wiki/index.md), then
only relevant pages. Search with `rg --hidden --no-ignore` when needed. Source is
authoritative: inspect affected files/callers before relying on summaries. Keep
implemented, planned/incomplete and runtime-verified status distinct. Historical
test counts and merged PRs do not prove today's build, deployment or acceptance.

Use the installed tools as one workflow:
**Karpathy → Superpowers → Ponytail → Impeccable (for UI/UX) → Testing → Knowledge Update**.
Read actual installed skill instructions; apply only those relevant to the task.

- **Karpathy Wiki:** use `karpathy-llm-wiki` for knowledge/context and maintenance.
  This machine's skill is `/Users/stephen/.codex/skills/karpathy-llm-wiki/SKILL.md`.
  Resolve `raw/`, `wiki/` and checker paths under `.local/karpathy-wiki/`, never
  repository root. Read raw snapshots only for evidence questions.
- **Superpowers:** use applicable installed skills for planning, systematic
  debugging, TDD, execution, code review and verification. Follow their real
  triggers/approval rules under the user's current scope; avoid duplicate plans.
- **Ponytail:** use the installed simplicity workflow for coding. Prefer existing
  code/components, then built-ins and installed dependencies; smallest complete
  solution, YAGNI, beneficial DRY, minimal dependencies. Avoid speculative
  abstractions/boilerplate/optimization. Never cut security, validation, error
  handling, accessibility, correctness or essential tests for fewer lines.
- **Impeccable:** use applicable installed instructions for Web/Android interface
  design, audit or refinement. Prioritize accessible, responsive, consistent,
  polished farmer-friendly experiences. Follow context/playbook/native guidance;
  inspect rendered results. Documentation maintenance does not authorize redesign.

Detailed workflow and historical plans: [docs/superpowers/README.md](docs/superpowers/README.md).

## Architecture and conventions

- `Core`: plain entities, contracts, interfaces, enums and pure business rules;
  no Infrastructure/provider dependency. `Infrastructure`: EF/Identity,
  repositories/unit of work, shared services, weather adapter and private files.
- `Web` (Razor MVC/cookies) and `Api` (opaque Identity bearer, not JWT) independently
  call shared services/database. Do not reintroduce Web→API business HTTP calls
  through legacy `Web/Clients`; CSV helpers there remain active.
- `Worker`: headless Hangfire automation, separate jobs storage. Checked-in
  processing is enabled; explicitly disable it for an idle host. Startup may
  register/enqueue jobs. Do not assume running it is read-only.
- Android: Compose → StateFlow ViewModel → repository → Retrofit, manually wired
  in `AppContainer`; Room/WorkManager for selected cached reads/queued writes.
  Keep cancellation, user scoping and session-generation guards intact.
- Reuse established direct DbContext patterns where present; no blanket new
  repository/abstraction rule. Preserve DTO numeric codes, additive compatibility,
  ISO farm dates, UTC timestamps and Base64 rowversion checks across clients.
- Current UI is AniGo, Harvest Gold + Pine, Razor/CSS/JS and Material3 Compose.
  Reuse effective CSS tokens, theme/shared components and local artwork; inspect
  the final CSS cascade. No PRODUCT.md or DESIGN.md currently exists; follow
  Impeccable's actual prerequisites for future new surfaces/redesigns.

## Critical business and security rules

- Farm access comes from enabled user + active farm ownership/membership, checked
  server-side per operation. Global roles, cached data and hidden buttons grant
  no authority. Owner alone manages membership/codes/approvals and farm archive;
  Owner/Manager manages business data, with service-specific exceptions.
- Signup creates only an account. Signed-in code requests require owner approval
  to become Field Worker; pending/rejected/cancelled requests grant no access.
  Preserve cancellation/decision history and retired invitation Gone responses.
- Crop catalog is global/add-only. Only one Active crop per plot; terminal cycles
  do not reopen. New monitoring/harvest records require Active status; authorized
  history remains readable. Issue follow-up is manager/owner, versioned and online.
- Stock never becomes negative. All balance changes retain ledger history;
  consumption links an active same-farm crop activity. Combined fertilizer/pesticide
  activity and matching supply consumption must roll back together, including task
  fulfillment. Reversals/voids retain audit reasons; never edit away history.
- Harvesting does not complete crops or generate sales. Separate units/cycles in
  totals; exclude voids. Finance is manually recorded PHP cash, not accounting
  profit or inferred harvest/inventory value. Preserve full-scope aggregates,
  filter context and CSV formula protection despite bounded displays.
- Stable-ID sync commits business result and ledger atomically; preserve the
  implemented operation/type and observation/combined/photo receipt guards before
  queue deletion. Never silently rebase stock conflicts.
  Photos use stable-ID replay/private authorized storage. Tokens remain memory-only;
  never put them in plaintext preferences/Room. Offline support is not full CRUD.
- SQL Server is SQL-first. No startup Migrate/EnsureCreated/EnsureDeleted or silent
  history adoption. Base schema excludes optional finance; existing-database
  upgrades require reviewed schema/history and separately authorized execution.
  Never run fresh-schema DDL or write tests on populated SmartFarmDB.

## Development, tests and Git

Inspect `git status`/branch first; preserve unrelated files and staged changes.
Use focused branches into `dev`, reviewed promotion to `main`, and existing remote
requirements. Do not infer remote protection/green CI from local refs. Skill
example commit/push/merge commands do not expand the user's authorization. Never
force-add secrets/private artifacts or perform destructive cleanup to get a clean tree.

Development defaults: Web `localhost:5080`, API `localhost:5081`, Android emulator
API `10.0.2.2:5081`. Release needs explicit HTTPS endpoint, durable private photos,
protected shared keys and separately configured SQL/jobs storage. Keep credentials
in local/deployment secret configuration, never docs or tracked appsettings.

Use [development checks](.local/karpathy-wiki/wiki/project/development.md) for exact
commands and [database recovery](database/RECOVERY.md) for operator acceptance.
Run meaningful tests for affected behavior and inspect output before success
claims. SQL tests need explicitly disposable initialized connections; absent
settings can skip work. CI builds/tests .NET and Android JVM, compiles device tests,
and checks recovery scripts. It also runs Node, public Chromium smoke/a11y,
Tailwind consistency and PR visual checks; it does not run live SQL, an Android
emulator or deployment.
Report unrun checks. Documentation-only edits need source/link/evidence/diff checks.

## Knowledge maintenance and navigation

After significant completed changes, capture sanitized new evidence in a new
`raw/project/` file, update only affected article sections/cross-references, refresh
article/index dates, append `wiki/log.md` and manifest provenance. Preserve immutable
snapshots, append-only history and valuable superseded decisions with status/date.
Plain knowledge queries never write files. See [wiki runbook](.local/karpathy-wiki/README.md);
its evidence checker requires the documented Python 3.10 interpreter here.

Wiki, manifest/runbook, Superpowers docs and this guide are Git-visible by user
instruction; do not re-ignore them. Private historical `verification/`, secrets and
generated files remain excluded. Visibility does not mean permission to publish
private data. Inspect exact paths and diffs before any authorized staging.

Source updates since the full audit: [current source status](.local/karpathy-wiki/wiki/project/current-source-status.md).
Use self-hosted `tw:` Tailwind utilities without Preflight; rebuild with `npm ci`
and `npm run css:build`, retaining reference-ui.css last. Global SuperAdmin/MFA
portal rights are separate from farm ownership/membership. Existing SQL verifier
counts cover base/finance only; review support/announcement extensions explicitly.

Task references: [architecture](.local/karpathy-wiki/wiki/project/architecture.md),
[business rules](.local/karpathy-wiki/wiki/project/features.md),
[API](.local/karpathy-wiki/wiki/project/api-contracts.md),
[database](.local/karpathy-wiki/wiki/project/database.md),
[Android/sync](.local/karpathy-wiki/wiki/project/android.md),
[frontend](.local/karpathy-wiki/wiki/project/frontend.md),
[automation](.local/karpathy-wiki/wiki/project/automation.md),
[finance/reports](.local/karpathy-wiki/wiki/project/finance-reports.md),
[decisions](.local/karpathy-wiki/wiki/project/decisions.md),
[progress](.local/karpathy-wiki/wiki/project/progress.md),
[known issues](.local/karpathy-wiki/wiki/project/known-issues.md).
