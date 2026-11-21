# Dependencies and local development

> Sources: SmartFarm build manifests, launch profiles, scripts, and tests
> Raw: [Agent workflow refresh](../../raw/project/2026-11-16-agent-workflow-refresh.md); [Architecture sources](../../raw/project/2026-10-27-architecture-source.md); [Android sources](../../raw/project/2026-10-27-android-source.md); [Progress sources](../../raw/project/2026-10-27-progress-source.md); [Database sources](../../raw/project/2026-10-27-database-source.md); [Host test fixes and results](../../raw/project/2026-10-27-host-test-fixes.md); [JPEG fixture and photo verification](../../raw/project/2026-10-27-valid-jpeg-photo-fixture.md); [Functional E2E evidence](../../raw/project/2026-10-27-functional-e2e.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Documentation verification](../../raw/project/2026-11-11-documentation-verification.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md); [Backend B0–B5 evidence](../../raw/project/2026-11-20-backend-b0-b5.md); [Backend integration check](../../raw/project/2026-11-20-backend-b0-b5-integration.md); [Backend delivery follow-up](../../raw/project/2026-11-20-backend-delivery-followup.md)
> Updated: 2026-11-20

## Branded email implementation — 20 November 2026

The [email plan](../../../../docs/superpowers/plans/2026-11-20-branded-email.md) and [operator notes](../../../../docs/email/README.md) record the shared HTML/plain-text layout for the two actual security email events. No new mail triggers, providers or schema changes were added. Release checks (693), ten renderer/MIME cases, seven strict disposable-SQL recovery/backend cases, fictional browser previews and authorized SMTP inbox delivery are verified. The Development-only preview tool loads no live configuration and cannot send. Existing production services/configuration remain unchanged until approved activation; actual mail-client visual QA and latest-head CI are separate gates.

## Production deployment update — 20 November 2026

The [production execution ledger](../../../../docs/deployment/2026-11-20-production-execution.md) supersedes earlier deployment/provider unknowns for this specific release. Both public HTTPS hosts, restored existing data/CHECKDB, full source schema comparison, opaque bearer/MVC login, physical-phone login, real Resend inbox delivery, actual Gemini client, separate Hangfire processing/saved weather, shared private storage and session persistence through AniGo-only restarts are verified. Historical local schema-drift and provider limits below remain dated evidence, not current results. No final CI/main merge, store signing, final recovery-bundle restore drill is implied. See the ledger for the approved photo-test and backup completion status.

## Backend B0–B5 verification

Final backend Release verification passed 649 .NET tests with both disposable SQL settings and strict SQL execution, 17 Python schema tests, 7 backup script tests, 100 integrated Node tests and 78 Android JVM tests. Full solution build (including the explicit demo seeder), Android debug build and instrumented-test compilation passed. One independent review's provider-stream, DST-boundary, SMTP-timing and sync-UTC findings were reproduced and fixed. Local populated database schema drift blocks seeding; actual email/Gemini providers, device execution, production Worker and full restore remain unverified. See the [plan](../../../../docs/superpowers/plans/2026-11-19-backend-b0-b5.md) for exact phase evidence and delivery status. Prior dated results below remain historical.

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

## Checked-in dependencies

Backend targets `net10.0`, nullable and implicit usings enabled. SDK request is
`10.0.100` with latest-feature roll-forward; EF/Identity/PKCS and local EF tool pin
`10.0.13`. Worker pins Hangfire `1.8.25` and Newtonsoft.Json `13.0.4`.
Android tracks Java/Kotlin bytecode target 17, SDK 36/minimum 26, AGP `9.1.1`,
Compose plugin `2.2.10`, Gradle `9.3.1`, Room `2.8.5`, Retrofit `3.0.0`,
OkHttp `4.12.0` and WorkManager `2.11.0`. Exact manifests remain authority;
these are repository versions, not recommendations about newest releases.

## Local hosts

Development profiles: Web `http://localhost:5080`, API `http://localhost:5081`;
emulator debug API `http://10.0.2.2:5081/`. Start hosts in separate terminals.
Android release needs an explicit HTTPS URL with trailing slash. Supply database
settings through local secrets/environment, never documentation or committed config.
In Development API may reuse only Web's DB secret when no explicit API setting exists.

Web/API never initialize/upgrade SQL. Worker defaults enabled and needs both
application/jobs storage; explicitly disable Hangfire to run idle. Before any
runtime tests read [database](database.md) and [automation](automation.md).

## Verification entry points

Choose checks by affected behavior; commands here are entry points, not claims
that they ran during this source audit.

```sh
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
dotnet test SmartFarm.slnx --no-build
python3 -m unittest discover -s database/verification -v
python3 -m unittest discover -s scripts/tests -v
node --test tests/web-ui/site.test.cjs tests/web-ui/farm-location.test.cjs
bash scripts/verify-phase-a.sh --skip-android
# From mobile/SmartFarm.Android:
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
# Authorized running device/emulator required:
bash ./gradlew :app:connectedDebugAndroidTest
```

`verify-phase-a.sh` combines Release backend, Python and default Android checks;
`--sql` requires a disposable initialized `SMARTFARM_TEST_CONNECTION` and enables
strict `BusinessWorkflowSqlTests` enforcement. `--device` opts into device execution.
`verify-phase6.sh` also checks real Web/API endpoints and supplies both SQL-suite
settings. Older phase1 scripts remain narrower entry points.

SQL suites require disposable `SmartFarmEnrollmentTest_` or
`SmartFarmEnrollmentBackend_` catalogs. Enrollment separately reads
`SMARTFARM_ENROLLMENT_TEST_CONNECTION`. Without configured connections methods
may return without SQL work; normal green suites/CI are not integration evidence.
Never aim them at a populated developer or production database. Source names such
as `StandaloneApiIntegrationTest` can mean MockWebServer, not a live backend.

Current CI has three workflows: `verify-smartfarm.yml` runs Release .NET/Node,
Android build/JVM/device-test compilation, Chromium and Tailwind/parity checks;
`phase-a-stability.yml` checks backup/schema/scripts; `android-ui-smoke.yml`
executes the full instrumented suite and farmer journey on an Android emulator for
matching PRs. Read the exact commit's job results and XML counts, not historical
counts or workflow presence. None supplies disposable SQL, physical-device or
provider/deployment acceptance. Placeholder folders do not establish coverage.

MVC tests use project-scoped `test.runsettings` to disable unused live JSON reload
and avoid exhausting Linux inotify instances while starting many test hosts.
Production host configuration is unchanged. For keyboard-driven device tests,
wait for actual IME/window readiness before scrolling and physically tapping;
keep visibility, submission-count and business-result assertions.

After significant changes, check affected AGENTS/PRODUCT/workflow guidance as well
as wiki evidence. Discover installed skill instructions rather than copying cached
commands; see [engineering workflow](../../../../docs/superpowers/README.md).
Impeccable context is loaded once per session, with native guidance for Android;
explicit freshness checks use its installed doctor workflow. Approved future
landing/showcase work is code-first; UI Phase 6 remains separate from maintenance.

## Host test maintenance

Keep database-health tests isolated from Development Web-secret fallback. Legacy
HTTP-client route tests compare PathAndQuery for date/plot/crop filters, but MVC
business pages use shared services. When tests fail, diagnose against the actual
host boundary rather than reintroducing Web→API dependency.

## Photo fixture maintenance

Use decodable image fixtures for `LocalPhotoFiles.copyFrom`, not just magic header
bytes. Optimization can recompress: assert meaningful image dimensions/queue/file
behavior. Test photo ID/observation receipt matches, dependency order and cleanup;
API tests should cover authorized reads and private storage configuration.

## Live functional verification

Historical [functional](functional-verification.md) and [PR48](pr48-review.md)
results retain their original scope. Current UI, SQL upgrades, device flows,
external provider/jobs, production storage and recovery need fresh task-specific
evidence. No general runtime readiness claim follows from documentation updates.

## Latest PR verification

[Progress](progress.md) maps the frontend overhaul, Phases A–D and later Pine/Gold
merges from local history to current source. It does not assert remote CI status.
Use the installed [engineering workflow](../../../../docs/superpowers/README.md)
for future planning/debugging/TDD/review/verification; avoid duplicate processes.

## Reference frontend acceptance

PR #86 merged at `ae06ab2` after .NET/Android/recovery PR CI passed. Local checks
passed 430 .NET tests with both disposable SQL settings and strict SQL execution,
47 Android JVM tests, 67 emulator tests and 12 Node Web tests. Playwright covered
186 pages at the required widths plus real form/role interactions. Prior Node
harness failures are resolved. These dated results do not verify physical devices,
production deployment, provider ingestion, Worker jobs or recovery restoration.

## Historical documentation synchronization checks

> **Status: Outdated** (2026-11-11)
> The earlier docs-only run below predates PR #86 and its runtime verification.

Fresh documentation verification passed the evidence checker, relative file targets,
index coverage/dates, shell syntax and whitespace checks. Python schema tests passed
15 and backup tests 7. Node passed 3 of 5; two unchanged harness checks fail before
assertions (missing mock DOM API and incomplete CSS-token parsing). See
[known issues](known-issues.md#resolved-local-node-check-failures). No full backend,
SQL, emulator, browser, provider/jobs, recovery or deployment run was performed.

## Source navigation

[Build defaults](../../../../Directory.Build.props), [solution](../../../../SmartFarm.slnx),
[Android build](../../../../mobile/SmartFarm.Android/app/build.gradle.kts),
[tests](../../../../tests), [CI](../../../../.github/workflows),
[Phase A verification](../../../../scripts/verify-phase-a.sh),
[recovery](../../../../database/RECOVERY.md).

## See Also

[Architecture](architecture.md), [Database](database.md), [Android](android.md),
[Automation](automation.md), [Decisions](decisions.md), [Progress](progress.md).
