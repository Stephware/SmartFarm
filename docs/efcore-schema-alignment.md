# EF Core schema alignment and migration adoption

`database/02_CreateSchema.sql` remains the SQL-first contract for the existing
`SmartFarmDB`. Infrastructure maps all 24 `dbo` tables: 17 plain Core entities and
seven ASP.NET Core Identity tables using GUID user/role IDs. Identity types stay
in Infrastructure, and Core entities reference users through scalar GUIDs. This
change registers no authentication workflows or automatic database operations.

Each table has a separate Fluent API configuration. Mappings explicitly retain
column types/nullability, 24 primary keys, 42 foreign keys and their delete
behavior, 38 checks, 30 named defaults, and 48 secondary indexes (including
filters). The FK index convention is disabled because the SQL contract defines
which indexes exist. Identity schema version 1 is explicit: adding passkeys or
other Identity schema changes requires a separate reviewed migration.

GUID primary keys are assigned by clients, with new domain/user/role instances
receiving a GUID locally. Calendar columns use `DateOnly`/`date`; timestamps use
`datetime2(3)` and keep the SQL UTC convention. The five `rowversion` properties
are database-generated optimistic concurrency tokens. Identity's standard
user/role concurrency stamps retain their framework semantics.
`InventoryItem.MetadataVersion` retains only its existing `NEWID()` insert
default; the schema has no updater/trigger or established application contract
for advancing it, so this foundation does not invent a second concurrency
protocol. `CropType.IsActive` defaults to true locally and in SQL, with EF's true
sentinel ensuring an explicit false is sent during an insert. A new Farm also
uses the existing `Asia/Manila` default.

## Repeatable verification without database access

From the repository root:

```bash
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
dotnet test SmartFarm.slnx --no-restore
dotnet ef dbcontext script --project src/SmartFarm.Infrastructure --output /tmp/smartfarm-model.sql
python3 database/verification/verify_schema.py --source database/02_CreateSchema.sql --generated /tmp/smartfarm-model.sql
python3 -m unittest discover -s database/verification -v
dotnet ef migrations has-pending-model-changes --project src/SmartFarm.Infrastructure
```

Use the .NET 10 SDK and EF CLI 10.0.13 to match the provider/design packages.
The design-time factory deliberately builds the model without a connection
string. The generated create script is for comparison only; **do not execute it
against an existing database**. The xUnit tests cover the model contract,
concurrency and client GUID behavior, the true/false sentinel, and the empty
baseline, farm enrollment invariants and snapshot drift. The Python verifier compares every supported
schema object and predicate with the checked-in SQL, failing on differences or
unsupported DDL. Its README also documents supplying a read-only live catalog
export for a three-way comparison.

## Verified baseline and approval boundary

`ExistingSchemaBaseline` has empty `Up` and `Down` methods. Its generated designer
and model snapshot describe the full existing schema for future migration
diffs. It is an adoption marker, not an empty-database provisioning migration.
Removing it must never drop manually created tables.

Before adopting this baseline in any environment:

1. Confirm the intended database and obtain a fresh read-only catalog export.
   Compare the source SQL, current EF-generated SQL, and that live catalog. Stop
   for any mismatch, unexpected table/history entry, disabled/untrusted
   constraint, trigger, or unavailable permission. The development database
   was independently verified before this baseline was prepared; this does not
   establish compatibility for another database or a later changed schema.
2. Verify the snapshot has no pending model changes, inspect the baseline
   `Up`/`Down`, and confirm no existing `__EFMigrationsHistory` table or entries
   conflict with the intended adoption.
3. Generate and review the **baseline-only** history adoption script offline:

   ```bash
   dotnet ef migrations script 0 ExistingSchemaBaseline --project src/SmartFarm.Infrastructure --output /tmp/smartfarm-baseline-adoption.sql
   ```

   This script is expected to create the EF history table and record the
   baseline. Even though the baseline contains no schema operations, applying
   this script is a database-changing operation and requires explicit approval.
   Do not insert a history row manually or run a normal `InitialCreate`.
4. After approval, have the database owner apply only that reviewed script and
   verify its history entry and an unchanged application schema. This step has
   **not** been performed by this implementation.

Future migrations must be reviewed against the adopted snapshot and the live
schema, with explicit approval before applying database changes. Startup and
the development health endpoint must never apply migrations, create/delete
schema, or modify migration history. A fresh empty database still requires the
existing manual setup scripts before baseline adoption.

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

Follow [database upgrade instructions](../database/README.md#upgrade-an-existing-database-for-farmer-approval).
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
