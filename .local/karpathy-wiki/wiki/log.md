# Wiki Log

## [2026-10-27] ingest | SmartFarm repository baseline
- Disposition: New
- Raw: raw/project/2026-10-27-architecture-source.md
- Raw: raw/project/2026-10-27-database-source.md
- Raw: raw/project/2026-10-27-features-source.md
- Raw: raw/project/2026-10-27-android-source.md
- Raw: raw/project/2026-10-27-automation-source.md
- Raw: raw/project/2026-10-27-progress-source.md
- Created: Architecture; Features; Database; Android and offline synchronization; Tasks, weather, and background automation; Dependencies and local development; Development decisions and constraints; Known issues, planned work, and verification gaps; Current project progress.
- Evidence: actual source, manifests, SQL/migrations, tests, local documentation, and recent local commits; historical claims resolved against current code.
- Verification scope: source review only; no application execution, live database access, emulator, deployment, or remote Git mutation.

## [2026-10-27] setup | Global skill and local discovery
- Reviewed upstream revision: `eafcc77001e496cc43499e4923b663aec722c813`.
- Installed: `/Users/stephen/.codex/skills/karpathy-llm-wiki/`; discovered globally through the symlink `/Users/stephen/.agents/skills/karpathy-llm-wiki`.
- Created: ignored root `AGENTS.md`, knowledge root/runbook, source manifest, immutable raw bundles, index, articles, and this log.
- Git: appended only `/.local/karpathy-wiki/` and `/AGENTS.md` to existing `.git/info/exclude`; no tracked configuration changed.
- Runtime: default Python 3.9 cannot run the upstream checker; existing Python 3.10 runs it unchanged. Exact command is in the runbook.

## [2026-10-27] lint | 0 issues found, 0 auto-fixed
- Upstream evidence checker: 0 fidelity suspects, 0 evidence errors, 0 unreferenced raw files.
- Upstream checker regression suite: 52 tests passed with the existing Python 3.10 interpreter.
- Local assertions: all 22 generated local files ignored, untracked, readable; nine articles indexed with matching dates and peer references; curated local links resolve; original source text and hashes match all 122 captured source files.
- Preservation assertions: all 477 tracked files byte-identical; Git index, branch, and HEAD unchanged; working tree remains clean.
- Global skill/checker blob hashes match the reviewed upstream source; global discovery symlink and templates are readable.
- Raw document links intentionally preserve their original source context; curated wiki links and Raw references were checked separately.
- New-session automatic discovery was not launched from this session; the runbook includes a concrete Codex prompt to verify it. No application/runtime readiness claims were made.

## [2026-10-27] ingest | Report route and missing-database health tests
- Disposition: Update
- Raw: raw/project/2026-10-27-host-test-fixes.md
- Updated: Dependencies and local development; Current project progress; index summaries.
- Reproduced: route query omitted by test recorder; health test unexpectedly used local Web database fallback.
- Fixed in tests only: preserve query string; inject an unconfigured SQL Server context for the missing-connection scenario.
- Verification: eight targeted tests and all 307 backend tests pass. SQL integration settings absent/unset; no Android/browser/deployment checks. Independent read-only review found no critical or important issues.
- Local wiki, source manifest, and AGENTS.md remain excluded from Git; unchanged wiki pages and prior raw captures were preserved.

## [2026-10-27] ingest | Host test fixes merged
- Disposition: Update
- Raw: raw/project/2026-10-27-host-test-merge.md
- Updated: Current project progress.
- PR: https://github.com/Stephware/SmartFarm/pull/46, merged into dev at `5d69ea1ed40a782d89dd98ff0db0bea96dc05b5d`.
- Verified: merged tree equals tested tree; local dev matches origin/dev; only the two test files changed; clean working tree; ignored local documentation preserved.

## [2026-10-27] ingest | Valid JPEG fixture and photo verification
- Disposition: Update
- Raw: raw/project/2026-10-27-valid-jpeg-photo-fixture.md
- Updated: Dependencies and local development; Current project progress; index summary.
- Reproduced: invalid six-byte JPEG fixture failed image decoding in the private-photo queue test.
- Fixed: generate a real JPEG, assert decoded stored dimensions, preserve bitmap/file cleanup and deletion checks. No production code or schema changes.
- Verification: all five selected photo emulator tests and all 18 Android JVM tests pass; JVM tests explicitly rerun instead of using cached results. Independent review found no blocking issues.
- PR: https://github.com/Stephware/SmartFarm/pull/47 merged into dev at `53d4b389b1c14a4d09f1a38c502c54a8b83e89f7`; tested and merged trees match; local dev synchronized.
- Cleanup: task emulator stopped; generated Room schema export preserved outside repository; wiki stays ignored. Backend, SQL integration, and full device UI suite not rerun.

## [2026-10-27] ingest | Real workflow and permissions verification
- Disposition: New; Update
- Raw: raw/project/2026-10-27-functional-e2e.md
- Created: Functional workflow and role verification.
- Updated: Current progress; Known issues; Android; Development; Features cross-reference; index.
- Evidence: 307 backend tests including five real SQL cases; 77 Web assertions across308 requests; real MainActivity UI workflow and ten role denial probes; independent SQL totals/role checks; two Web UI script tests.
- Found: open Android inventory display remains24.000kg after sync while API/SQL is23.000kg. No accounting failure or permission bypass observed. No production fix made in this testing task.
- Gaps: Android reports/dashboard unavailable; browser UI blocked by OS Computer Use permission; long-lived scheduling, physical devices, camera UI and production/load outside scope.
- Cleanup: only task-created database/hosts removed; normal debug API URL restored and task emulator stopped; generated schema copy kept outside repository; Git clean and HEAD unchanged.
- Privacy: retained sanitized reports/results/screenshots locally; excluded private test state, passwords, tokens, and signing keys.

## [2026-10-28] ingest | PR48 implementation and runtime review
- Disposition: New; Update
- Raw: raw/project/2026-10-28-pr48-review.md
- Created: PR48 review: blocked, not merged.
- Updated: Current progress; Known issues; index scope/date and entries.
- Reviewed: PR48 head `9a5165d3ca809f00b61eebec508c6dbb54556d6f`, targeting dev `53d4b389b1c14a4d09f1a38c502c54a8b83e89f7`.
- Passed:313 backend with five live SQL cases;18 Android JVM;36 emulator;51 scoped live Web assertions;10 production startup checks;two Web UI script tests.
- Blocker: real API accepts plantedDate9999-12-31 with daily watering/no expected harvest, then the same crop page changes from HTTP200 to HTTP500 with ArgumentOutOfRangeException in the new agenda.
- Decision: do not merge; PR remains open/draft. Recommended future-month scheduling guard plus regression test, no application edits made in this review.
- Cleanup: disposable database/hosts removed; task emulator stopped; checkout returned to unchanged, clean dev. Sanitized evidence kept local; private credentials/keys excluded.

## [2026-10-28] ingest | PR48 boundary fix and merge
- Disposition: Update
- Raw: raw/project/2026-10-28-pr48-fix-and-merge.md
- Updated: pr48-review, progress, known-issues, features, architecture, automation, android, development, decisions.
- Verified: four red/green boundary cases, 317 backend tests with five live SQL cases, 51 fresh Web checks across 155 requests, accepted max-date activation now HTTP200.
- Merged: PR48 into dev at `de548ef108767bcadab47745d63e50fb43f3ea64`; tested/merged trees match. Earlier Android and startup checks retained for unchanged files.
- Cleanup: task SQL/hosts/keys removed; existing database preserved. Wiki remains local/ignored.

## [2026-10-28] lint | 0 issues found, 0 auto-fixed
- Evidence checker: zero fidelity suspects, evidence errors, or unreferenced raw files.
- Local checks: all 68 wiki/guidance files ignored, untracked, and readable; article links and index dates valid; retained JSON has no credential keys.
- Final Git: clean dev synchronized with origin/dev; merged tree equals tested tree.

## [2026-11-11] ingest | SmartFarm current implementation and development workflow
- Disposition: Update; New
- Raw: raw/project/2026-11-11-backend-current.md
- Raw: raw/project/2026-11-11-reports-current.md
- Raw: raw/project/2026-11-11-android-current.md
- Raw: raw/project/2026-11-11-web-workflow-current.md
- Raw: raw/project/2026-11-11-documentation-audit.md
- Raw: raw/project/2026-11-11-pr61-78-metadata.md
- Updated: Architecture; Features; Database; Android and offline synchronization; Tasks, weather, and automation; Dependencies and development; Development decisions; Known issues; Current progress; historical Functional verification and PR48 review interpretation.
- New: Frontend; API contracts; Finance and reports.
- Reviewed dev at ac624be; PR61–78 merged metadata confirmed and all merge SHAs checked as local ancestors. Later Pine/Gold work included. Source review does not establish fresh runtime acceptance.
- User explicitly superseded local-only policy: shareable documentation is Git-visible; secrets/generated/private verification artifacts remain excluded. Original raw snapshots and earlier log entries preserved. No application/schema changes or Git integration authorized.

## [2026-11-11] ingest | Documentation verification results
- Disposition: Update
- Raw: raw/project/2026-11-11-documentation-verification.md
- Updated: Development; Known issues; Progress.
- Evidence checker clean; maintained relative file targets/index dates consistent; 15 schema and 7 recovery Python tests passed; Node 3 passed/2 failed in unchanged harness/source. Bash syntax and tracked diff whitespace passed. No runtime readiness claim; app/test/schema files and original raw snapshots preserved.

## [2026-11-11] lint | 0 documentation issues found, 0 auto-fixed
- Final maintained scope: 41 Markdown files; 381 relative file targets and referenced local Markdown anchors resolved. All 14 article index entries/dates matched; whitespace checks passed.
- Upstream evidence checker: 0 fidelity suspects, 0 evidence errors, 0 unreferenced raw files.
- All 58 new documentation/provenance files are Git-visible; private verification, environment files and generated output remain excluded. Staging index empty; dev HEAD ac624be unchanged. Original 13 raw snapshots and 590 non-document tracked source/config/test files matched baseline hashes.
- Independent source review corrections applied: precise client receipt scope, explicit separate API startup/disposable test setup, access-versus-refresh-token wording, and removal of the incomplete old 04→03 upgrade example. Current user Git-visibility policy takes precedence over historical local-only instructions.
- Source and documentation synchronization complete. Verification scope remains Python/schema/backup, Node and documentation checks; Node has 2 existing harness failures. No full backend build/tests, live SQL/device/browser/provider/jobs/recovery/deployment acceptance performed; no application/schema/test edits, commits, push, merge or PR creation.

## [2026-11-11] ingest | Reference frontend reconstruction and verification
- Disposition: Update
- Raw: raw/project/2026-11-11-reference-frontend-merge.md
- Updated: Frontend; Android; Development; Known issues; Progress; index.
- PR #86 merged into dev after all PR CI passed; 430 backend/SQL, 47 JVM, 67 emulator and 12 Node tests passed; 186 browser captures and real interactions checked. Earlier Node harness failures are historical/resolved. Physical devices, production/provider/jobs/recovery acceptance remain unverified.
- Maintained only affected local knowledge; no AGENTS or shared documentation was added to the implementation PR. Private runtime data was excluded.

## [2026-11-12] ingest | Documentation publication source check
- Disposition: New; Update
- Raw: raw/project/2026-11-12-documentation-publication.md
- New: Current source status.
- Updated: Index; active source articles and their current-source cross-references; runbooks and agent/workflow guidance.
- Publish documentation on current dev with Node/Tailwind/browser CI, admin/MFA, navigation and extended-schema limits accurately scoped. Historical immutable evidence is preserved; no fresh runtime acceptance or schema execution claimed.


## [2026-11-13] decision | Farmer-first MVP reset (planning only)
- Disposition: New decision and staged Codex handoff; **no application change**.
- Basis: explicit user scope decision; read-only review of current `dev` at `29c55166`, MVC nine-link farm navigation, Android five-tab + More navigation, actual recording/offline/Worker code.
- Created: `wiki/project/mvp-simplification.md` and an executable staged plan at `docs/superpowers/plans/2026-11-13-mvp-simplification.md`.
- Updated: `PRODUCT.md`, `AGENTS.md`, README and relevant wiki navigation/context to distinguish implemented old features from future MVP target.
- No raw source evidence snapshot added because this entry records a **user product decision**, not a new verification run. Historical raw snapshots, source hashes and manifest provenance remain untouched.
- Token-efficiency note: short canonical context reduces duplicated Codex prompting; cannot eliminate paid/model tokens and does not authorize removing secure API credentials.
- Verification: document links, scope consistency, Git diff and applicable CI to be checked on the PR; no SQL, API, emulator, Worker, data or deployment claims.

## 2026-11-13 — MVP Stage 0 verified locally

Updated the MVP article and existing execution plan with active callers, safe retention boundaries and the current regression baseline. Added sanitized evidence and appended manifest provenance. No application/schema changes; Stage 1 remains next.


## 2026-11-13 — MVP Stage 1 MVC workspace

Four primary farm sections, integrated plot/crop history and simpler Today/Supplies/Harvests presentation; exact eligible water/fertilizer task prefill. Updated only MVP status/context and appended sanitized verification evidence. Shared services, API and schema unchanged. Review/CI remains the merge gate; later stages remain planned.


## 2026-11-13 — MVP Stage 2 Android navigation

Three native primary entries and a simpler existing Record sheet; My Farm groups required records with account/management/sync secondary. Native tests, scoped live disposable-API inspection and review recorded in the MVP article/plan; appended sanitized evidence. No business/API/Room/schema changes; CI remains the merge gate.

## 2026-11-13 ingest | MVP Stage 3 recording
- Disposition: Update
- Raw: raw/project/2026-11-13-mvp-stage-3.md
- Updated: MVP simplification; current progress; index and execution status.
- Short forms, exact task envelopes, atomic MVC supply use and rejected-draft recovery verified; no API/schema/business-service changes. Stage 2 merged with seven green checks; Stage 4 remains next after Stage 3 gates. Private runtime evidence stays excluded.

## 2026-11-13 ingest | Stage 3 CI test isolation
- Disposition: Update
- Raw: raw/project/2026-11-13-mvp-stage-3-ci.md
- Updated: MVP simplification.
- Fixed the recovery factory's missing inventory stub; full no-database and strict SQL suites passed. Application source unchanged.
