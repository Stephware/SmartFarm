# Current project progress

> Sources: SmartFarm checkout, recent commits, implementation and test inventory
> Raw: [Progress sources](../../raw/project/2026-10-27-progress-source.md); [Architecture sources](../../raw/project/2026-10-27-architecture-source.md); [Feature sources](../../raw/project/2026-10-27-features-source.md); [Android sources](../../raw/project/2026-10-27-android-source.md); [Automation sources](../../raw/project/2026-10-27-automation-source.md); [Host test fixes and results](../../raw/project/2026-10-27-host-test-fixes.md); [Merged test fixes](../../raw/project/2026-10-27-host-test-merge.md); [JPEG fixture and photo verification](../../raw/project/2026-10-27-valid-jpeg-photo-fixture.md); [Functional E2E evidence](../../raw/project/2026-10-27-functional-e2e.md); [PR48 review evidence](../../raw/project/2026-10-28-pr48-review.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [Current audit](../../raw/project/2026-11-11-documentation-audit.md); [PR61–78 metadata](../../raw/project/2026-11-11-pr61-78-metadata.md); [Documentation verification](../../raw/project/2026-11-11-documentation-verification.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md); [MVP offline confirmation](../../raw/project/2026-11-14-mvp-stage-4.md)
> Updated: 2026-11-14

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

**MVP status:** [MVP simplification](mvp-simplification.md) and [execution plan](../../../../docs/superpowers/plans/2026-11-13-mvp-simplification.md). Stages 0–3 merged; Stage 4 offline confirmation/refresh implemented and locally verified. See the MVP article/plan for current evidence and remaining stages. Older PR history below remains historical.

## Reviewed baseline

Full source audit baseline: branch `dev`, HEAD `ac624be`, on 2026-11-11.
Reference frontend PR #86 subsequently merged at `ae06ab2`; local dev is synchronized.
The pre-existing untracked Gradle daemon configuration was preserved. GitHub
metadata confirms PR61–78 merged; every merge SHA is an ancestor of this checkout.
This verifies provenance, not passing CI or deployment. No branch/protection
settings were changed.

## Completed in source

See [features](features.md) for current behavior and [frontend](frontend.md) for
incumbent Web/Android conventions. The history below was cross-checked with
controllers/services, client wiring, schema/model and test/workflow sources.

| PR | Merged implementation scope |
| --- | --- |
| #61 `34710c7` | UI phase 1: initial Web/Android design tokens and shared components |
| #62 `22fed90` | UI phase 2: Web navigation/entry/farm selection |
| #63 `6bcb9d1` | UI phase 2 smoke-copy correction |
| #64 `9a345b7` | UI phase 3: dashboard, plots, members and crops |
| #65 `ed9a513` | UI phase 4: tasks, monitoring/photos, inventory and harvest |
| #66 `d5a28b7` | Android refresh/logout races, cancellation and safe network errors |
| #67 `f7541ef` | Weather freshness/cache timestamps and farm-access preservation |
| #68 `8619bde` | UI phase 5: Web reports/weather/finance/settings |
| #69 `fcdbe8a` | UI phase 6: Android workspace tabs and core farm/auth screens |
| #70 `a266d11` | UI phase 7: Android field/offline/pending/sync experience |
| #71 `a11f936` | UI phase 8: responsive/accessibility/role/navigation verification sources |
| #72 `3016c31` | Phase A: strict verification, backup/recovery helpers and safety CI |
| #73 `1f6440b` | Phase B: Android daily work center, manager task forms and guided notes |
| #74 `c4e7f61` | Phase C1: structured crop issues and explicit follow-up/task linking |
| #75 `9d7a903` | Phase C2: atomic field activity and matching supply consumption sync |
| #76 `a5e592d` | Phase D1: cycle-specific partial-harvest comparisons; initial finance aggregation |
| #77 `a8e7c94` | Phase D2: filtered cash insights, charts/breakdowns and safe finance CSV |
| #78 `cfcabb9` | Phase D3: filter-preserving cycle-total CSV, including safe empty exports |

Later Pine and Gold merges follow PR78: foundation `f96bf33`, dashboard `0a2ed4f`,
crop artwork `dcfbc34`, fieldwork `1a12e8f`, inventory/harvest `7a0716b`,
weather/reports/mobile insights `69a2c28`, and frontend polish `ac624be`.
Final signup simplification `c901cea` supersedes the intermediate confirmation
commit: current UI uses name, email and one password; farm joining is after login.

## Reference frontend reconstruction

PR #86 reconstructs the supplied HTML reference across Razor and native Compose,
with light defaults, explicit dark appearance, responsive navigation, crop assets,
calendars and preserved real form/data/offline behavior. All PR CI passed before
merge; the merged source tree equals tested feature `29a114c`. No API/service/schema
changes were included, and preexisting documentation stayed outside the PR.

Dated local verification: 430 .NET tests with strict disposable SQL, 47 Android JVM
tests, 67 emulator tests, 12 Web Node tests and 186 Playwright page captures. Real
form/role/CSV/appearance interactions and MainActivity system bars were checked.
Physical devices, deployment, provider ingestion, jobs and restore remain outside
that acceptance scope.

## Still incomplete or planned

See [known issues](known-issues.md) for current parity, freshness/session, schema
rollout and production verification limits. CI now exists and Worker defaults
enabled. Complete offline CRUD, secure persisted tokens, CD and a verified
production/recovery environment are not established.

## Historical documentation verification

> **Status: Outdated** (2026-11-11)
> The docs-only results below predate reference frontend acceptance above.

Documentation evidence/links/index and whitespace checks passed. Python verifier
and backup checks passed; Node checks exposed two existing harness failures, now
recorded in [known issues](known-issues.md#resolved-local-node-check-failures).
Application/test/schema files and original raw snapshots remain unchanged. No
current full backend, SQL, device or deployment acceptance is claimed.

## Historical verification record

The sections below preserve the original October work, counts, merge observations
and limits. References to then-current dev, ignored docs, absent Android reports,
disabled Worker or planned CI describe those revisions only. They do not override
today's source-backed pages or prove today's runtime behavior.

> **Status: Outdated** (2026-11-11)
> Initial local-only documentation policy, signup-with-code UI, Android report/task
> parity limits, CI absence and disabled Worker defaults have been superseded.
> Valuable dated verification is retained below and in immutable raw snapshots.

## Host-test fix verification

The test fixes were merged into `dev` through PR 46, at commit
`5d69ea1ed40a782d89dd98ff0db0bea96dc05b5d`. Local `dev` is synchronized with
`origin/dev`; its tree exactly matches the tested branch tree. Local documentation
and the ignored wiki were preserved.

Report-route query comparison and missing-database health isolation were corrected
in tests only. Both original failures were reproduced first; the corrected
targeted tests passed. Full backend verification then passed 307 tests:
API 71 + Auth 138 + SchemaVerification 14 + Weather 37 + Worker 47.
The checked-in application code, database schema, and business logic were unchanged.

SQL integration connection settings were unset/absent, so their guarded test
methods returned without exercising SQL workflows. No Android, browser, or
deployment checks were performed. Existing xUnit2000 analyzer warnings remain.
These results are later work and do not change the initial wiki setup's historical
verification scope below.

## JPEG fixture verification

PR 47 replaced the invalid JPEG fixture and merged into `dev` at
`53d4b389b1c14a4d09f1a38c502c54a8b83e89f7`. The original queue test's decode
failure was reproduced on the emulator. After replacement, all five photo queue
and image optimizer tests passed, along with a fresh run of all 18 Android JVM
tests, including upload retries. No application code or database schema changed.
The merged tree matches the tested branch tree; local `dev` is synchronized.

Only the selected photo instrumentation classes ran; backend, SQL integration,
and the full device UI suite were not rerun. The task's headless emulator was
stopped afterward. Existing compiler warnings remain unchanged.

## Historical functional verification

The full backend suite passed 307 tests with real disposable SQL connections,
including all five guarded SQL workflow/enrollment cases. Real Web MVC forms
passed 77 assertions across 308 requests. Android MainActivity testing completed
the Owner workflow through harvest, offline queue upload, and Manager/Field Worker
field actions and UI gates; ten prohibited API actions returned HTTP404.

Independent SQL verified the final totals and membership roles. An observed
Android display issue remains: the open stock screen showed **24.000 kg** after
sync while API/SQL correctly held **23.000 kg**. Android reports UI is absent;
Web browser click-through could not run because OS Computer Use permission was
unavailable. See [functional verification](functional-verification.md) for the
role matrix, evidence, methods, and remaining scope limits.

Task-created SQL database/hosts were removed, the emulator stopped, and the normal
debug API build restored. Source and Git state remained unchanged.

## PR48 completed

The monthly crop agenda, shared private photo-storage validation and AniGo labels
merged through PR48 at `de548ef108767bcadab47745d63e50fb43f3ea64`. The shared
scheduler's date-limit overflow was fixed with four failing-then-passing
regressions. Fresh backend **317** and Web **51** checks passed, including real SQL
and the previously failing page now returning **HTTP200**. Unchanged Android
code retains the earlier **18** JVM / **36** emulator passes. The tested and
merged trees match, local `dev` is synchronized, and disposable resources were
removed. See [PR48 verification](pr48-review.md) for evidence and limits.

### Earlier PR48 status

PR48's agenda, shared photo-storage validation and AniGo labels were reviewed at
`9a5165d3ca809f00b61eebec508c6dbb54556d6f`. Backend, Android and normal-path
runtime tests passed, but an accepted boundary planting date caused the new crop
page to return HTTP500. The PR remains open/draft; `dev` was not changed.
See [PR48 review](pr48-review.md) for the reproduction and recommended fix.


> **Status: Outdated** (2026-10-28)
> The boundary failure was fixed in shared scheduling and PR48 merged into dev.
> See the current result above and the linked fix/merge evidence.

## Wiki setup validation

Initial setup validated local ignore/readability behavior. That policy has changed
by explicit user request; current docs are Git-visible and private runtime artifacts
remain excluded. Current verification, provenance and maintenance commands are in
[the runbook](../../README.md); outcomes are appended to [the log](../log.md).

## See Also

[Features](features.md), [Known issues](known-issues.md), [Development](development.md),
[Decisions](decisions.md), [Architecture](architecture.md), [Frontend](frontend.md).
