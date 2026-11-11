# SmartFarm backup and recovery acceptance

Run this procedure against **a disposable environment**, never by restoring over SmartFarmDB.
A passing build, `RESTORE VERIFYONLY`, or a folder copy is **not** evidence of end-to-end recovery.

## What must be protected

1. SQL Server `SmartFarmDB`: farms, Identity/accounts, crop cycles, tasks, activities, observations, inventory ledger, harvests, finance, notifications, and idempotent sync keys.
2. Private observation photos: the actual directory configured as `ObservationPhotos__StoragePath` on Web and API. SQL stores photo keys, not the image bytes.
3. The deployment's configuration and key material, particularly the shared ASP.NET Core Data Protection key ring. **Do not put secrets or keys in Git**. Store encrypted backups with restricted access.
4. Pending Android Room writes and photo uploads remain on **each device** until server confirmation. Server backups cannot restore unuploaded mobile records.

## Verify a backup without replacing anything

1. During a controlled maintenance/quiescent window, take a **SQL Server native database backup with CHECKSUM** to a secure server-side backup volume. Example SQL for an operator with backup permission:

   ```sql
   BACKUP DATABASE [SmartFarmDB]
     TO DISK = N'/var/opt/mssql/backups/SmartFarmDB-YYYYMMDD.bak'
     WITH COPY_ONLY, COMPRESSION, CHECKSUM, INIT, STATS = 10;
   ```

   Change the file name each time, secure the backup destination, and transfer an encrypted copy off the database host. The path is inside the SQL Server machine/container, **not** a path on your Mac.

2. Snapshot the private observation-photo directory into a **fresh, separate, access-controlled** directory at the same recovery point. Preserve filenames, subdirectories, and contents. Do not expose it under `wwwroot`. Do not delete or move the source.
3. Verify the native backup and copied photos:

   ```bash
   export SMARTFARM_SQL_SERVER=localhost,14333
   # For SQL authentication use SQLCMDPASSWORD in the environment and SMARTFARM_SQL_USER.
   bash scripts/verify_sql_backup.sh /var/opt/mssql/backups/SmartFarmDB-YYYYMMDD.bak
   python3 scripts/verify_photo_backup.py /private/live/observation-photos /separate/backup/observation-photos
   ```

   The SQL check calls `RESTORE VERIFYONLY WITH CHECKSUM`; the photo check compares relative file paths and SHA-256 content. Both are read-only. An empty photo set is not accepted as proof of photo recovery. Store verification logs without connection strings, secrets, or photo contents.

## Mandatory disposable restore rehearsal

1. On a **separate test SQL instance**, inspect the backup using `RESTORE FILELISTONLY`. Restore into a uniquely named `SmartFarmRecoveryTest_...` database with explicit `MOVE` paths. **Never** use `REPLACE`, `SmartFarmDB`, or a production data directory.
2. Run `DBCC CHECKDB` and the read-only `database/03_VerifySchema.sql` checks against the recovered database after reviewing any expected optional upgrades.
3. Start a test Web/API instance with a **separate** private photo directory populated from the snapshot, matching test Data Protection configuration, and no live external jobs enabled.
4. Verify an existing test owner can access their farm, other accounts are denied, at least one private observation image loads, inventory and harvest totals agree, and existing sync operation identifiers remain intact. Use test data only; do not expose real farmer data in shared test environments.
5. Record the actual recovery time, missing records/files, date of snapshot, and operator outcome. A failure blocks release. Decide the required backup schedule and acceptable recovery point/time with the farm owner.

## Repeatable regression command

```bash
bash scripts/verify-phase-a.sh                    # .NET, Python, Android build/unit/compile
bash scripts/verify-phase-a.sh --sql              # adds real SQL farm-to-harvest test; disposable DB only
bash scripts/verify-phase-a.sh --device           # adds connected Android UI/instrumentation tests
bash scripts/verify-phase-a.sh --sql --device     # full automated acceptance gate
```

The SQL-backed API test needs `SMARTFARM_TEST_CONNECTION` to an **initialized, disposable** `SmartFarmEnrollmentTest_...` or `SmartFarmEnrollmentBackend_...` database. The test refuses the production database. Device tests require an authorized emulator/phone and Android SDK.

Before the final `dev` → `main` promotion also manually test Owner, Manager and Field Worker Web flows, new and archived plots, crop transitions, weather Worker deduplication, phone/tablet large fonts/dark mode, camera/gallery uploads, loss of connectivity during sync, stock conflict review and retry, and restoration from backup. Save acceptance results outside source control when they include user or operational data.
