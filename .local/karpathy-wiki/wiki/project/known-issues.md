# Known issues, planned work, and verification gaps

> Sources: SmartFarm implementation, historical documentation, and file inventory
> Raw: [Progress sources](../../raw/project/2026-10-27-progress-source.md); [Architecture sources](../../raw/project/2026-10-27-architecture-source.md); [Feature sources](../../raw/project/2026-10-27-features-source.md); [Android sources](../../raw/project/2026-10-27-android-source.md); [Automation sources](../../raw/project/2026-10-27-automation-source.md); [Database sources](../../raw/project/2026-10-27-database-source.md); [Functional E2E evidence](../../raw/project/2026-10-27-functional-e2e.md); [PR48 review evidence](../../raw/project/2026-10-28-pr48-review.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Documentation verification](../../raw/project/2026-11-11-documentation-verification.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

## Confirmed current limits

- **Post-sync freshness:** source still does not invalidate read cache or refresh
  open inventory/monitoring/harvest values on successful sync. The historical
  stale-stock symptom is in [functional evidence](functional-verification.md);
  accounting passed there. This audit did not reproduce the UI symptom again.
- **Android sessions:** memory-only tokens mean process restart requires login;
  durable Room rows do not imply durable authentication.
- **Selected offline/parity:** administrative writes/follow-up/voids/reversals
  remain online. Android now has task management and report/finance insights,
  but lacks Web's detailed finance entry UI, cycle-total comparison and CSV export.
- **Existing-database rollout:** code/model contains issue, inventory metadata,
  weather and optional finance changes. Not every base-schema change has a
  dedicated forward migration. Inspect live metadata/history and prepare an
  explicit reviewed rollout; do not infer compatibility or blindly apply all
  migrations to the already-current fresh schema.
- **Media/recovery:** durable shared private photos and protected key-ring storage
  are operator responsibilities; recovery rehearsal is not proven by scripts.
- **Coverage:** optional SQL suites can return without database execution. CI
  compiles device tests and runs Node/public Chromium checks, but does not run
  an Android emulator, live SQL or deployment.
- **Worker:** checked-in processing is enabled, not idle. Missing connection or
  schema config fails startup. Provider reachability/job execution remains runtime
  work; saved weather can be missing/stale even when UI is implemented.

## Resolved local Node check failures

> **Status: Outdated** (2026-11-11)
> PR #86 repaired the DOM harness and effective-cascade parser. All 12 Node Web
> checks now pass; real browser verification is recorded in [frontend](frontend.md).
> The following describes the earlier documentation-only run.

The documentation synchronization ran both Node files: 3 of 5 checks passed;
2 existing `site.test.cjs` checks fail before meaningful assertions. The mock DOM
lacks `getElementById` required by current navigation; the contrast parser assumes
each CSS `:root` override is a complete palette and dereferences an undefined
color. These unchanged source/test files were not repaired in this docs-only task.
This establishes test-harness failures, not a rendered accessibility or form-safety
verdict. A future scoped fix needs regression coverage and relevant UI validation.

## Resolved PR48 boundary failure

The old agenda maximum-date crash was fixed in shared scheduler arithmetic and
PR48 merged. Dated evidence remains in [PR48 review](pr48-review.md). Do not list
its initial review blocker as current, or reuse its passing counts for today's
changed source.

## Remaining production work

Secure persisted Android tokens and general email confirmation/password recovery
remain absent. Web Super Admin TOTP and one-time recovery-code login now exist;
see current source status for the separate administrator security boundary. Registration/login/enrollment rate limits now exist, but they are
per-process; production proxy/distributed behavior is unverified. Logout discards
mobile tokens without immediately revoking issued opaque access tokens.

Configure and verify HTTPS, release API URL, protected shared keys, private durable
storage, SQL upgrades, jobs storage and backup restoration for the actual
environment. No automatic CD/deploy pipeline or email/SMS/push transport was found.
This is a verified gap inventory, not an approved delivery roadmap.

## Historical documentation conflicts

> **Status: Outdated** (2026-11-11)
> Earlier claims that signup joins a farm, Worker defaults disabled, CI is only
> planned, Android has no reports/manager tasks, or only the initial Room schema
> is exported are superseded by current code. Older phase-only exclusions and
> raw snapshots remain dated history; current guidance has been synchronized.

## Verification scope

PR #86 later verified backend builds/tests with disposable SQL, native builds/
emulator tests, real browser interactions and green PR CI. Post-sync display
freshness and listed platform gaps were not repaired. Physical devices, provider
requests, Worker jobs, restoration and deployment remain unverified.

> **Status: Outdated** (2026-11-11)
> The following records only the earlier source/documentation audit.

## Not verified during the documentation synchronization

No full application build/backend suite, live SQL schema/workflow, emulator,
interactive browser, provider request, Worker execution, restore rehearsal,
production deployment or remote branch protection/CI status is established by
this source audit. Documentation verification results are recorded in the log.
Do not describe an unavailable check as an application failure or a passing check.

## See Also

[Progress](progress.md), [Features](features.md), [Android](android.md),
[Automation](automation.md), [Database](database.md), [Development](development.md).
