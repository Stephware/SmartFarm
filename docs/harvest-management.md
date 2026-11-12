# Harvest management

Reviewed against `dev` at `ac624be` on 2026-11-11. Shared Infrastructure services
serve independent MVC/API and Android. Partial records, task fulfillment, void
history, queued Android records and filtered reports are implemented; this source
review is not a new runtime acceptance result.

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
- Completing a crop cycle remains the **separate crop status action**.
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
  direct POST has no stable operation ID. Sync has a separate idempotent ledger; avoid retrying a
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
Recording now uses the stable-ID sync queue through the bearer API; voiding remains online.

Android shows local pending records separately from confirmed yield. Harvest
creation can fulfill eligible harvest tasks, but never closes the crop. Sync replay
must not create a second harvest. Report date/plot/crop filters apply before unit
and planting-cycle aggregation; voids are excluded from yield. Web exports entries
and cycle totals with filter context and CSV safety. Android insights expose a
read-only subset; no mobile cycle-total comparison or CSV exporter. Harvest quantity
never implies cash income. See [reports/finance](../.local/karpathy-wiki/wiki/project/finance-reports.md).

## Local verification

Use an initialized **disposable** test database and isolated Web/API configuration
before the write checks below; never point them at populated SmartFarmDB. Configure
both hosts and private photo storage as applicable using [local smoke setup](local-smoke-test.md).
Use approved test-schema preparation; do not blindly rerun guarded production-name
creation scripts on another database. These checklists are future checks, not
results produced by this documentation task.

With isolated disposable SQL configured on both hosts, from the repo root:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
# Separate terminal, required for Android:
dotnet run --project src/SmartFarm.Api
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
10. Test task fulfillment and stable-ID replay/receipt matching alongside the
    workflow; do not make unauthorized schema/history or jobs changes.

Actual SQL Server and Android runtime tests must pass before merging.

