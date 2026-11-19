# Backend integration contract (B0–B5)

Implementation baseline: fetched `origin/dev` at `6fbd0a07`, 2026-11-19. Backend branch: `feature/anigo-backend-b0-b5`. This contract supplements existing Core DTOs; old numeric enum values, rowversions and sync envelopes remain authoritative.

## B0 baseline and demo operations

Existing support: shared farm/plot/crop/member/enrollment services, atomic stock operations, partial harvests, reports, monitoring/private photos, opaque Identity bearer/refresh, cookie MVC, idempotent sync ledger, saved Open-Meteo forecasts, deterministic advisories, in-app notifications and separate Hangfire jobs storage. No email/reset/Gemini integration exists in the baseline. Ordinary business lists are generally unpaged. Farm time zone already defaults to `Asia/Manila`.

Invitation endpoints intentionally return 410; use farm code plus owner approval. The demo includes a pending enrollment request instead of resurrecting invitation links. Finance/support/announcement records and schema history stay intact; retired UI is not restored.

Run from repo root with `DOTNET_ENVIRONMENT=Development` and `ConnectionStrings__SmartFarm` or existing Web user secrets:

```sh
dotnet run --project tools/SmartFarm.DemoSeeder
# After successful preflight, explicitly apply (new private credentials path):
dotnet run --project tools/SmartFarm.DemoSeeder -- --apply --credentials /private/tmp/anigo-demo-credentials.json
# Optional real/local representative inspection photo, stored privately:
# add --photo-source /absolute/photo.jpg --photo-root /absolute/shared/private-photo-root
```

The tool only accepts loopback `SmartFarmDB` or disposable `SmartFarmEnrollmentBackend_` catalogs in Development. It compares the complete EF CREATE contract to live catalog metadata using the existing read-only verifier; drift blocks application. It never creates/changes schema or migration history. Apply requires SQL `BACKUP DATABASE ... COPY_ONLY, CHECKSUM` and `RESTORE VERIFYONLY ... CHECKSUM` success, with server files under `/var/opt/mssql/backup`. This is backup validation, not a restore drill. Missing permissions/directory or schema mismatch leaves the dataset unapplied.

Demo IDs are deterministic (`DemoDataset.Id`); names use `AniGo demo v1`, emails use `anigo-demo-*@example.test`, dedupe keys use `anigo-demo-v1:`, forecast provider is `SyntheticDemo`. Each account gets its own cryptographically random password in an exclusively created private file; no shared checked-in credential. A serializable transaction creates the dataset once; repeat apply never overwrites rows. There are three farms, owner/manager/worker/applicant users, crops in planned/active/completed states, due/overdue/upcoming/completed work, inspection/activity, balanced low stock, multiple partial harvests in kg and bunches, active/resolved synthetic advisories and read/unread notifications. Photos require an explicit local source and shared private storage; none is fabricated. Demo IDs and their foreign-key graph can be selected for a separately reviewed cleanup, never deleted automatically. Existing application/demo edits are retained on repeat runs.

## Existing API inventory

All business routes require opaque bearer authentication and current farm access. MVC calls shared services directly.

| Area | Existing contract |
| --- | --- |
| Authentication | `/api/auth/register`, `/login`, `/refresh`, `/me` |
| Setup and roles | `/api/farms`, farm plots/members/access, crop-types, crop-cycles and transitions |
| Enrollment | `/api/farm-join-requests`, farm join-requests/decision, cancel and join-code/regenerate |
| Records | cycle monitoring/activities/observations/follow-up/private photos, cycle harvests/void |
| Supplies | farm inventory, item transactions/reverse, eligible activities |
| Daily work | farm tasks/update/close, dashboard, weather, notifications/read |
| Reports | farm reports/harvests; existing finance contracts retained |
| Offline | `/api/sync/operations/{operationId}`, existing type-specific payloads unchanged; stable photo PUT |

Schema/migration baseline: SQL-first scripts plus explicit EF baseline/enrollment/issue/admin/support/announcement migrations. Finance remains a separate SQL module. No startup migrations. Live alignment and seeding status are recorded in the execution plan after preflight.
