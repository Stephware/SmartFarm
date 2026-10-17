# EF Core schema alignment and migration adoption

`database/02_CreateSchema.sql` remains the SQL-first contract for the existing
`SmartFarmDB`. Infrastructure maps all 23 `dbo` tables: 16 plain Core entities and
seven ASP.NET Core Identity tables using GUID user/role IDs. Identity types stay
in Infrastructure, and Core entities reference users through scalar GUIDs. This
change registers no authentication workflows or automatic database operations.

Each table has a separate Fluent API configuration. Mappings explicitly retain
column types/nullability, 23 primary keys, 39 foreign keys and their delete
behavior, 36 checks, 28 named defaults, and 45 secondary indexes (including
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
baseline plus snapshot drift. The Python verifier compares every supported
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
