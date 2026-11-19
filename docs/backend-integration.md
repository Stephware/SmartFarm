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

## B1 time, language, errors and pages

Farm setup defaults to `Asia/Manila`; preserve each configured zone. SQL instant columns remain UTC `datetime2`, farm dates remain ISO `yyyy-MM-dd`. API instant fields now serialize with `Z` even when SQL materializes an Unspecified DateTime; offset-bearing inputs normalize to UTC. Date-only values never acquire an offset. Invalid legacy weather zones fall back to Manila for reads; new farm validation still rejects invalid zones.

`GET/PUT /api/auth/preferences` returns/accepts `{ "language": "en" | "fil" }`, persisted in existing Identity user claims (`anigo:language`). Default is English. `BackendMessages` defines stable message IDs and English/Filipino deterministic guidance. Existing stored messages remain historical text; clients can render supplied keys and structured facts. Numeric enum values do not change. Domain failures retain `message` and add `code`: `resource.not_found`, `record.conflict`, `record.invalidinput`. Invalid model binding returns `validation.invalid` plus keyed validation errors; clients should use codes/field names rather than parse English descriptions.

`GET /api/farms/{farmId}/records/{resource}` accepts `tasks`, `activities`, `observations`, `harvests`, `inventory`, `movements`, `members`, `enrollment`. Members/enrollment require Owner, other records require current farm membership. `GET /api/notifications/page` pages the authenticated user's notifications.

Common parameters: `page` (1–100000, default 1), `pageSize` (1–100, default 25), `search` (literal substring, max 120), `sort=asc|desc` (default desc). Response: `{ items, page, pageSize, totalCount, hasMore }`. Every order uses ID as final tie-breaker. Paging is offset based, represents current records at query time, and can shift when data changes; refresh from page one after writes. These are display endpoints, never synchronization cursors. Counts and rows use identical filters. Existing unpaged lists, report aggregates, photo contracts and sync/replay endpoints are unchanged.

| Resource | Additional filters | Item shape/order |
| --- | --- | --- |
| tasks | status, type, cropCycleId, assignedToUserId, from/to | existing FarmTaskDetails; due date |
| activities | type, cropCycleId, from/to | `{cropCycleId, record: ActivityDetails}`; occurred instant |
| observations | type=health, status=follow-up, cropCycleId, from/to | `{cropCycleId, record: ObservationDetails}` with private photo metadata; observed instant |
| harvests | cropCycleId, voided, from/to | existing HarvestEntry; harvest instant; report totals remain separate |
| inventory | type=category, lowStock | existing InventoryItemDetails; item name |
| movements | itemId, type, from/to | existing InventoryTransactionDetails; occurred instant |
| members | type=role, status=0 active/1 removed | id/userId/role/joinedAtUtc/removedAtUtc/fullName/email; joined instant |
| enrollment | enrollmentStatus=Pending/Approved/Rejected/Canceled, from/to | id/userId/status/requestedAtUtc/reviewedAtUtc/fullName/email; requested instant |
| notifications | type, unread | existing NotificationDetails; created instant |

Record `from/to` are inclusive farm-local dates; task dates are compared directly, instant histories use local-midnight UTC bounds. Invalid bounds/sort/size return 400. No new schema migration is required for B1.

## B2 saved weather and Ani alerts

`GET /api/farms/{farmId}/weather/records?category=recent|active|upcoming|resolved|history` uses common paging/date/type/search/sort filters. `recent` returns saved forecast snapshots; `upcoming` limits dates to farm-today or later; `history` limits dates to before farm-today. `active/resolved` return corresponding stored advisories, including unresolved historical warnings until explicitly resolved. Each snapshot keeps its own identity, date, provider, retrieval time and values; snapshots are not current conditions. Response contains farm zone/today/coordinates, `cautionKey=weather.caution` and a `records` page. Each record includes `isStale`, `isSynthetic`, `isOfficialWarning=false`, original severity/status and localization key/guidance where applicable. Freshness allows up to 12 hours age and 5 minutes clock skew. Missing retrieval time is stale.

`GET /api/notifications/alerts` uses paging, type, unread and substring search. It extends existing notifications with farm association (where linked), category, routine/important/high priority, source, localization key/message, destination (`task`, `weather`, `supplies`, `enrollment`), task/advisory IDs, severity, freshness and `shouldInterrupt`. Routine alerts never interrupt. Only unread active severity >=2 advisories for today through four days ahead with fresh real-provider forecasts can interrupt. Synthetic demo records cannot interrupt. Stored warning severity remains rule controlled; no PAGASA feed is implemented or implied.

`PUT /api/notifications/{id}/acknowledge` is idempotent and sets existing read state; `acknowledgedAtUtc` equals `readAtUtc`. Reading/acknowledging suppresses further interruption for that notification; it stays in the center. A new deduplicated notification on severity change remains a meaningful update. No second notification database or acknowledgment column is introduced. Task/weather/stock alerts recheck current membership/owner access, including legacy reads/read actions; removed members cannot read former-farm notification content. Enrollment notifications are personal account updates and may lack farm association because the legacy record has no farm FK.

Existing Hangfire schedules, missing-reading rules, severity thresholds and dedupe keys are retained. Rain >=20/50 mm, wind >=50/80 km/h and maximum temperature >=35/40 C produce warning/severe forecast advisories. These are application thresholds, not official emergency classifications. Missing measurements never clear a prior warning. Legacy notification list retains its existing newest-100 cap; paged reads are the complete history contract.
