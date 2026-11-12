# Dependencies and local development

> Sources: SmartFarm build manifests, launch profiles, scripts, and tests
> Raw: [Architecture sources](../../raw/project/2026-10-27-architecture-source.md); [Android sources](../../raw/project/2026-10-27-android-source.md); [Progress sources](../../raw/project/2026-10-27-progress-source.md); [Database sources](../../raw/project/2026-10-27-database-source.md); [Host test fixes and results](../../raw/project/2026-10-27-host-test-fixes.md); [JPEG fixture and photo verification](../../raw/project/2026-10-27-valid-jpeg-photo-fixture.md); [Functional E2E evidence](../../raw/project/2026-10-27-functional-e2e.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Documentation verification](../../raw/project/2026-11-11-documentation-verification.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

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

CI now has `verify-smartfarm.yml` for Release .NET tests, Android build/JVM and
device-test compilation, and `phase-a-stability.yml` for backup/schema/script checks.
CI additionally runs Node, public Chromium and Tailwind/parity checks; neither
workflow supplies a SQL instance, Android emulator or deployment. Workflow existence
does not prove a green remote run or enforced branch rules. Placeholder test
folders outside the solution do not establish coverage.

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
