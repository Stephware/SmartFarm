# Crop management

Reviewed against `dev` at `ac624be` on 2026-11-11. Current crop management uses
shared Core rules/Infrastructure services, independent MVC/API hosts and Android
Retrofit/repository/ViewModel/Compose. New planning/status writes remain online;
selected reads are cached. This guide describes source, not a fresh runtime run.

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
  and amounts are implemented in [Harvest management](harvest-management.md).
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

## Current presentation and cross-module effects

Web includes a monthly planting/watering/harvest agenda, crop timeline/photo history
and illustrated plot/crop views. Android includes illustrated plans/active/history
and quick monitoring/harvest entry, but no matching monthly agenda. Crop transitions
fulfill/cancel related tasks and dismiss reminders; inspect these services together.
Ending an active crop is separate from recording harvest. See
[frontend](../.local/karpathy-wiki/wiki/project/frontend.md) and
[current verification scope](../.local/karpathy-wiki/wiki/project/development.md).

## Local testing

Use an initialized **disposable** test database and isolated Web/API configuration
before the write checks below; never point them at populated SmartFarmDB. Configure
both hosts and private photo storage as applicable using [local smoke setup](local-smoke-test.md).
Use approved test-schema preparation; do not blindly rerun guarded production-name
creation scripts on another database. These checklists are future checks, not
results produced by this documentation task.

Run from the repository root:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
# Separate terminal, required for Android:
dotnet run --project src/SmartFarm.Api
```

With the isolated disposable database configured on both hosts, sign into Web. Open
**My farms → a farm → Crop management**. Add a crop type if necessary, plan
a crop, reschedule it, activate it by recording the planting date, then complete
or cancel/fail it. The history remains visible.

For Android, launch the debug app on an emulator while SmartFarm.Api is running on port 5081:
`cd mobile/SmartFarm.Android && ./gradlew assembleDebug testDebugUnitTest`.
Sign in → My farms → select a farm → Crop management. Planning/transitions remain online-only; selected crop reads are cached.
Other field modules have queued writes, so the app as a whole is not online-only.

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
