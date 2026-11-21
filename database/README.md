# SmartFarm database setup

SmartFarm uses Microsoft SQL Server and SQL-first schema. Developers keep separate
local records/credentials while sharing reviewed scripts. Reviewed against `dev`
at `ac624be` on 2026-11-11; no live database was inspected or changed by this update.

Choose [macOS Docker/DBeaver](README_macOS.md) or
[Windows SQL Server/SSMS/DBeaver](README_Windows.md).

## Files and separate purposes

| File | Purpose |
| --- | --- |
| `01_CreateDatabase.sql` | Create SmartFarmDB if absent; run connected to master |
| `02_CreateSchema.sql` | Nonrepeatable fresh base: seven Identity + 18 business tables, including current issue fields |
| `03_VerifySchema.sql` | Read-only structural checks; permits optional finance |
| `04_FarmerRegistrationApproval.sql` | Existing-schema join-code/request backfill; rerunnable, no EF history |
| `05_FarmEnrollmentWorkflow.sql` | Existing enrollment cancellation/review/notification constraints |
| `06_FarmFinance.sql` | Optional separate finance ledger; needed for finance features |
| `05_CreateJobsDatabase.sql` | Separate Hangfire database only; not an application upgrade |
| `06_VerifyHangfireJobs.sql` | Read-only jobs/heartbeat evidence in jobs database |
| `compose.yaml`, `.env.example` | Local SQL Server 2022 Developer configuration |
| `verification/` | Offline CREATE/catalog comparison tools; no database connection |
| `RECOVERY.md` | Backup checks and mandatory disposable restore acceptance |

## Fresh database

Review target/credentials, run 01 against master, then 02 against the new empty
SmartFarmDB. Run 06_FarmFinance when using the current finance module, then inspect the applicable verification scope below.
Base has 25 tables; finance adds one, support adds two and announcements add one,
for 29 full-model tables. Apply reviewed 08_SupportManagement and
09_PlatformAnnouncements extensions when using those modules. Base already
contains the AdminAuditLog introduced by 07_AdminFoundation. The current
base already has enrollment/workflow/issue structures; do not blindly replay all
migrations/upgrades on it. Creation refuses wrong database or existing user tables.
Never use fresh-schema DDL as an existing-data repair.

SQL scripts do not initialize `__EFMigrationsHistory`. Application startup and
health checks do not apply schema changes. Before EF adoption compare source,
model and complete live metadata/history; see
[EF alignment](../docs/efcore-schema-alignment.md).

## Current verification boundary

Publication source check: dev `3c5a8b0`, 2026-11-12. `03_VerifySchema.sql` currently
checks base plus optional finance counts; it does not accept the complete support/
announcement schema. Do not treat it as full extended-model acceptance or remove
valid tables to make it pass. Compare complete live metadata with current mappings
and all required extension contracts. See [current source status](../.local/karpathy-wiki/wiki/project/current-source-status.md)
and [admin setup](ADMIN_SETUP.md). No database/verifier code was changed here.

## Existing database rollout

Inventory actual schema/history and back up before selecting missing changes.
Older pre-enrollment databases use reviewed 04 then 05; finance uses its separate
06. Structured issues use the additive `CropIssueFollowUps` EF migration when
appropriate; no standalone issue upgrade SQL is currently checked in. Current
fresh DDL also contains metadata/weather additions without a dedicated migration
for every historical schema state. Prepare a reviewed explicit rollout for the
actual target instead of claiming all older databases are covered.

The issue migration must not be blindly applied to a fresh schema that already
has its columns. SQL-only setup and adopted EF history are different paths;
coordinate them explicitly. Changes require separately authorized execution by
the database owner; no schema/history action is authorized by reading this guide.

## Jobs storage and runtime

Checked-in Worker processing is enabled. It needs application connection plus a
separate preprovisioned Hangfire catalog/schema. 05_CreateJobsDatabase creates only
the database; reviewed matching schema installation or explicit first-start
`Hangfire__InitializeSchema=true` is a separate operation, then return it to false.
Use [Worker setup](../docs/background-worker.md); do not point jobs storage at
SmartFarmDB. An idle host requires explicit `Hangfire__Enabled=false`.

Never commit `.env`, connection strings, records, backups, keys, volumes or photos.
Use independent local stores and protected production credentials/TLS. Matching
schema files are evidence, not permission to mutate a live database.

## Upgrade an existing database for farmer approval

Back up the intended database and review `04_FarmerRegistrationApproval.sql`.
The database owner applies it only as one part of the target-specific reviewed
rollout. Script 04 alone does not add all structures required by the current
verifier; complete the missing workflow, issue, metadata/weather and finance scope
as appropriate before running 03. Do not reuse the former 04→03 example as a
complete upgrade path for an older populated database. See Existing database
rollout above and the EF alignment guide.

Never rerun `02_CreateSchema.sql` against existing data. The upgrade preserves
accounts, ownership and membership, fills only missing codes with cryptographic
random values, adds request history and its constraints, and retains codes on
rerun. It locks the farm table during the backfill, so schedule it before deploying
the updated clients. No upgrade runs at application startup.

The upgrade writes no EF history. If EF migration history has already been
adopted, the corresponding `FarmerRegistrationApproval` migration executes the
same rerunnable SQL; generate and review the migration script before applying
it. An unadopted SQL-first database can continue using the explicit SQL script.
Downgrade is deliberately blocked to preserve shared codes and decision history.

## Later upgrade context

The approval section above describes the first upgrade only; current workflow
adds cancellation, code rotation and in-app enrollment notices through 05 and
services. Finance and issue rollout are separate as described above. Preserve
stable codes/history and inspect the target's EF adoption status before any action.
