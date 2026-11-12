# EF Core schema alignment and migration adoption

Reviewed against `dev` at `ac624be` on 2026-11-11. Source/model expectations are not
live metadata measurements. The historical verification records below retain their
original revision/scope and do not establish current database compatibility.

## Publication source update

At dev `3c5a8b0` (2026-11-12), base CREATE has 25 tables, 214 columns,
25 primary keys, 44 foreign keys, 39 checks and 51 indexes. Full EF source includes
finance/admin/support/announcements: 29 tables, 256 columns, 53 foreign keys,
48 checks, 60 indexes and eight rowversions. Earlier counts below are historical.
The existing 03 SQL verifier counts base/finance only and is not full extended-
model acceptance. Compare equivalent complete contracts; do not concatenate ALTER
upgrades into this CREATE-only Python parser or modify valid data to satisfy an
outdated count. See the central database runbook for the verification boundary.

> **Status: Outdated** (2026-11-12)
> Baseline counts/verification below retain their original revision and scope.

## Current contract and schema split

SQL-first `database/02_CreateSchema.sql` defines 24 base tables (seven Identity and
17 business), 206 columns, 43 foreign keys, 49 secondary indexes and 39 checks.
Optional `06_FarmFinance.sql` adds one table, 13 columns, four foreign keys, three
indexes and three checks. Full EF model expectations are therefore 25 tables,
219 columns, 47 foreign keys, 52 indexes and 42 checks, with six SQL rowversions.
`SchemaModelTests.cs` and `03_VerifySchema.sql` are the source for these expectations.
The verifier supports optional finance; base-only SQL does not equal the full EF
CREATE output. Do not suppress a real mismatch to report success.

Explicit Fluent configurations retain SQL names/types/nullability/defaults,
filtered indexes and no-action history relationships. GUIDs are client-assigned;
farm dates are DateOnly/date and UTC events datetime2(3). Identity schema Version1
is explicit. Core stores user GUID references; Identity types stay Infrastructure.
Conventional FK indexes are disabled to match DDL. Inventory MetadataVersion changes
on metadata edit/archive but is not a replacement for SQL rowversion concurrency.

## Read-only verification

```sh
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
dotnet test tests/SmartFarm.SchemaVerification/SmartFarm.SchemaVerification.csproj --no-build
python3 -m unittest discover -s database/verification -v
dotnet ef dbcontext script --project src/SmartFarm.Infrastructure --output /tmp/smartfarm-model.sql
dotnet ef migrations has-pending-model-changes --project src/SmartFarm.Infrastructure
```

The design-time factory needs no live connection. Generated SQL is comparison
input, never a command to execute over existing tables. For `verify_schema.py`,
compare matching scopes: base SQL to base catalog, or a reviewed complete CREATE
reference to full model/catalog. The parser rejects ALTER upgrades; do not pass
concatenated upgrade scripts and assume they are a supported contract. See
[verifier limitations](../database/verification/README.md).

## Explicit adoption and rollout

`ExistingSchemaBaseline` has empty Up/Down and is only an adoption marker.
Enrollment and workflow migrations execute reviewed rerunnable SQL; issue-follow-up
migration is additive and downgrade refuses history loss. Finance has a separate
SQL script without a dedicated EF migration. Fresh DDL already includes issue
fields, so blindly applying that migration again is unsafe. Other current base
changes do not all have dedicated forward migrations; inspect actual schema first.

Before any database-changing action:

1. Identify target, back up/rehearse recovery, read live catalog and EF history.
   Compare complete intended scope including finance/issue fields, trusted checks,
   indexes and unexpected objects. Stop for unexplained differences.
2. Confirm snapshot/model state and inspect relevant migration SQL. Generate the
   baseline-only script if history adoption is actually needed:
   `dotnet ef migrations script 0 ExistingSchemaBaseline --project src/SmartFarm.Infrastructure --output /tmp/smartfarm-baseline-adoption.sql`.
3. Obtain the separately scoped approval required for applying reviewed database
   changes/history. SQL scripts do not populate EF history; never insert a history
   row casually or run InitialCreate over existing tables.
4. Have the database owner apply only the reviewed operations and re-verify.
   Startup/health never migrates, creates/deletes schema or adopts history.

See [database setup](../database/README.md) for each script's purpose rather than
sorting numeric prefixes; jobs scripts target separate storage. This documentation
synchronization applied no schema or history operation.

## Historical verification interpretation

The records below preserve prior results and decisions. Counts, table scope,
request states and equality statements refer to their stated October snapshots.
Cancellation/workflow, issue fields, metadata/weather and optional finance changes
supersede them for current rollout. Current source/runtime status is in the
[wiki database page](../.local/karpathy-wiki/wiki/project/database.md).

## Phase 2 verification record

On 2026-10-17, read-only inspection of the configured development `SmartFarmDB`
matched the checked-in SQL and EF-generated DDL: 23 tables, 193 columns, 23
primary keys, 39 foreign keys, 36 checks, 28 named defaults, and 45 secondary
indexes. All constraints were trusted/enabled; no triggers or migration history
table were present. Each of the 23 entity projections executed a SQL `SELECT`
with a zero-row limit successfully. A second full catalog capture matched the
first exactly after verification.

The Web host returned 200 for MVC and the configured development database health
check, 503 for missing/malformed/unreachable database settings, and 404 for the
health endpoint in Production. Four offline xUnit tests and 13 Python verifier
regression tests passed. The repository catalog export was executed successfully
and matched the independent catalog capture. No schema,
application data, or migration history was changed. Credentials stayed in local
user secrets and were excluded from artifacts/source.

## Farmer registration approval upgrade (2026-10-27)

`database/04_FarmerRegistrationApproval.sql` is the explicit upgrade for existing
SQL-first databases. It backfills missing `Farm.JoinCode` values using
`CRYPT_GEN_RANDOM(6)` and uppercase hex, retains existing codes on rerun, and adds
`FarmJoinRequest`. A unique code index and a unique farm/user index filtered to
Pending prevent collisions and duplicate live requests. State/reviewer checks
require Pending requests to have no decision metadata and Approved/Rejected
requests to have both reviewer and timestamp. All three foreign keys use
NO ACTION. Existing users, memberships, ownership and historic decisions survive.

Follow [current database upgrade instructions](../database/README.md#upgrade-an-existing-database-for-farmer-approval).
The transaction locks `Farm` during backfill; arrange a deployment window.
`20261027022450_FarmerRegistrationApproval` executes the same rerunnable SQL when
using an already adopted EF migration history. Its Down operation deliberately
refuses to remove codes or history. The SQL-first script does not insert history;
no application startup path applies it. A fresh database's current create script
already contains the upgraded contract.

Offline verification now expects 24 tables, 201 columns, 24 primary keys,
42 foreign keys, 38 check constraints, 30 named defaults and 48 secondary indexes.
The historical baseline verification record above describes the earlier schema.

Verification on 2026-10-27: 14 offline schema xUnit tests and 15 Python verifier
regressions passed; EF-generated CREATE matched the complete fresh SQL contract,
and the snapshot had no pending changes. A uniquely named disposable SQL Server
database was initialized with the previous schema and seeded with two users,
two farms and an active Manager membership. Running the upgrade twice retained
those records and produced unique, valid, stable codes. SQL rejected duplicate
Pending requests, invalid states and missing/inconsistent reviewer metadata;
rejection retained history and permitted a new Pending request. The read-only
verifier passed, and the upgraded live catalog matched the fresh contract in
full. Actual fresh-schema creation followed by the upgrade and verifier also
passed. All schema-test databases were removed; no application database or its
migration history was changed. The enrollment upgrade and EF migration were
also checked for exact SQL equality, and downgrade refusal was verified.
The generated EF baseline-adoption/enrollment script was executed only in a
disposable database and passed structural verification with both history rows.
