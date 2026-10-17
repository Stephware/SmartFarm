# Phase 3 — Crop management

Crop management builds on Phase 1 Identity and Phase 2 farm/plot authorization.
It uses the manually created `CropType` and `CropCycle` tables without changing
the SQL-first schema or EF Core migration history. The reference implementation
follows SmartFarm's existing Core contracts → Infrastructure EF Core service →
ASP.NET Core API/MVC controllers and Android Retrofit → repository → ViewModel →
Compose pattern. No CI/CD has been introduced.

## Permissions and global crop catalog

- **Owner / Manager:** create shared crop types, plan crops in their active plots,
  edit **Planned** cycles, and perform allowed status transitions.
- **Field Worker:** read crop types, schedules and history for farms they belong to.
- **Others:** no access; cross-farm requests return 404.
- Every mutation checks the current database membership. Client UI visibility is
  not considered authorization.

`CropType` is **global** in the existing schema (no FarmId). This phase lets a
farm Owner/Manager **add** a new globally shared crop type but intentionally
does not expose rename/deactivation of other farms' catalog entries. Duplicate
crop names are rejected by `UX_CropType_Name`; catalog changes must be reviewed
if farm-specific crop types become a future requirement. No crop types are
automatically seeded.

## Cycle behavior

The SQL-defined statuses are:

| Value | Status | Allowed next statuses |
| --- | --- | --- |
| 0 | Planned | Active, Cancelled |
| 1 | Active | Completed, Failed, Cancelled |
| 2 | Completed | None |
| 3 | Failed | None |
| 4 | Cancelled | None |

- Multiple future **Planned** crops can be recorded for a plot. The existing
  filtered unique index `UX_CropCycle_OneActivePerPlot` permits only **one Active**
  crop cycle per plot, including concurrent activation requests.
- A plan needs an active crop type and nonarchived plot. Crop defaults can fill
  its harvest unit and watering interval.
- Activation records an actual planting date; the date cannot be after the
  configured expected harvest date. A completed or failed crop must have an
  actual planting date (enforced by the SQL check constraint and transition
  rules).
- Failed and cancelled cycles require a reason. Completed cycles get
  `CompletedAtUtc`. Terminal cycles are immutable; history remains visible.
- Only Planned cycles may be rescheduled or change crop type. Changes and
  transitions require a Base64 `rowVersion`; stale requests return **409**.
- Ending an active cycle is required before archiving its plot or farm.
- Completing a cycle does **not** create harvest records. Partial harvesting
  and harvest amounts belong to the later Harvest Management phase.
- Planting dates are farm calendar dates (`DateOnly`), not UTC timestamps.

## API contract (all bearer-authenticated)

| Method | Route | Purpose |
| --- | --- | --- |
| GET / POST | `/api/farms/{farmId}/crop-types` | List active types / create a shared type |
| GET | `/api/farms/{farmId}/crop-cycles` | List all cycles, including history |
| GET | `/api/farms/{farmId}/crop-cycles/{cycleId}` | Read one cycle |
| POST | `/api/farms/{farmId}/plots/{plotId}/crop-cycles` | Create a Planned cycle |
| PUT | `/api/farms/{farmId}/crop-cycles/{cycleId}` | Edit a Planned cycle |
| POST | `/api/farms/{farmId}/crop-cycles/{cycleId}/transitions` | Change lifecycle status |

All mutations use request DTOs and service validation. The API returns 400
for invalid inputs, 404 for inaccessible resources, and 409 for collisions or
stale data. GETs return 200 with the user's authorized records. JSON `status`
values are the numeric SQL enum (0–4), not status strings. Android maps them
to labels and uses ISO `yyyy-MM-dd` strings for farm dates.

## Local testing

Run from the repository root:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
```

With your configured local `SmartFarmDB`, sign into the web UI. Open
**My farms → a farm → Crop management**. Add a crop type if necessary, plan
a crop, reschedule it, activate it by recording the planting date, then complete
or cancel/fail it. The history remains visible.

For Android, launch the debug app on an emulator while Web is running:
`cd mobile/SmartFarm.Android && ./gradlew assembleDebug testDebugUnitTest`.
Sign in → My farms → select a farm → Crop management. This phase is online-only;
Room and WorkManager synchronization of crop changes are not yet enabled.

Verify with two accounts:
1. Field Worker can read plans/history but cannot create types, plan, or transition.
2. Manager can plan/activate but cannot manage memberships.
3. Only one active cycle per plot; a second activation produces 409.
4. A stale `rowVersion` produces 409, and terminal cycles cannot be reopened.
5. A crop cannot be created in an archived plot or another farm.
6. A plot/farm containing an Active cycle cannot be archived.
7. Completed/failed/cancelled history remains readable, even when plots are
   subsequently archived.
8. Confirm no schema or migration-history changes occur.

Repository-only review does not prove runtime success. **Local .NET, SQL Server
and Android verification must pass before the PR is merged.**
