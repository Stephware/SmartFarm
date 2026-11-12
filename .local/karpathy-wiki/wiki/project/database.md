# Database and schema workflow

> Sources: SmartFarm SQL, EF model, migration and schema-test sources
> Raw: [Database sources](../../raw/project/2026-10-27-database-source.md); [Feature sources](../../raw/project/2026-10-27-features-source.md); [backend source review](../../raw/project/2026-11-11-backend-current.md); [reports source review](../../raw/project/2026-11-11-reports-current.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

## Implemented contract

SQL Server under `dbo` is authoritative. EF mappings explicitly follow SQL-first
DDL and disable the conventional extra foreign-key indexes. The current model
expects 29 tables, 256 columns, 53 foreign keys, 60 secondary indexes and 48 checks,
including finance, admin audit, support and announcements; eight rowversions are
concurrency tokens. These are source expectations, not live measurements. Base
CREATE has 25 tables; optional/module upgrades explain the difference. The existing
03 verifier counts only base/finance and is not complete extended-model acceptance.


Farm → Plot → CropCycle connects activities, observations/photos and harvests.
Farm also scopes members, join requests, tasks, inventory, finance and weather.
Notifications reference users; SyncOperation stores stable operation IDs, payload
hashes and saved results. Observation issue/follow-up fields and rowversion extend
monitoring. Inventory links activities and reversal history.

Business GUIDs are assigned by clients, farm dates use `DateOnly`/SQL `date`, and
UTC event timestamps use `datetime2(3)`. Updates carry Base64 rowversion where
specified. Active membership/plot names/crops, pending requests and deduplication
keys use filtered unique indexes. No-action deletes preserve history. Identity
retains its older explicit schema version and seven tables. Do not confuse
membership role codes with `FarmAccessLevel` values.

## Initialization and upgrades

- New database: review [database setup](../../../../database/README.md), create the
  database, run base schema, then the separate finance script when finance is
  required, and verify. Creation is not repeatable; never rerun it on business data.
- Existing pre-enrollment schema: review/back up before applying
  `04_FarmerRegistrationApproval.sql` and `05_FarmEnrollmentWorkflow.sql`.
  The workflow upgrade extends request states and notification constraints.
- Finance is `06_FarmFinance.sql`. `05_CreateJobsDatabase.sql` and
  `06_VerifyHangfireJobs.sql` concern separate jobs storage, not application
  upgrade ordering. Number prefixes alone are not a safe migration runbook.
- Structured issue fields are included in current fresh DDL. Existing databases
  need the reviewed additive `CropIssueFollowUps` EF migration; there is no
  standalone crop-issue SQL upgrade file. Inspect current schema/history first.
- Startup never calls Migrate/EnsureCreated or adopts migration history.
  `ExistingSchemaBaseline` has empty Up/Down. Enrollment and workflow migrations
  embed reviewed SQL; destructive downgrades are blocked. The issue migration is
  additive. Finance has no corresponding EF migration in the current inventory.
- SQL scripts do not write EF history. Baseline/history adoption is a separate
  explicitly approved database operation after source/model/live comparison.

The read-only Python verifier parses the base CREATE contract; it rejects ALTER
upgrade scripts and is not a general SQL engine. Full EF model versus base-only
SQL has an expected finance mismatch until a reviewed complete comparison contract
is prepared. Do not suppress that mismatch or claim equality from unit tests.

Compose uses an amd64 Developer SQL Server image, loopback binding and named
volume. Apple Silicon emulation is vendor-unsupported; follow the platform guide.
Recovery requires an actual disposable restore plus private-photo checks, not
only `RESTORE VERIFYONLY`.

## Source navigation

[Setup](../../../../database/README.md), [base DDL](../../../../database/02_CreateSchema.sql),
[workflow upgrade](../../../../database/05_FarmEnrollmentWorkflow.sql),
[finance](../../../../database/06_FarmFinance.sql),
[EF migrations](../../../../src/SmartFarm.Infrastructure/Persistence/Migrations),
[DbContext](../../../../src/SmartFarm.Infrastructure/Persistence/SmartFarmDbContext.cs),
[model tests](../../../../tests/SmartFarm.SchemaVerification/SchemaModelTests.cs),
[verifier](../../../../database/verification/README.md),
[recovery](../../../../database/RECOVERY.md).

## See Also

[Architecture](architecture.md), [Decisions](decisions.md), [Development](development.md),
[Finance and reports](finance-reports.md), [Known issues](known-issues.md).
