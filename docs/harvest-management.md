# Phase 6 — Harvest Management

Harvest Management follows the existing SmartFarm service/controller and
Android Retrofit/repository/ViewModel/Compose structure. The authoritative
database is the SQL-first `dbo.Harvest` table in `database/02_CreateSchema.sql`.
There are **no SQL migrations or schema changes**.

## Features and permissions

- Every **Active** crop cycle can have multiple partial harvest entries.
- Owner, Manager and Field Worker may record a partial harvest on an active,
  nonarchived plot in a farm they belong to.
- All active farm members can read harvest totals and history for their farm,
  including archived-plot history and completed crop cycles.
- Only Owner and Manager can void a harvest, with a required reason.
- Voiding records `VoidedAtUtc`, `VoidedByUserId` and `VoidReason` in
  the original row. It does **not** delete or overwrite the quantity.
- Voided entries are excluded from valid totals and counted separately.
- Completing a crop cycle remains the **separate Phase 3 crop status action**.
  Harvest entry creation never automatically completes the cycle, nor does
  completion automatically create a harvest.
- Failed, cancelled, completed and planned cycles cannot accept new harvests,
  though their histories remain readable and eligible entries can be voided.
- The saved harvest unit is always copied from `CropCycle.HarvestUnit`.
  Users cannot submit their own unit or cause mixed-unit totals for a cycle.
- Quantity uses positive `decimal(18,3)` precision with a maximum of
  three fractional digits. Notes are optional, up to 1000 characters.
- The server stamps `HarvestedAtUtc` using the current UTC time and
  records the authenticated user's ID. Backdated entries are not supported
  in this initial phase.
- Records are append-only except the atomic void operation. The database
  has no Harvest `rowversion` column, so double-void protection uses a
  conditional `UPDATE ... WHERE VoidedAtUtc IS NULL`.
- Record creation checks cycle status inside a serializable SQL transaction
  to coordinate with concurrent crop completion.
- Repeated submissions of the **record** endpoint create multiple records:
  idempotency tokens are not supported by this table; avoid retrying a
  potentially successful POST without refreshing the history first.

## Bearer API

Base: `/api/farms/{farmId}/crop-cycles/{cycleId}/harvests`

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/` | Get harvests, valid totals, void count and permissions |
| POST | `/` | Record a partial harvest (`quantity`, optional `notes`) |
| POST | `/{harvestId}/void` | Void an entry (`reason`) — Owner/Manager only |

The endpoints require Identity bearer authentication. They return 404 for
inaccessible farms/cycles/harvests, 400 for invalid input, and 409 for
attempts such as double-voiding or recording after cycle completion.
Browser MVC actions use cookie authentication and CSRF protection.

## Web and Android

Web: sign in → **My Farms → Farm → Crop management → Harvest records**.
Enter a quantity in the crop cycle's assigned unit and optional notes.
History shows all harvests (including voided ones), the active yield total
and reasons for any voids.

Android: sign in → **My Farms → Farm → Crop management → Harvest records**.
It calls the live API via the existing Identity bearer-token client.

Harvest operations are intentionally **online-only**. Task management,
automations, weather-triggered actions, notifications, Hangfire schedules,
Room persistence and WorkManager synchronization are out of scope.

## Local verification

Configure a reachable existing SQL Server, then from the repo root:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
```

From `mobile/SmartFarm.Android` with the Android SDK/emulator available:

```bash
./gradlew assembleDebug testDebugUnitTest
```

Test with two registered users:

1. Create a farm and plot, plan a crop and mark it **Active**.
2. Record two partial harvests (e.g. 2.500 kg and 1.250 kg).
   The valid total should be **3.750 kg** in both MVC and Android.
3. Confirm a Field Worker can record harvests but cannot void them.
4. Have the Owner void the 1.250 kg harvest with a reason. The valid
   total should become **2.500 kg**; the original entry must remain visible.
5. Void it again: expect 409; no second successful void.
6. Confirm invalid zero/negative/overprecise quantities are rejected.
7. Confirm another farm member cannot read or modify harvests here.
8. Complete the cycle using the existing Crop management action, then
   verify its harvest history remains readable but a new harvest is rejected.
9. Verify the assigned unit and timestamps; no harvest save should edit
   inventory balances or crop status.
10. Check no schema/migration/history changes or background jobs were added.

Actual SQL Server and Android runtime tests must pass before merging.

