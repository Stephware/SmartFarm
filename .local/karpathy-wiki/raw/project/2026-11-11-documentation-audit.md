# SmartFarm documentation audit evidence

> Source: local source audits at ac624be; user documentation synchronization instructions
> Collected: 2026-11-11
> Published: Unknown

These are original source-review observations, not runtime acceptance. Application files were inspected read-only. Prior raw snapshots are immutable.

## backend review

# SmartFarm backend/data/test/CI source audit

Reviewed 2026-11-11, local HEAD `ac624be` (`dev`, `origin/dev` shown together by local git). Read-only source review; no application build/test, live SQL, deployed Worker/provider, or remote GitHub status check performed. No credentials, runtime records, photo contents, or private configuration read. This is source evidence, not a runtime verification certificate.

## Scope and authority

Reviewed Core entities/contracts/enums/interfaces/rules; Infrastructure host registration, Identity, farm authorization/enrollment, business services, repositories, persistence mappings/migrations/snapshot, photo store and weather provider; API program/controllers/models; Worker program/jobs/scheduler/automation/storage configuration; SQL initialization/upgrades/verification/recovery, verification scripts, test inventory and relevant assertions; two executable CI workflows; local first-parent Git history from PR61 through PR78 and subsequent merges. Source takes precedence over October wiki and historical docs.

## Current architecture and permissions

- `SmartFarm.slnx`: Core, Infrastructure, Api, Web, Worker; five active backend test projects (Api.Tests, Auth.Tests, SchemaVerification, Weather.Tests, Worker.Tests). Android is outside the solution. `Directory.Build.props`: `net10.0`, nullable/implicit usings; `global.json`: SDK `10.0.100`, latestFeature roll forward. Infrastructure pins Identity/EF/PKCS `10.0.13`; Worker pins Hangfire `1.8.25`, Hosting `10.0.13`, Newtonsoft.Json `13.0.4`.
- `Infrastructure/DependencyInjection.cs`: scoped repository interfaces, `IUnitOfWork`, farm access/enrollment/management, crop, monitoring, fieldwork-stock, inventory, harvest, report, finance, task and weather-read services. MVC/API independently call the same services/database; Web HTTP clients are legacy/support classes, CSV builders remain active. Core has no database/provider dependency. Finance/reports/weather reads, automation and sync ledger legitimately use DbContext directly; do not claim every service only uses repositories.
- `Infrastructure/Persistence/SmartFarmDbContext.cs`: SQL Server Identity Guid context, Identity schema Version1 (no passkey table), explicit configurations, conventional FK index creation removed to match SQL-first schema. AddSmartFarmPersistence only configures DbContext; startup does not connect, initialize, upgrade or apply migrations.
- `Infrastructure/Security/FarmAccessService.cs` + `Core/Services/Security/FarmAccessRules.cs`: actor must exist and be enabled; farm must be unarchived; owner uses Farm.OwnerUserId, manager/worker uses active membership Role 1/2. Global Identity roles do not confer farm access. View permits Owner/Manager/FieldWorker; Manage permits Owner/Manager; ManageMembers Owner only. Business permission denials generally become missing resources, avoiding cross-farm disclosure.
- `Infrastructure/Services/Farms/FarmManagementService.cs`: anyone with enabled account can create own farm; Owner/Manager edits farm and manages plots; only Owner archives farm and changes/removes members. Farm archive refuses active crop cycles or positive stock; plot archive refuses active crop. Soft archives preserve history. Farm timezone must resolve, coordinates must be present as pair with valid ranges and at most six decimal places; plot positive area uses two decimal places. Farm/plot mutations use Base64 eight-byte SQL rowversions.

## Authentication/enrollment and API conventions

- `Identity/IdentitySetup.cs`: MVC Identity cookie `SmartFarm.Auth`, HttpOnly, Lax, 8-hour sliding lifetime, Secure outside Development, cookie validation rejects disabled/missing users. API only Identity opaque bearer tokens (not JWT), 30-minute access, 7-day refresh. Password length 10, digit/lower/upper required, symbol optional; unique email; 5 failed attempts / 10-minute lockout. Confirmed email remains unnecessary. No password reset/email-confirmation/2FA flow was found in active API.
- `Api/Controllers/AuthController.cs`: register returns 201 and requires separate sign in; login checks disabled account and lockout; refresh unprotects token and validates expiry/security stamp/disabled state; me checks live enabled user. No server logout/revocation route. `AccountRegistrationService.cs` now takes name/email/password only, never grants farm membership, and catches persistence failures with sanitized failure. Old claim that signup optionally requests a farm is superseded: account creation is independent from enrollment.
- `Infrastructure/Security/EnrollmentRateLimiting.cs`: fixed-window registration 5/10 minutes by IP; login 20/minute by IP; enrollment 10/minute by authenticated user, fallback IP; zero queue; HTTP 429 with Retry-After when available and generic message. Both hosts register/use rate limiting. Policies are attached to relevant API registration/login/request/decision/cancel/rotation actions. These are per-process limiters; no distributed limiter is implemented.
- `FarmEnrollmentService.cs`: normalize farm code trim/remove hyphens/uppercase; accept 12 ASCII hex chars; active farm/user required; current owner/member gets conflict; repeat pending request returns existing request. Requests, decisions, cancellations and rotation take serializable transaction with farm lock first. Approval creates FieldWorker membership, never Manager. Repeating same decision/cancel idempotent; conflicting final state rejected. Applicant can cancel own Pending request; `Canceled` history attributes review to same user. Owner overview splits Pending and history. Owner may rotate code only when submitted CurrentCode matches exactly; cryptographic 6-byte hex new code, unique-index collision retry. Existing pending request decisions do not depend on a still-current code. Request and decision notifications (Type 4) participate in same transaction and stable deduplication keys. Retired invitation endpoints return Gone, no active email invitation flow.
- `Api/Program.cs`: controller APIs, no OpenAPI/swagger setup; Development fallback reads only Web DB secret when explicit API DB unset; outside Development generic error handler/HSTS/HTTPS redirection. DbException middleware emits sanitized 503 JSON with DB-health guidance and logs server-side. Development-only `/health/database` checks connection plus Users/Farms/FarmMembers; `/api/status` is liveness, not full readiness. Shared Data Protection application name includes environment; optional KeyRingPath must be absolute. Configure shared protected keys and private durable photo mount for deployments.
- `Core/Contracts/NamedCodeJsonConverters.cs` and `Api/Controllers/ReferenceDataController.cs`: supported symbolic input names coexist with old numeric codes; response numeric values retained, many DTOs add computed readable `...Name`; anonymous `/api/reference-data` lists stable code labels. Not every byte property has converter (issue codes remain numeric), so do not describe universal string enums. Domain errors map in FarmApiControllerBase to 404 generic missing, 409 conflict, 400 invalid; `[ApiController]` validates model input; authentication yields 401.

## Current API surface

All farm business endpoints below require bearer authentication and services enforce farm scope.

- `/api/auth/register|login|refresh`, GET `/api/auth/me`; anonymous `/api/status`, `/api/reference-data`.
- `/api/farms`: list/create; `/{farmId}` get/update/archive; nested plots list/get/create/update/archive; `/access`; `/members` list/change role/remove. Old member invitation/accept routes respond Gone.
- `/api/farm-join-requests`: mine/submit; `/api/farms/{farmId}/join-requests` owner overview; `/{requestId}/decision`, `/{requestId}/cancel`; `/join-code/regenerate`.
- `/api/farms/{farmId}/crop-types` list/create; `/crop-cycles` list/get/update/transition; `/plots/{plotId}/crop-cycles` plan.
- `/api/farms/{farmId}/crop-cycles/{cycleId}/monitoring`: get, POST activities/observations, PUT observations/{observationId}/follow-up, POST observation photos, PUT observation photos/{photoId} for idempotent stable photo identity, GET photos/{photoId}. No direct public photo URL.
- `/api/farms/{farmId}/inventory`: list/create/item get/update/archive/history/movement/reverse/activity options. `/crop-cycles/{cycleId}/harvests`: get/record/void.
- `/api/farms/{farmId}/tasks` list/create/update/close; `/api/notifications` list, PUT `/{id}/read` scoped to current user.
- `/api/farms/{farmId}/dashboard`, `/reports/harvests?from&to&plotId&cropTypeId`, `/weather`, `/finance` list/record/void, `/finance/insights?from&to`.
- POST `/api/sync/operations/{operationId}`: exact matching payload for operation type activity, activity_stock, observation, inventory, harvest. No direct standalone API activity_stock route; that operation goes through sync endpoint (MVC calls shared service).

Source: every class in `src/SmartFarm.Api/Controllers/`; contracts live under `Core/Contracts/` and interfaces under `Core/Interfaces/`.

## Crop, monitoring, tasks, inventory, harvest rules

- Crop types are global catalog; Owner/Manager of an accessible farm can create, read active types; no edit/deactivate. Crop planning and plan edits Owner/Manager; only Planned editable. `CropCycleRules`: Planned -> Active/Cancelled; Active -> Completed/Failed/Cancelled; terminal cannot reopen. Actual planted date required on activation and expected harvest cannot precede it; Failed/Cancelled reason required (<=300), reason prohibited for Active/Completed. Active uniqueness enforced by filtered SQL index per plot, not merely preflight. Date maximum bug remains fixed in shared TaskRules integer-day watering iteration.
- `CropManagementService.TransitionAsync`: activation completes pending planting tasks and dismisses unread reminders; terminal transitions cancel all pending cycle tasks and dismiss reminders, saved with crop mutation. Harvest does not complete crop.
- `MonitoringService`: members including workers can read history and add activity/observation on active crop with active plot; management required only for issue follow-up. Activities server timestamp, allowed types Watering/Fertilizing/Pesticide/Weeding/Other; inspections are observations. Explicit FarmTaskId must match farm/cycle/activity task type and completion permission; closed task conflicts. Otherwise compatible due watering/fertilizing task can be auto-selected. Serializable/ambient transaction wraps activity + task + reminder changes. Worker may fulfill generated tasks but may not fulfill somebody else's manual assignment; standalone task close still only assigned worker or manager.
- `RecordObservationInput`: health 0..3, stage<=40, notes<=1000; structured issue category 0 None, 1 Pest, 2 Disease, 3 Nutrient deficiency, 4 Environmental stress, 5 Other; severity 0 None, 1 Low, 2 Moderate, 3 High. Category None requires severity None; an issue requires severity>=1 and health NeedsAttention/Critical. New issue defaults FollowUpStatus Open(1), none is 0. Old request bodies default to no issue. `UpdateFollowUpAsync` Owner/Manager, eight-byte rowversion; Open(1)/Resolved(2), optional task must belong to same farm/cycle with Inspection(4) or Other(6). No automatically created follow-up task and task completion does not itself mark issue resolved; manager confirms separately. Follow-up can be updated on historical ended crop because recording=false, within accessible farm.
- Photos: maximum actual file 5 MiB; request limit 6 MiB; JPEG/PNG/WebP signature must match declared content type; random storage keys and safe-key validation, no public static access. Stable photo PUT retries same photo ID and compares uploader, media type, length and exact saved bytes; mismatched content conflicts. Private store cleans file on failed save and failed DB insert. Local filesystem only. `ObservationPhotoStorageRegistration`: non-Development needs explicit absolute non-root path outside any wwwroot segment; Development common Web/App_Data fallback; multiple hosts need same mounted durable volume.
- `InventoryService` + StockRules: Owner/Manager item setup/metadata/receipt/adjustment/reversal/archive; any member consumption. Initial stock becomes Opening ledger row; updates cannot overwrite quantity; unit immutable once any ledger row exists. Consumption negative and links to active-crop activity same farm; receipt positive without activity; adjustment nonzero without activity plus reason. Decimal(18,3) quantities, stock nonnegative, rowversion conflict protection. Reversal creates opposing audited row with unique one-reversal-per-original; cannot reverse reversal or cause invalid balance. Archive requires zero stock. Low stock quantity <= reorder threshold. MetadataVersion GUID changes on item metadata edit/archive, not ordinary stock movement; it is not EF concurrency token (rowversion is).
- `FieldworkStockService.cs` Phase C2: fertilizer/pesticide activity only; positive <=3dp quantity used, supply must be unarchived correct Fertilizers/Pesticides category. Serializable transaction when no ambient transaction; monitoring activity + matching task fulfillment + stock consumption use same DbContext and transaction; stale rowversion/insufficient stock must roll all back. Uses existing sync ambient transaction safely.
- `HarvestService` + HarvestRules: any member records repeated positive partial harvest with <=3 decimals on active crop; unit copied from crop. Serializable/ambient transaction protects active check, insert, and harvest task fulfillment; optional harvest task selection can include future task as repository rule. Crop completion explicit. Owner/Manager void reason 3..300; retained history; conditional SQL UPDATE prevents duplicate void. Totals exclude voids, no editing/deleting original harvests.
- `FarmTaskService`: Owner/Manager create/edit pending tasks; linked cycle must be current Planned/Active on unarchived plot; assignee active owner/member; eight-byte rowversion for update/close. Workers close only own assigned tasks; Completed/Skipped/Cancelled. Due-date/assignee changes dismiss existing unread reminders. Notifications current-user only, latest 100; mark-read idempotent.

## Reports and finance: Phases D1-D3

- `FarmReportService.HarvestsAsync`: farm View access includes workers; report from/to validated between 2000-01-01 and 2100-12-30 and correct ordering; optional GUID filters reject empty GUID. UTC harvest timestamps filtered by farm-local day boundaries with inclusive To represented as exclusive next-day UTC; legacy invalid zone falls back UTC. Query retains void history; rules exclude voids from totals. Report exposes unit, crop/plot/unit, monthly/unit groups, raw entries and additive CycleTotals (default empty list for older constructors/clients).
- `HarvestReportRules`: CycleTotals key includes cycle ID + crop + plot + unit, aggregates repeated partial harvests with valid count/first/last farm date; keeps separate planting cycles and units. Filters are applied before aggregation, so filtered cycle totals are not lifetime yield. `Web/ViewModels/Reports/HarvestAnalyticsBuilder`: monthly max 12 bars, cycle/crop-plot max 10 bars per unit; current active PlotDetails area used for optional quantity/m² estimate, not historical captured plot area and not normalized scientific yield. Full totals/table/CSV not restricted by chart bar limits.
- `FarmReportService.DashboardAsync`: active plots/crop status/task/overdue/low-stock/current concerning crop counts, last 30 farm days harvest by unit, five recent valid harvests; additive AttentionItems bounded four each overdue tasks, stock and current concerning active crops. Concerning uses latest observation by ObservedAtUtc, not any historic bad observation and not issue resolution status. Queries stay farm-scoped. DTO additive fields preserve older clients.
- `FarmFinanceService`: manual PHP cash ledger independent from inventory/harvest. View for all farm members, record/void Owner/Manager. Kind Expense/Income only, trimmed category 2..60, PHP positive 0.01..999999999999.99 (2 decimals), date 2000-01-01..2100-12-30, notes<=500, optional cycle must belong to farm (not necessarily active). Records immutable apart from void metadata; repeated void conflicts; reason 3..300. List shows newest 500 entries including voids while all-time totals aggregate all nonvoid entries.
- Insights apply optional inclusive ledger EntryDate range and SQL aggregate every matching nonvoid record, independent of 500-history display cap. `FarmFinanceInsightRules` produces overall cash income/expenses/net, category totals+counts, descending monthly trend, cycle allocations and Farm-wide/unassigned group; labels include crop/plot/short cycle ID. No sale price estimate, inventory costing, accounting profit, automatic revenue or cost generation.
- Web CSV sources `Clients/HarvestCsv.cs`, `HarvestCycleCsv.cs`, `FinanceInsightsCsv.cs`: RFC-style quoted cells and double-quote escaping; prefix apostrophe for formula-like untrusted names/notes including leading whitespace/control characters; invariant decimal formats (0.000 quantities / 0.00 money). Cycle CSV exports exact filtered totals, IDs/units/first/last dates/count/filter context; empty report is header-only. Finance CSV includes summary/month/category/cycle sections for same authorized filter. Controllers reauthorize through shared services; farm-scoped cookie auth. CSV is Web capability, not JSON API download endpoint.

## Sync contracts and concurrency

`Api/Controllers/SyncOperationsController.cs`: stable nonempty operation/farm/target GUID; View permission rechecked before ledger access including replay; require exactly one matching payload. SHA-256 serialized input; serializable SQL transaction, `WITH (UPDLOCK,HOLDLOCK)` ledger read locks missing GUID gap, preventing concurrent duplicate mutation. Existing operation must match actor/farm/type/hash or 409; replay returns saved result with Replayed=true without repeat write. Successful business write and completed SyncOperation result committed together. Failure rolls back ledger/business work. Photo idempotency is separate stable PUT route, no photo bytes embedded in sync JSON. No queue ledger expiration/cleanup job was found.

## Database state and deployment boundaries

- `database/02_CreateSchema.sql` fresh base remains 7 Identity + 17 business = 24 tables; refuses wrong DB and any existing user table (any schema), wraps transaction, explicit filtered-index SET options. Does NOT include finance ledger; optional `06_FarmFinance.sql` adds one table. Base now includes CropObservation issue fields/follow-up task FK/rowversion, InventoryItem.MetadataVersion, forecast probability/wind. No application startup schema changes.
- `tests/SmartFarm.SchemaVerification/SchemaModelTests.cs` current full EF model assertions exactly: 25 entities, 219 properties, 47 FKs, 52 indexes, 42 checks, 34 defaults, 6 RowVersion columns. Finance contributes 13 columns, 4 FKs, 3 indexes, 3 checks, 1 default. Base therefore 24 tables/206 columns/43 FKs/49 indexes/39 checks; `03_VerifySchema.sql` dynamically permits optional finance and verifies matching counts with six rowversions. Do not treat these as live metadata measurements.
- Current migrations: empty ExistingSchemaBaseline; FarmerRegistrationApproval; FarmEnrollmentWorkflow; CropIssueFollowUps (4 assembly migrations per schema test). Approval migration mirrors explicit 04 SQL; workflow mirrors explicit 05 SQL and expands cancellation/notification checks. Both downs blocked. CropIssueFollowUps Up alters observation adding category/severity/status/link/rowversion/defaults/index/check/FK; Down throws to preserve history. Fresh updated schema already includes those columns: do not blindly apply issue migration to fresh SQL-first schema without reviewed EF adoption. Finance table has separate rerunnable `06_FarmFinance.sql`, no dedicated finance migration in assembly. MetadataVersion/weather changes likewise exist in current create script/mapping but no dedicated forward migration in listed assembly; existing database compatibility must be reviewed rather than implied complete.
- `database/05_FarmEnrollmentWorkflow.sql`: wrong DB guard, requires existing enrollment and notifications tables; rerunnable replacement of state/review/type checks; cancellation decision review integrity. `06_FarmFinance.sql`: wrong DB guard, requires farm/crop tables, creates only if absent, preserves existing data. `04_FarmerRegistrationApproval.sql` rerunnable enrollment backfill/upgrade; explicit owner backup/review, no EF history.
- `database/03_VerifySchema.sql`: read-only structural counts, trusted/enabled constraints, required filtered indexes, six rowversions, optional finance; historical error message strings do not reflect finance addition but formulas do. `database/verification/verify_schema.py` offline parser compares SQL CREATE with EF-generated and/or exported catalog JSON, ignores exact dbo.__EFMigrationsHistory only; no network/database writes; scoped supported syntax, not general SQL engine. README counts still old 24/201/42/38/48; correct current base above. To compare full EF now, need reviewed combined schema reference including optional finance, since base CREATE intentionally excludes finance.
- `database/README.md` currently still documents only 01-04 and old inventory of setup files; insufficient as full current upgrade runbook. `database/RECOVERY.md` newest recovery acceptance scope includes SQL, photo volume, shared key ring/configuration and unuploaded device queue; mandatory separate disposable restore rehearsal, DBCC CHECKDB, test host/account/private-photo/stock/harvest/sync verification, no production replacement. No evidence rehearsal executed here.

## Worker and weather

- `Worker/appsettings.json` NOW `Hangfire.Enabled=true`, WorkerCount=1, InitializeSchema=false, RunJobsOnStartup=false; OpenMeteo expected HTTPS origin/15-second timeout. October wiki disabled-by-default claim is outdated. Without explicit application and separate jobs DB config, enabled worker fails fast. Disabled override can still start idle without storage. No HTTP listener/dashboard.
- `BackgroundProcessingRegistration.cs`: reject unsafe/reserved/missing/attached jobs DB and same catalog as app; recommended SmartFarmJobsDB; worker count1..64; Hangfire SQL storage PrepareSchemaIfNecessary only opt-in InitializeSchema, heavy migrations false; default queue, 15-second poll, 5-minute batch/invisibility. `database/05_CreateJobsDatabase.sql` creates separate DB only; first reviewed schema initialization requires explicit true then return false. `06_VerifyHangfireJobs.sql` read-only recurring registration/worker heartbeat/all-three successful job queries; not a health endpoint.
- Scheduler registers smartfarm-worker-check hourly; smartfarm-tasks-and-reminders hourly; smartfarm-weather-check `0 */6 * * *`; UTC cron. Startup always enqueues heartbeat; optional RunJobsOnStartup enqueues task+weather for local verification. Actual farm dates use farm timezone; invalid zones skipped/logged.
- FarmAutomationService task scan: unarchived farms, current Planned/Active on active plots; horizon today+7, stable dedupe keys crop/type/date. Planned planting, active watering and expected harvest, initial assignee owner; task completion/reminder eligibility rules as above. Due/overdue reminders dedupe per task+due date; recheck enabled recipient and current membership, fallback enabled owner.
- New weekly low-stock digest: top 6 low items for unarchived farm; Monday-start local week key `stock:{farmId}:{yyyyMMdd}`; Type Other(3), active Owner/Manager only; dedupe per recipient/farm/week. No direct email/SMS/push delivery.
- Weather adapter requests 4 days with timezone=auto; Worker intentionally stores usable today through today+2 (3-day window), validates plausible numbers; read service returns latest persisted daily forecast/advisories in today..today+4, so view window does not imply Worker stores 5 days. Provider reading is Worker concern; opening API/MVC weather does not fetch. Forecast probability and wind optional; missing values must not falsely resolve warnings. Rain>=20mm (severe>=50), wind>=50km/h (severe>=80), high>=35C (severe>=40); warnings dedupe farm/day/type and notifications advisory/severity/current-user; newer known clearing signal resolves old advisory.
- `WeatherServiceCollectionExtensions` validates expected OpenMeteo origin, HTTPS base URL, timeout1..120, optional header credential validity; disables redirects and HTTP logging, restricts request same origin. `WeatherHttpClient` validates finite coordinates, total send+parse timeout, caller cancellation preserved, sanitized transport/provider/JSON errors. Weather service/adapter tests fake external provider; no source review proves live provider/deployed jobs.

## Verification and CI reality

- `scripts/verify-phase-a.sh`: full Release restore/build/test; Python schema and backup-script unit tests; Android debug build, JVM tests and instrumentation compilation by default; optional --sql requires initialized disposable SQL + `SMARTFARM_REQUIRE_SQL_TESTS=1` and runs BusinessWorkflowSqlTests; optional --device requires authorized adb device; --skip-android explicit and incompatible with --device. Script prints limitations for omitted SQL/device and recovery/browser acceptance.
- `scripts/verify-phase6.sh`: strict live API+MVC `/health/database` and `/api/status`/home HTTP200, disposable allowed-prefix SQL variables, .NET Release test, Android build/JVM tests, optional device instrumentation. It explicitly says Worker runtime requires separate log/dashboard evidence. `verify-phase1.sh/.ps1` older combined checks remain.
- `tests/SmartFarm.Api.Tests/BusinessWorkflowSqlTests.cs`: no SMARTFARM_TEST_CONNECTION returns normally unless SMARTFARM_REQUIRE_SQL_TESTS=1 then fails; enforce disposable SmartFarmEnrollmentTest_/SmartFarmEnrollmentBackend_ DB prefixes before writes. Real workflow source covers owner/worker/outsider paths, crop state, linked field activity/task, stock/harvest and idempotent sync including new activity_stock rollback/replay/payload mismatch. `FarmEnrollmentSqlTests.cs` independently uses SMARTFARM_ENROLLMENT_TEST_CONNECTION and returns normally when absent. Passing ordinary suite does not prove SQL tests executed.
- Other backend tests include API auth/scheme/enrollment/input/readable-code/health/endpoint contracts; MVC host independence/role access/form recovery/CSVs/timeline/analytics/issue validation; schema model/snapshot/SQL safety/repository registration; weather parsing/origin/timeouts; Worker job registrations/schedules/date boundaries/low stock/business rules. FieldworkStockServiceTests tests proper category, transaction ownership, stale stock rollback and ambient transaction. C1 issue rule tests are source unit coverage, not demonstrated live SQL follow-up flow in this audit. No running test count should be inferred from test source inventory.
- `.github/workflows/verify-smartfarm.yml`: PR to dev/main, push dev, manual trigger; read-only repo permission/concurrency cancellation. Ubuntu .NET10 Release restore/build/test; Android Java17 SDK36 debug build+assembleAndroidTest+JVM tests. SQL integration not provisioned; device instrumentation compiled, not executed. No deployment pipeline or remote CI run/branch-protection verification here.
- `.github/workflows/phase-a-stability.yml`: PR dev/main, push dev; Python3.12; bash syntax, Python compile, backup checker unit tests, schema verifier unit tests. New executable CI supersedes wiki claim workflows placeholder/planned. JS `tests/web-ui/site.test.cjs` and `farm-location.test.cjs` exist but these two YAML files do not run Node tests.
- `scripts/verify_sql_backup.sh`: read-only RESTORE VERIFYONLY WITH CHECKSUM against operator-selected native backup path; uses env credential, escapes path, no restore; explicitly not evidence of recovery. `verify_photo_backup.py`: compares relative paths and SHA256 in separate nonnested real directories, rejects symlinks and empty backup; matching bytes do not prove SQL photo keys align. `scripts/tests/test_verify_photo_backup.py` covers success/failure. Recovery acceptance remains an operator exercise, not completed by CI.

## Local history anchors for progress refresh

First-parent source evidence: PR61 34710c7 UI phase1; PR62 22fed90 UI phase2; PR63 6bcb9d1 smoke copy; PR64 9a345b7 UI phase3; PR65 ed9a513 UI phase4; d5a28b7 Android session/errors; f7541ef weather freshness/cache; PR68 8619bde UI phase5; PR69 fcdbe8a UI phase6; PR70 a266d11 UI phase7; PR71 a11f936 UI phase8; PR72 3016c31 Phase A stability/recovery; PR73 1f6440b Phase B daily work; PR74 c4e7f61 Phase C1 crop health; PR75 9d7a903 Phase C2 activity stock; PR76 a5e592d Phase D1 yield comparison; PR77 a8e7c94 Phase D2 finance insights; PR78 cfcabb9 Phase D3 cycle export. Later Pine/Gold frontend merges f96bf33,0a2ed4f,dcfbc34,1a12e8f,7a0716b,69a2c28,ac624be follow PR78 and must not be omitted from current HEAD status. Do not infer remote PR numbers for two unlabeled merge messages without remote evidence.

## Wiki claims to supersede or qualify

1. architecture mostly stable; expand scoped services for finance/fieldwork-stock and new dashboard/report read models, rate limiting. Cookie/bearer split remains correct. Correct signup optional enrollment assumption.
2. database old 24 tables/201 cols/42FK/48 indexes/38 checks now wrong current base/full distinctions above; workflow/issue migrations and optional finance missing; initialization instructions stop too early for full app.
3. features missing finance ledger/insights, cycle comparisons/CSV, structured crop issues/follow-ups, combined stock+activity, weekly stock digest, dashboard attention drilldowns and current UI surfaces. Android report parity should be taken from Android auditor because later mobile reports exist.
4. automation disabled default now false claim; enabled true, startup heartbeat/optional immediate runs and weekly digest missing; rain probability/wind handling should be added.
5. development/known-issues CI placeholder statement outdated; rate-limit absent statement outdated; no tests run during this audit must remain explicit. Android memory-only tokens/stale post-sync inventory claims likely superseded by later history but require Android source review, do not simply carry them forward.
6. decisions must separate SQL-first reviewed adoption from incomplete forward schema upgrade coverage; finance totals manual cash, current-area yield estimate, unit isolation, sync atomicity and private filesystem photo deployment constraints remain important.
7. recovery/build/dev gates implemented as scripts/CI, but real SQL/device/restore/provider/deployment acceptance not established by source existence or this audit.


## android review

# SmartFarm Android source audit

Reviewed 2026-11-11 in `/Users/stephen/Documents/SmartFarm`, current local history ending at `ac624be` (Merge Pine and Gold frontend polish). Read-only audit: no application, schema, wiki, Git, configuration, or runtime data edits. This report is the only artifact written. Source existence and local merged history establish implementation, not deployed/runtime correctness. No build, JVM test, emulator/instrumentation, API request, or Worker run was executed for this audit.

## Important corrections to the October wiki

- Android now has a farm overview/dashboard-style home and a dedicated `insights/{farmId}` screen. The older statement that Android has no dashboard/report screen is superseded. Reports remain a read-only subset of Web functionality.
- Android tasks now support Owner/Manager create and edit, alongside listing, completion, notifications, Today work grouping, and active-crop shortcuts. The old “list/complete only” statement is superseded.
- The selected offline write set now includes `activity_stock`, an atomic activity plus linked supply consumption request. Structured crop issues are carried inside queued observations. Issue follow-up decisions remain online-only.
- CI workflows now exist. `.github/workflows/verify-smartfarm.yml` builds Android, compiles instrumentation tests, and executes JVM unit tests; it explicitly does not execute instrumentation tests. `.github/workflows/phase-a-stability.yml` provides recovery/schema script checks. The old “no executable workflow” statement is superseded.
- Memory-only sessions and the lack of sync-driven refresh of open inventory state remain true in current source. Their runtime symptoms were not newly reproduced here.

## Architecture and source map

All Kotlin paths below are relative to `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/`.

- `MainActivity.kt`: edge-to-edge Compose activity; activity-scoped ViewModels assembled through factories.
- `SmartFarmApplication.kt`: lazy `AppContainer`, WorkManager `Configuration.Provider`; no startup upload scheduling until login.
- `di/AppContainer.kt`: manual dependency injection; one lazy Retrofit client, repositories, Room database, offline cache, sync repository, WorkManager. No Hilt/service locator framework beyond this container.
- `navigation/SmartFarmNavHost.kt`: Navigation Compose routes, auth/logout cleanup and feature callbacks.
- `data/remote/api/*Api.kt`, `data/remote/dto/*Dtos.kt`: Retrofit contracts/Gson transfer types. No direct database access or Web/MVC scraping.
- `data/repository/*Repository.kt`: authenticated requests and selected read-cache boundaries. `data/local/OfflineReadCache.kt` implements API-first snapshots.
- `features/*/*ViewModel.kt`: StateFlow state, coroutine orchestration, validation, local save receipts. Composables collect state with lifecycle support and call ViewModel methods through navigation callbacks.
- `data/sync/*`: operation/photo queues, worker, upload receipts, image optimization/private files.
- `ui/theme/Theme.kt`, `ui/components/*`: Material3 tokens, shared presentation and safe-action components.

Navigation: welcome → login/register → farms → farm detail. Account opens farms, sync status, notifications, logout. Workspace bottom tabs cover farm home, tasks, crops, inventory, account; tabs are shown only on the primary farm/tasks/crops/inventory routes. Nested routes include monitoring with optional `quick=watering|inspection`, inventory item, crop harvests, and farm insights. The bottom bar retains the active farm as shared navigation root. A dedicated Android monthly agenda screen is still absent.

Farm detail has Overview, Weather, Plots, Members and role-gated Settings sections. The new home composes crop counts/current cycles, personal due work, saved weather summary, and links to work/crops/stock/reports. `features/farms/FarmHomeOverview.kt` avoids presenting cached tasks as current work and labels saved crop records.

## Build and delivery configuration

Authoritative tracked files: `mobile/SmartFarm.Android/{build.gradle.kts,settings.gradle.kts,gradle.properties,gradle/wrapper/gradle-wrapper.properties}`, `app/build.gradle.kts`, and main/debug manifests. The pre-existing untracked `gradle/gradle-daemon-jvm.properties` is a local environment artifact and is not treated as project configuration.

- Application/namespace `com.smartfarm.mobile`; user-facing branding AniGo; version `0.1.0`, version code 1.
- AGP 9.1.1, Compose compiler plugin 2.2.10, KSP 2.3.6, Room plugin/runtime/compiler 2.8.5; Gradle wrapper 9.3.1 with distribution checksum.
- Compile/target SDK 36, minimum SDK 26; Kotlin/Java bytecode target 17. CI installs Java 17. A pre-existing untracked daemon JVM criteria file locally asks for toolchainVersion 25; that is an environment caveat, not the tracked build requirement.
- Compose BOM 2025.10.01, Navigation 2.9.5, Lifecycle 2.9.4, WorkManager 2.11.0, Retrofit 3.0.0, OkHttp 4.12.0, Glide 5.0.5, ExifInterface 1.4.1. Versions are observed configuration, not a recommendation about current releases.
- Debug API defaults to `http://10.0.2.2:5081/`, overridable with `-PsmartfarmDebugApiUrl=...`. Release endpoint must be supplied using `-PsmartfarmReleaseApiUrl=https://.../`; blank default passes Gradle configuration but fails the client factory when used.
- `ApiClientFactory.kt` requires HTTPS except explicitly permitted debug HTTP hosts `10.0.2.2`, `127.0.0.1`, `localhost`; rejects URL credentials/query/fragment and missing trailing slash. Timeouts: connect 15s, read 30s, call 45s. Main manifest forbids cleartext, debug applies local network-security config.
- Permissions: Internet, coarse/fine location. FileProvider is private with temporary URI grants for photos. Application disables backup and uses extraction/backup XML rules. WorkManager default initializer is removed in favor of the Application configuration.

## API consumer inventory

`data/remote/api/` contains the complete Android consumer contracts:

| Contract | Consumer behavior |
|---|---|
| AuthApi | register, login, refresh, `/api/auth/me` |
| StatusApi | unauthenticated `/api/status` backend check |
| FarmApi | farms CRUD/archive, plots CRUD/archive, members role/removal, own enrollment request list/create/cancel, owner request overview/decisions, join-code regeneration |
| CropApi | crop types list/create; cycles list/plan/edit and lifecycle transitions |
| MonitoringApi | history, direct activity/observation contracts, online issue follow-up, legacy photo POST, stable-ID photo PUT, authorized private photo streaming |
| InventoryApi | workspace/list, eligible activities, item CRUD/archive, transaction history/post, reversal |
| HarvestApi | crop-cycle harvest list, record, void |
| TaskApi | farm tasks list/create/update/close; notifications list/read |
| WeatherApi | persisted farm weather overview at `/api/farms/{farmId}/weather` |
| FarmInsightsApi | filtered `/reports/harvests` and `/finance/insights` under farm |
| SyncApi | stable-ID POST `/api/sync/operations/{operationId}` |

Some direct POST contracts remain available in repositories, but current record forms route new activity, observation, inventory movement and harvest records through `SyncRepository`; do not infer actual UI transport solely from presence of a Retrofit method.

## Identity, role, and business presentation rules

`data/repository/AuthRepository.kt` stores access/refresh tokens only in memory and uses `/me` to establish the authenticated user ID. The refresh mutex serializes simultaneous 401 refreshes; waiting calls reuse a rotated token. A generation counter guards login, refresh, authorized results and retries against logout/account switches. Logout increments generation and clears credentials; it does not revoke already-issued server tokens.

Navigation logout clears feature states, private photo ViewModel caches and Glide memory. Cached reads/queued operations are scoped by user ID and remain durable; another account does not receive those rows through normal DAOs. After process death a persisted queue waits for login before authenticating. Login enables periodic sync and schedules pending work. There is no secure persisted token store, automatic offline login, push-notification transport, or Android background weather-ingestion worker.

Role gates are presentation aids; the server remains authoritative. Farm membership roles are the returned `Owner`, `Manager`, `FieldWorker` values, rather than trusted local global-role claims.

- Owners manage enrollment codes/applicants/decisions and membership; farm/plot management, crop plans/transitions and inventory item management are Owner/Manager UI actions. Offline mode suppresses administrative actions. Enrollment overview bypasses Room and is cleared on loads/role changes; only the owner sees it.
- Register submits name/email/password only. Joining is a later signed-in request; pending status does not add the farm to the authorized list. Current registration has one password field; an intermediate confirmation change in local history was superseded by `c901cea`.
- Owners/Managers can create/edit tasks using live farm/member/crop choices. Tasks and notifications require a live request; cached choices disable task mutations. Due dates use farm timezone. Personal open work includes tasks assigned to the actor and owner-visible unassigned tasks; overdue/today/upcoming buckets do not create jobs. Presets supply text only. Completion is visible to managers or the assigned actor; server authorization still decides. Active-cycle task shortcuts open existing logging/harvest flows; they do not auto-record completion.
- Crop screens separate planned/active cycles from terminal history, support manager planning/transitions, and allow historical monitoring/harvest viewing. Server `canRecord` controls field forms; active-crop requirements are revalidated by API/sync.
- Monitoring supports activity, observation/structured inspection, camera/gallery photo, history and issue follow-up. An issue requires category 1..5, severity 1..3, and health status 2 or 3; neutral entries use category/severity zero. Follow-up status 1..2 plus optional task and rowversion is online-only and shown to Owner/Manager. Invalid/offline supply options do not prevent logging an activity without stock.
- Inventory fields display per-item units, low-stock thresholds, archived/read-only status and immutable transaction history. Workers receive consumption UI; managers also receive receipt and signed adjustment UI. Consumption input is positive in the form and negated at the boundary; three-decimal precision and range are checked. Item management, archive, reverse are online-only; reversals require confirmation/reason. Pending changes are explicitly distinct from confirmed balance.
- Linked stock use is optional on activity kinds 2/3; matching inventory categories 2/3 are required. Quantity must be positive, at most three decimals, within saved stock, item in same farm and not archived, with rowversion. One `activity_stock` request represents activity plus stock use; client does not issue two independent saves.
- Harvest supports multiple partial entries, retained void history and totals. Recording does not finish the cycle. Server `canRecord`/`canVoid` govern controls. Recording queues a positive three-decimal quantity; voiding is online-only with a 3–300 character reason and confirmation. Lost-response copy directs refresh before resubmission.

## Offline support, retry and conflict behavior

`data/local/SmartFarmDatabase.kt` is Room schema 4, entities `LocalMetadata`, `PendingOperation`, `CachedResponse`, `PendingPhoto`, with explicit migrations 1→2→3→4. Exported schemas 1 and 4 are tracked under `app/schemas`; the old wiki claim that only schema 1 is tracked is superseded. `OfflineObservationQueue.kt` atomically publishes observation/photo rows in Room.

Read cache: API first, falls back only for IOException to previously saved JSON for current user/resource key. HTTP 401/403/404/validation and other HTTP failures never become cached success. Cached data is marked with saved-at time. Selected snapshots cover farms/details/plots/members, crop types/cycles, monitoring, inventory workspace/item/history/activities, harvest overview and weather. Enrollment, tasks, notifications and insights are online-only. Cached weather is separate from farm's administrative offline state; an offline weather response alone cannot disable live farm management.

Queued write types are exactly `activity`, `observation` (including structured issue fields), `inventory`, `harvest`, `activity_stock`; photos have a separate queue. No general farm/plot/enrollment/crop/task/finance CRUD queue, issue-follow-up queue, inventory reversal queue, or harvest-void queue exists.

`SyncRepository.kt` and `SyncWorker.kt`:

- Each operation gets a UUID and immutable serialized request. DAO selects pending rows for the user in created order, up to 40 per batch; uploadMutex serializes repository upload calls. UI watcher returns newest 100 operations, so it is a limited view rather than a total queue inventory.
- Network-connected unique one-off WorkManager job uses APPEND_OR_REPLACE and exponential backoff starting at 30 seconds. Login installs a KEEP periodic connected job every 15 minutes. Worker returns retry for network/server errors and success when no session or no retryable work remains.
- Operations upload before ready photos. Matching operation ID/type is required before row deletion. Observation responses must provide a UUID to resolve dependent photos. Combined receipts must contain valid activity and matching inventory IDs. Malformed/unrelated receipts move rows to review rather than deleting them.
- HTTP 400/403/404/409/422 mark manual review; 401 stops current run until session recovery; other HTTP errors and IOException request WorkManager retry. Cancellation is rethrown, not turned into success/error copy.
- Inventory and combined stock retry cannot use the ordinary retry button. A live stock preview checks item/category/archival/current balance, shows old change and expected balance, and requires explicit user confirmation. Confirmation refetches item and requires unchanged rowversion; then updates payload rowversion under the SAME operation ID and returns it to pending. There is no silent rebase. Server ledger/idempotency behavior must be documented from backend source separately.
- Other reviewed operations/photos can be retried explicitly. Discarding an observation operation also removes dependent queued photos/files. All lookup/delete/review methods scope by authenticated user; guessed IDs cannot remove another account's private file.
- No sync completion flow invalidates `OfflineReadCache` or refreshes InventoryViewModel/MonitoringViewModel/HarvestViewModel loaded values. UI notices/pending rows update, but open confirmed totals/history can remain old until reload. This continues to support the old known issue at source level; no new runtime reproduction was made.

## Photo safety and presentation

`data/sync/LocalPhotoFiles.kt`, `ImageOptimizer.kt`, `features/monitoring/MonitoringViewModel.kt`, `PhotoThumbnailFactory.kt`, `ui/components/GlidePhoto.kt`, and `MonitoringScreens.kt` implement private camera/gallery handling.

Photos are copied to app-private UUID filenames before queue publication; JPEG/PNG/WebP magic bytes are checked. Source cap is 30 MB; pixel bound is 120 million; optimized output cap is 5 MB. JPEG large-photo processing respects EXIF rotation/flip, aims at max edge 2560 and quality sequence 93/90/87/85; under-limit originals can be retained when savings are insufficient. Small PNG/WebP is preserved losslessly; oversized transparent images retain lossless handling. Queueing optimizes once, keeping identical bytes for retries. Camera sources and stale abandoned files are cleaned with a grace period; queued IDs from every account are protected.

Observation+photo staging creates both matching Room rows in a transaction; failures remove the file. Pending photo waits for server observation ID, then uses stable-ID PUT. Receipt must match photo and observation IDs before file/row removal. Private photo GET uses bearer authorization and bounded reads, not a public URL. Display/thumbnail caches are bounded and memory-only; thumbnail concurrent downloads are bounded. Glide disk caching is disabled. These guarantees are source behavior, not a fresh loss-of-network/process-death test result.

## Weather and reports/finance rollout

Weather is a saved farm forecast/advisory response; Android does not call OpenMeteo itself. `features/farms/WeatherFreshness.kt` marks data stale after 12 hours, invalid timestamps, or over five minutes in future. Weather details identify provider, retrieved time, saved date, wind/rain/temperature and active advisories, warn on offline copies and missing coordinates, and use farm timezone to select next available day. Farm settings coordinate changes invalidate cached weather and clear visible weather until reopen. Missing weather never prevents farm opening.

`features/insights/FarmInsights{Screen,ViewModel}.kt` and `data/repository/FarmInsightsRepository.kt` now expose live read-only reports. Date presets All dates / Last 30 days / This year derive farm-local dates; invalid timezone disables filtered presets. Harvest summaries exclude voids and separate units, show crop/plot comparison bars (up to 8 per unit) and monthly quantities. Finance shows manually entered PHP cash totals and up to six recent monthly trend bars. It explicitly does not equate recorded net cash with profit or infer sales from harvests. Pending mobile harvests are excluded. Harvest and finance failures are separate so one can render when the other fails.

Report loads cancel old jobs, clear old farm/period data first, rethrow cancellation and do not let stale completion reset a newer load. Logout calls insights.clear(). No Room report cache or offline report calculation. Mobile transfer DTO ignores server fields it does not need; it has no cycleTotals model/view, CSV exporter or detailed finance entry editor. Current UI directs users to Web for detailed entries and exports. Thus backend/Web D1–D3 scope is broader than Android even after mobile report rollout.

## UI tokens and local rollout history

Current `Theme.kt` is Harvest Gold + Pine: light/dark schemes selected by system dark mode, pine primary `0xFF153D2D`, harvest gold `0xFFB58A32`, darker gold ink `0xFF80601D`, cream `0xFFF7F8F4`; sans-serif Material3 typography and 6/10/14/18/24dp shape scale. `FarmDesignComponents.kt` supplies shared heroes, section cards/headers, status chips, metric cards, empty states and decorative plot artwork. `CropArtwork.kt` maps recognized crops with safe unknown fallback. Lists use LazyColumn/RecordPage, forms use saveable state/draft receipts, date/timezone inputs, busy disabling and confirmation components. No design alteration was made during this audit.

Local Git history establishes these merged implementation stages, not remote PR status or test success:

| Local merge | Scope relevant to docs |
|---|---|
| PR61 `34710c7` | initial design tokens/shared Android components and Web CSS |
| PR62 `22fed90`, PR63 `6bcb9d1` | Web entry/farm selection redesign and smoke-copy repair |
| PR64 `9a345b7`, PR65 `ed9a513` | Web dashboard/farm/crop and fieldwork/inventory/harvest UI |
| intervening merges `d5a28b7`, `f7541ef` | Android session refresh/logout, cancellation/error handling, weather freshness/cache separation |
| PR68 `8619bde` | Web reports/weather/finance/settings |
| PR69 `fcdbe8a` | Android farm workspace tabs, farms/plots/members/settings, crop/tasks/auth/welcome |
| PR70 `a266d11` | Android monitoring/photo, pending records, inventory/partial harvest and sync UI |
| PR71 `a11f936` | responsive/accessibility navigation/sync coverage |
| PR72 `3016c31` (Phase A) | recovery scripts/procedure, verification commands/CI, explicit required-SQL-test configuration failures |
| PR73 `1f6440b` (Phase B) | farm-local Today work center, manager task forms/live choices, crop shortcuts, note presets |
| PR74 `c4e7f61` (Phase C1) | structured inspection issues, persisted follow-up fields and online manager follow-up |
| PR75 `9d7a903` (Phase C2) | single atomic activity+stock queue and stock review/receipt flow |
| PR76 `a5e592d` (Phase D1) | backend/Web cycle yield comparisons and partial totals; read-only finance aggregate work |
| PR77 `a8e7c94` (Phase D2) | filtered backend/Web finance insights and safe authorized CSV |
| PR78 `cfcabb9` (Phase D3) | filtered planting-cycle totals CSV export on Web/backend |
| `f96bf33` through `ac624be` | subsequent Pine/Gold foundation, farm home/dashboard, crop illustrations, fieldwork filters/inspection simplification, inventory/harvest polish, Android weather/report consumers and welcome/sign-in polish |

PR66/67 are not inferred from the numbering gap; local named intervening merges provide the evidence actually available. Current register UI intentionally keeps name/email/one password, following last local simplification commit.

## Test evidence and limits

This audit inspected test source and CI commands but executed none. Do not report any suite as passing on this basis.

JVM source under `app/src/test/java/com/smartfarm/mobile/` includes Retrofit endpoint/JSON contracts (auth, enrollment, sync, photos, tasks, weather), AuthSessionSafety refresh/logout races, sync receipt matching including combined stock, API URL restrictions, network error messages, task farm-calendar/personal buckets/presets, practical quantities, inventory filters, weather freshness, chart bounds, crop art mapping, theme contrast and registration hints. `StandaloneApiIntegrationTest` uses MockWebServer; its name is not live production/API evidence.

Instrumentation source under `app/src/androidTest/...` includes actual Room/cache account isolation, offline queue/review/photo dependency handling, large-image optimization and thumbnails, draft retention/local-save feedback, responsive lists/forms, enrollment role/offline states, safe actions, Today manager/worker controls, quick logging, combined fieldwork controls, navigation/offline notices and weather-only cache fallback. CI compiles these only.

Configured Android CI command, not run by this audit: `bash ./gradlew --no-daemon :app:assembleDebug :app:assembleAndroidTest :app:testDebugUnitTest`, from `mobile/SmartFarm.Android`. Existing instrumentation would additionally need an emulator/device and `:app:connectedDebugAndroidTest` execution. Release endpoint/signing, emulator user flows, actual backend/SQL contracts, process-loss sync resumption, external weather ingest, and device accessibility/visual rendering remain unverified here.

## Documentation priorities

Update Android/features/known-issues/development/architecture/progress sections narrowly with the superseded claims above; preserve October runtime evidence as historical evidence with its date and scope. Explicitly document platform parity rather than calling all Phase D work Android functionality. Retain the session and post-sync freshness limitations. Do not convert test-file existence or merge commit messages into passing runtime/deployment claims.


## web-history review

# SmartFarm Web, history and documentation audit

Reviewed 2026-11-11, local HEAD `ac624be`. Read-only repository inspection; only this report was written. No application, SQL, browser, device or deployed runtime was exercised. Existing test code and historical verification records are evidence of coverage/intention, not fresh passing results. No credentials or runtime artifacts were read. Wiki index and relevant architecture/features/development pages were consulted first; current source overrides their October 28 summaries.

## Current Web architecture and source navigation

- `src/SmartFarm.Web/Program.cs`: Razor MVC with static CSS/vanilla JS, cookie Identity, scoped shared Infrastructure business services, antiforgery POST actions, enrollment/login rate limiting. Web does not depend on the API host for farm business operations. `Clients/*ApiService.cs` and their legacy route tests do not describe the active MVC request path; CSV formatters in `Clients/` remain active.
- `SmartFarm.slnx` contains five source projects (Core, Infrastructure, Api, Web, Worker) and five active backend test projects (Api, Auth, SchemaVerification, Weather, Worker). README says four and omits Api.
- Services registered in `src/SmartFarm.Infrastructure/DependencyInjection.cs:67`: farm/access/enrollment, crops, monitoring, atomic fieldwork-stock, inventory, harvest, reports, finance, tasks, weather. Core interfaces/contracts → Infrastructure service/repository/EF → MVC controller/viewmodel/Razor. Keep authorization in services; page controls alone are insufficient.
- Layout `src/SmartFarm.Web/Views/Shared/_Layout.cshtml` chooses guest/authenticated shells. Guest navigation: Home, Log in, Create account. Workspace account navigation: My farms, Notifications, My account, Switch farm, POST Log out. Context-specific farm navigation is authored once in `_FarmNavigation.cshtml`: Farm overview, Dashboard, Tasks, Crops, Plot layout, Finances, Inventory, Weather, Reports.
- `wwwroot/js/site.js:37` clones the authorized `_FarmNavigation` links into desktop sidebar, mobile menu and four shortcuts (Dashboard, Tasks, Crops, Inventory), preserving generated URLs/farm context and `aria-current`; More opens the native details menu. Original farm navigation remains usable without JS and is hidden only after successful enhancement. Do not maintain another independent route list.
- `wwwroot/css/site.css:1223` is the latest Harvest Gold + Pine token override, following older green/brown declarations earlier in that file. Main light values: pine `#153d2d`, gold `#b58a32`, gold ink `#80601d`, page `#f7f8f4`, surface white. Dark overrides start at line 1340. CSS cascade order matters: reading just the first `:root` yields the obsolete palette. Desktop sidebar width 264px; latest responsive workspace breakpoint 1000px, smaller refinements 560px. Inter is requested with system fallbacks, not proof of a bundled/downloaded font.
- `_Layout` loads versioned `site.css`, `dashboard.css`, `crop-art.css`, `fieldwork.css`, `harvest-inventory.css`, `insights.css`, `access-polish.css`; shared deferred JS is `site.js` plus `access-polish.js`. Monitoring additionally loads `fieldwork.js`; farm creation/editing uses `farm-location-picker.js`. No SPA/new JS package toolchain is evidenced.
- Progressive enhancements: skip link/main landmark, focusable linked error summary, explicit labels, 44px control minimums, named destructive confirmation via `form[data-confirm]`, copy-code fallback, optional password visibility. `fieldwork.js` previews JPEG/PNG/WebP and validates client MIME/5MB size; server signature/size/access checks remain authoritative. `Presentation/CropArtwork.cs` picks local SVGs for tomato/kamatis, eggplant/talong/aubergine, rice/palay/paddy, corn/maize/mais, otherwise sprout. Artwork is decorative display-name matching, not crop health inference.
- `Filters/ApiUnavailableFilter.cs` handles HTTP/database exceptions with 503 recovery views. GET gets a validated local retry URL; failed writes explicitly remain unconfirmed rather than inviting a blind POST retry.

## Workflows and role boundaries

| Area | Current implemented flow and evidence |
|---|---|
| Signup/login | **Latest UI has only full name, email and password.** `Views/Account/Register.cshtml`, `Controllers/AccountController.cs:66`, `ViewModels/Auth/RegisterInput.cs`. Optional confirmation property still exists for compatibility/validation if supplied; no confirmation field or join-farm choice/code is rendered. API likewise exposes optional confirmation without joinFarm/farmCode, and Android DTO/screen submit only name/email/password. Register calls shared registration with name/email/password, then redirects to login with instructions to create/request a farm after sign-in. Do not restore old documentation claiming those fields are required in the UI. Login validates local return URL, enabled account and lockout; login/registration use rate limiting. |
| Enrollment | After login, My farms lists accessible farms and join-request status; submit owner code, cancel own request. Owner sees code/applicants, regenerates code, approves/rejects, manages roles/removal. `FarmsController.cs:35-123`, `Views/Shared/_FarmEnrollment.cshtml`, `Views/Farms/Index.cshtml`. Retired invitation actions return Gone. Latest code rotation/cancellation extends October 27 design. |
| Farms/plots | Create, edit, archive and manage plots; rowversion-driven forms. `FarmsController`, `Views/Farms/*`. Owner manages membership/enrollment and whole-farm archive; managers can edit/manage plots. Layout is a schematic, not surveyed/GIS geometry. `PlotLayoutController`, `ViewModels/Farms/PlotLayoutPage.cs`. Selected crop links retain cycle context. |
| Dashboard | Authorized farm + report totals + plots/cycles/tasks + persisted weather + owner-only applicant panel. `DashboardController.cs:23`; owner panel/weather failure has bounded fallback. Charts/totals come from real services, not sample dashboard data. |
| Crops | Crop catalog additions, plan/edit Planned cycle, lifecycle transitions, monthly agenda, crop cards/history. `CropsController`, `Views/Crops/Index.cshtml`, `CropAgendaBuilder`. Global crop catalog; no catalog rename/deactivation surface. Monitoring/Harvest pages enter via cycle-specific links and mark Crops current in farm navigation. Timeline is `CropTimelineController`/`CropTimelineBuilder`. |
| Tasks | `TasksController` + `Views/Tasks/Index`: today (includes overdue), upcoming, completed/closed, all; farm-local date, no guessed local day for invalid zone. Owner/Manager creates/edits; Owner/Manager or assigned worker can close allowed task. Service `Services/Tasks/FarmTaskService.cs:55-129` authorizes, validates crop/assignee membership, handles reminders. Some forms redirect on errors rather than full draft recovery; do not claim all forms preserve drafts merely from crop/monitoring recovery. |
| Monitoring | Active crop activity/inspection/photo writes, readable history, quick entry, task linking, structured issue category/severity, Owner/Manager follow-up resolution/task linking. `MonitoringController.cs:52-136`, `Views/Monitoring/Index.cshtml:64-125,216-250`. API role lookup comment at controller line 70 is legacy wording: it calls shared farm services. Current MVC Activity action still records activity independently; do not claim Android combined activity+stock entry has a corresponding Web atomic form. |
| Photos | Authenticated private download, `nosniff` and `private, no-store` at `MonitoringController.cs:165-172`. Form/request limits 6MB accommodate multipart; domain max image is 5MB. Separate hosts require same durable private photo volume and shared Data Protection key ring. |
| Inventory | All/low/out filters and search, balances, item details/ledger, create/edit/archive, receipts/consumption/adjustment/reversal. `InventoryController`, `Views/Inventory/*`, `ViewModels/Inventory/InventoryPages.cs`. Field Worker consumes linked active-crop activity; management writes require Owner/Manager. Stock remains ledger-backed/nonnegative and rowversion-sensitive. |
| Harvest | Repeated partial harvest records; history and totals; Owner/Manager can void with reason, original record remains. Crop completion separate. `HarvestsController`/`Views/Harvests/Index.cshtml`. Active cycle required for new records. |
| Notifications/weather | `NotificationsController` handles own notification reads; Weather reads persisted forecast/advisories, freshness labels and stale notices, not browser direct provider polling. `WeatherController`/`Views/Weather/Index.cshtml`; dashboard shares saved weather data. |

`Core/Services/Security/FarmAccessRules.cs`: View = Owner/Manager/FieldWorker; Manage = Owner/Manager; ManageMembers = Owner. Owner is farm owner GUID; active membership controls other farm roles. Check operation-level services for narrower permissions. Removed/disabled users and archived farms lose access; nonmembers receive NotFound for farm data. There is no global admin UI. Finance viewing/export is available to active farm members, not managers only.

## Reports and finances (important new behavior)

- `Controllers/ReportsController.cs`: `Harvests`, `ExportCsv`, `ExportCycleCsv` share service authorization and date/plot/crop-type filters. Malformed model binding returns 400; invalid domain dates return 400, inaccessible farms return 404.
- `Infrastructure/Services/Reports/FarmReportService.cs:44` validates dates (2000-01-01 through 2100-12-30), resolves farm timezone for timestamp/date boundaries, selects only authorized farm data. `Core/Services/Reports` builds valid totals. Voided records remain in entry export/history but are excluded from yield totals. Units remain separate.
- `Views/Reports/Harvests.cshtml`: crop-cycle comparisons and cycle totals CSV, monthly and crop/plot charts, per-area estimate using **current recorded plot area** (historical caveat explicitly shown), latest 100 entries in HTML, all matching entries exported. `HarvestAnalyticsBuilder`: max 12 monthly bars/unit, 10 crop-cycle or crop/plot bars/unit; each unit has independent scale. Filtered partial totals must not be described as lifetime or normalized agronomic yield.
- `HarvestCsv.Cell` quotes separators/quotes and prefixes potentially formula-leading untrusted cells (including trim-leading `= + - @` and first-character tab/CR/LF). All three CSV builders reuse this. `HarvestCycleCsv` emits cycle GUID, crop/plot/unit, first/last matching date, valid count/quantity, filter bounds. Header-only output for no cycles is intentional. Numeric formatting uses invariant culture.
- `Controllers/FarmFinanceController.cs`: Index and `ExportInsightsCsv` are explicitly cookie-authenticated. Service checks FarmAction.View for reading/export and Manage for record/void. Current recorded cash ledger uses manual Income/Expense entries in PHP, optional same-farm crop-cycle allocation, positive 2-decimal money and reasoned void history.
- `Infrastructure/Services/Finance/FarmFinanceService.cs`: recent-entry display capped at 500; overview sums and date-filtered insights aggregate **all matching nonvoided rows**, not those 500. Filters are inclusive EntryDate. Monthly, category and crop-allocation summaries feed `FinanceInsightsCsv`. HTML shows recent 12 months/top 20 categories/allocations; export includes all matching aggregates.
- `Views/FarmFinance/Index.cshtml:34` explicitly states harvest quantity is not assumed sales, inventory movements do not automatically become expense, and net recorded cash is not complete profit/accounting. Preserve that distinction in new docs.

## Exact local merge/phase evidence

Local `git log --first-parent` and merge messages establish the following. IDs identify merged commits, not proof of a GitHub deployment, approval status or successful CI. PR66/67 numbers are **not present** in local merge messages; do not infer numbering from gaps.

| PR / merge | Evidenced phase | Content from contained commits |
|---|---|---|
| #61 `34710c7` (Nov10) | ui-overhaul-phase-1 | Green/brown Web tokens, earthy Android theme/typography, reusable Android farm UI, contrast checks. |
| #62 `22fed90` (Nov10) | ui-overhaul-phase-2 | Responsive Web workspace, landing, login/registration, farm list/join/create and map layout. |
| #63 `6bcb9d1` (Nov10) | ui-phase-2-smoke-copy fix | Restore smoke-test wording in redesigned Web pages. |
| #64 `9a345b7` (Nov11) | ui-overhaul-phase-3 | Web dashboard, schematic plots, members, crops/planning/timeline and management forms. |
| #65 `ed9a513` | ui-overhaul-phase-4 | Web task, monitoring/photo, inventory/ledger/edit and harvest operations UI. |
| `d5a28b7` | Unnumbered local merge | Android concurrent refresh/logout, cancellation, safe error messages and uncertain-response retries. |
| `f7541ef` | Unnumbered local merge | Weather stale detection/cache timestamp separation, farm access preservation, stale Web/Android notices, instrumentation compilation in CI. |
| #68 `8619bde` | ui-overhaul-phase-5 | Web reports/charts, weather/advisory, finance, notifications, account/access denied/settings. |
| #69 `fcdbe8a` | ui-overhaul-phase-6 | Android workspace bottom navigation, farm hero/plot artwork, farms/plots/members/settings, crops/tasks/notifications/account/auth/welcome. |
| #70 `a266d11` | ui-overhaul-phase-7 | Android offline notices, pending records, field inspections/photos, stock/harvest, sync/review. |
| #71 `a11f936` | ui-overhaul-phase-8 | Accessible Web entry/protected-page tests and role/responsive coverage; Android bottom nav/offline/queued-record/conflict tests and fixes. |
| #72 `3016c31` | phase-a-stability | Strict opt-in verification command, required SQL setting failure, SHA-256 private photo backup verification, nondestructive SQL backup checks, recovery docs, CI. |
| #73 `1f6440b` | phase-b-daily-work | Android task create/edit contracts, farm-local date/presets, Today center/manager forms, task-to-active-crop actions, manual field note guides. |
| #74 `c4e7f61` | phase-c1-crop-health | Structured issues/severity/follow-up model, additive SQL/migration/verification, backend authorization, Web and Android inspection/follow-up UI. |
| #75 `9d7a903` | phase-c2-activity-stock | Atomic combined activity and supply service/API/sync, Android queue/review/entry, rollback/idempotence tests. |
| #76 `a5e592d` | phase-d1-yield-comparisons | Per-cycle partial harvest comparisons, separated-unit charts/tests. Also contains initial finance aggregate rules/service read-only insights commits; do not attribute every finance change exclusively to D2. |
| #77 `a8e7c94` | phase-d2-finance-insights | Filtered API/Web finance insights, charts/breakdowns, safe authorized CSV, aggregation/security tests. |
| #78 `cfcabb9` | phase-d3-cycle-export | Filter-preserving cycle totals CSV, safe/empty report tests. |

All rows after #64 above are Nov11 according to local commit author dates. Later Harvest Gold + Pine work is a separate subsequent redesign, not more numbered `ui-overhaul-phase-*` PRs in local evidence:

1. `f96bf33` foundation: `b446745` Web navigation + `9adecfb` Android theme.
2. `0a2ed4f` dashboard: Web/Android home and dashboard test updates.
3. `dcfbc34` crop illustrations: local artwork, crops/plot screens and selected-crop link fix.
4. `1a12e8f` fieldwork: task filters, simplified inspection/photos.
5. `7a0716b` inventory/harvest UI and quantity/filter tests.
6. `69a2c28` weather/reports: Web weather/finance, Android insights/weather/detail/report-state-account safety.
7. `ac624be` frontend polish: landing/account/welcome/sign-in; `3b1a9d0` adds confirmation, then **`c901cea` simplifies registration**, which is the final behavior. Do not report confirmation merely because the preceding commit exists.

## Documentation accuracy inventory

All 17 README/docs Markdown files' relative link **file targets** were mechanically checked; no missing targets found. External URLs and heading anchors were not validated. Content/command routing is the main problem. Existing `docs/superpowers` designs/plans are historical artifacts; retain their verification dates/status and mark superseding behavior instead of treating checkboxes as fresh runtime evidence.

| Document | Current problem / retained useful scope |
|---|---|
| README.md | Stale overview says Android online-only, tasks/offline/automation/farm jobs/weather adapter/business tests/CI future. Five hosts/projects confused as four, layout omits Api and active test projects, Web incorrectly described as MVC+API. Weather provider OpenMeteo exists. Shared private-photo deployment section is useful. Git workflow remains authoritative instruction; local Git history does not independently verify remote branch protection. |
| docs/local-smoke-test.md | Wrong branch/PR14 pending story; wrong combined-host model and Web `/api/status` port 5080; emulator URL wrong (5081 current). Old welcome Check backend wording no longer describes whole app. Says no upgrades needed/23-table schema, contradicted by newer explicit upgrades. Keep home-without-DB and health connectivity caveat. Use Api status 5081 and independent Web 5080. |
| docs/android-initialization.md | Historical foundation: metadata-only Room, no workers/auth/business data are obsolete. Build entry points/SDK setup still useful subject to manifest pins. |
| docs/reference-alignment.md | Combined Web/API, no repositories, Room/offline deferred, no weather adapter/jobs/notifications, PR14 pending are obsolete. Conceptual comparisons are historical source review, not verified external repo state today. |
| docs/backend-database-connection.md | 23-table claim stale; Worker no persistence stale. Web Development health/connectivity and no-auto-schema behavior remain correct. Production test command now fails without required explicit private photo storage and may redirect HTTP due HTTPS; cannot simply expect 404 using old bare command. |
| docs/efcore-schema-alignment.md | Hardcoded 24-table/17-entity/key/check/default/index counts describe prior enrollment snapshot, not later finance/layout/issue schema. Old 2026-10-17/27 verification records should stay historical. Baseline adoption caution and explicit no startup migration remain useful; account for subsequent additive upgrades/migrations rather than advising just approval upgrade. |
| docs/authentication.md | Says signup UI requires confirmation and join-farm radio/code; latest UI has neither. Rate limiting is implemented, so future-abuse-control claim stale. API `Models/Auth/RegisterInput.cs` accepts optional confirmation only (validated when supplied); no joinFarm/farmCode properties. `AuthController.Register` calls plain name/email/password registration. Android `RegisterRequest` and `RegisterScreen` take only fullName/email/password. Joining is a separate authenticated enrollment flow on both clients. Memory-only token limitation should be verified in Android source before updating; not assumed fixed. No-CI sentence describes that phase only. |
| docs/farm-plot-management.md | Registration with farm code no longer current UI; missing request cancel/code rotation. Online-only global wording stale, though mutation-specific scope matters. Role/archive and rowversion guidance mostly retained. |
| docs/crop-management.md | Correct core lifecycle/global catalog/rowversion boundaries. Android should run against API, not just Web. Crop writes still online-only (do not claim offline plan/status writes from generic offline availability). Omits new agenda/timeline/illustrations. No-CI phrasing is historical phase scope. |
| docs/crop-monitoring.md | Task linkage/completion, structured issues/follow-up and selected offline queued writes now exist. Global append-only/no-edit wording needs follow-up exception; SQL schema changed in C1. Private storage fallback applies Development only; Production requires explicit absolute private shared path. Preserve photo access/type/size rules. |
| docs/inventory-management.md | Android queued inventory writes now exist, and combined activity-stock flow added; global online-only claim stale. Low-stock notification no-yet statement should be checked against current Worker. Ledger/rowversion/role rules retained. |
| docs/harvest-management.md | Offline harvest recording now exists. Tasks/weather/notifications/schedules/Room/WorkManager implemented subsequently; these are historic scope exclusions, not current absence. Partial harvest/void/completion rules still useful. |
| docs/background-worker.md | No jobs registered/future scheduling false: farm automation service and recurring schedules implemented. Separate jobs DB/schema/runtime configuration constraints still relevant; headless/no-dashboard remains useful. |
| docs/weather-integration.md | No provider selected/implement adapter false: `Integrations/Weather/OpenMeteoAdapter.cs` registered when `Weather:Provider=OpenMeteo`; exact allowed provider base configured in extensions. Persisted weather/advisories/freshness and Worker ingestion now exist. |
| docs/superpowers/plans/2026-10-26-ui-improvements.md | Historical five-group plan (before PR61). Preserve dated completed tasks and old counts as past verification. Its two baseline failures/JPEG fixture failure were later resolved (wiki PR48/progress evidence); not current failures by assertion. Design palette superseded by newest Pine/Gold work. |
| docs/superpowers/specs/2026-10-27-farmer-registration-approval-design.md | Historical chosen approval design; signup choices/code transaction no longer Web/Android onboarding; code rotation now implemented despite old out-of-scope statement. Approval/no-access-pending/owner-only decisions remain. |
| docs/superpowers/plans/2026-10-27-farmer-registration-approval.md | Historical implementation/evidence, not fresh tests; preserve counts and explicit unperformed browser/device checks. Signup fields and no-code-rotation scope superseded; old known baseline failures do not establish present failures. |

## Verified commands and deployment boundaries (source existence, not execution)

```sh
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
dotnet test SmartFarm.slnx --no-build
dotnet run --project src/SmartFarm.Web
dotnet run --project src/SmartFarm.Api
dotnet run --project src/SmartFarm.Worker
node --test tests/web-ui/site.test.cjs tests/web-ui/farm-location.test.cjs
python3 -m unittest discover -s database/verification -v
python3 -m unittest discover -s scripts/tests -v
bash scripts/verify-phase-a.sh --skip-android
# Opt in only with disposable initialized SQL and authorized existing emulator:
bash scripts/verify-phase-a.sh --sql --device
# From mobile/SmartFarm.Android:
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
bash ./gradlew :app:connectedDebugAndroidTest
```

- Launch profiles: Web http://localhost:5080, Api http://localhost:5081; `/api/status` belongs to Api. `/health/database` is Development only. Web checks connectivity only; API health is broader table readiness (see separate backend audit).
- `scripts/verify-phase-a.sh`: Release backend+schema+backup unit checks; explicit NOT VERIFIED messages for unrequested SQL/device checks. `--sql` sets required-SQL enforcement for BusinessWorkflowSqlTests, not a claim every SQL suite ran.
- `scripts/verify-phase6.sh`: strict running-host checks plus disposable SQL integration + backend/Android; requires SMARTFARM_API_URL, SMARTFARM_WEB_URL, SMARTFARM_TEST_CONNECTION; defaults enrollment test setting to same disposable connection. `RUN_ANDROID_DEVICE_TESTS=1` opts in device execution. Environment settings must refer only to initialized disposable allowlisted databases, never populated SmartFarmDB.
- `.github/workflows/verify-smartfarm.yml` now triggers PRs dev/main, pushes dev/manual: Release .NET build/tests; Android assemble, instrumentation **compile**, JVM tests. No real SQL integration or emulator execution in that workflow. `.github/workflows/phase-a-stability.yml` adds backup/script/schema checks. Presence proves CI configuration exists, not a current green remote run or CD deployment.
- `tests/SmartFarm.Auth.Tests/UiOverhaulPageTests.cs` checks rendered landmarks/auth form fields/antiforgery; `BusinessPageAccessTests` checks protected routes. `FormRecoveryTests`, `FarmEnrollmentWebTests`, `MvcIndependentHostTests`, `PhaseOneInsightsTests`, `PhaseThreeFeaturesTests`, CSV/rules/artwork tests provide targeted coverage. Node checks simulate DOM/source behaviors; neither set establishes interactive browser visual quality.
- No hosting platform/deploy command or automated CD is evidenced by audited source. Deployment requires explicit SQL-first database setup/upgrades, configured separate host connection strings, same absolute durable private photo mount on Web/API, environment-matching Data Protection application/key ring, production HTTPS, API release URL for Android; Worker enablement separately requires its jobs schema/database and app DB. Do not execute a fresh schema script against populated data, auto-migrate hosts, or treat local filesystem path equality as shared storage.
- Location search `FarmLocationsController` is authenticated server-side explicit Nominatim search (not autocomplete), per-process one request/s and one-day query cache; map picker has manual pin/address fallback. This is an external dependency with no live availability verification in this audit.

## Wiki impacts for coordinator

Affected wiki architecture/development/features/progress/known-issues likely need updates. October28 “CI placeholder”, “Android no report screen”, “signup optionally requests farm”, and dependency/current-feature omissions are superseded. Historical runtime claims should be retained with exact dates and explicit scope; no fresh runtime claims are supplied by this audit. Do not change immutable snapshots or copy this whole report as a replacement for selective evidence-backed section updates.


## `.github/workflows/phase-a-stability.yml`

````text
name: Phase A recovery and verification checks

on:
  pull_request:
    branches: [dev, main]
  push:
    branches: [dev]

permissions:
  contents: read

jobs:
  safe-recovery-checks:
    name: Read-only backup checks
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: "3.12"
      - name: Check safety scripts syntax
        run: |
          bash -n scripts/verify-phase-a.sh
          bash -n scripts/verify_sql_backup.sh
          python3 -m py_compile scripts/verify_photo_backup.py
      - name: Test photo-backup failure and success cases
        run: python3 -m unittest discover -s scripts/tests -v
      - name: Recheck SQL schema verifier
        run: python3 -m unittest discover -s database/verification -v

````
