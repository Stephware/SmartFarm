# Phase 4 — Activities, inspections, and observation photos

This phase extends the existing `CropActivity`, `CropObservation`, and
`ObservationPhoto` SQL tables. There are **no schema changes, migrations, or
CI/CD workflows**. The implementation follows SmartFarm's Core contracts,
Infrastructure services, ASP.NET Core MVC/API controllers and Android
Retrofit/repository/ViewModel/Compose pattern.

## Scope

- Record watering, fertilizing, pesticide, weeding and other farm activities
  against an **Active** crop cycle.
- Record crop inspection/health observations: Not Assessed (0), Healthy (1),
  Needs Attention (2), Critical (3), with optional growth stage and notes.
- Show chronological activity and observation history even for completed,
  failed, or cancelled crop cycles.
- Upload JPEG, PNG or WebP files (5 MB maximum) to an observation during an
  active crop cycle; show thumbnails in the browser, and view uploaded images
  from Android by calling authenticated API endpoints.
- Every activity and observation is an **append-only entry** stamped with
  the server's current UTC time and the authenticated user's Identity GUID.
  Retrospective entry times, deleting/editing a historical record, and
  photo removal are not implemented.
- Activity logging does not create or complete `FarmTask` rows. The schema
  supports `CropActivity.FarmTaskId`, but task integration comes later.
- No automatic notifications, external weather integration, or offline
  synchronization are added by this phase.

## Authorization

- Owners, Managers and **Field Workers** can record activity, add observations
  and attach photos when the crop cycle is Active and the plot is not archived.
- Any active farm member may read the monitoring history/photos for that farm's
  crop cycles, even after those cycles reach a terminal state.
- A removed or disabled member cannot access monitoring history or images;
  accessing a crop in another farm returns 404.
- Photo bytes are not published in `wwwroot`. Every photo request verifies
  farm access and the observation-to-cycle relationship first.
- Web uses cookie auth and antiforgery-protected POST forms. Android calls
  bearer-only API routes; images are retrieved with bearer authentication.

## Routes

Bearer API base:
`/api/farms/{farmId}/crop-cycles/{cycleId}/monitoring`

| Method | Relative route | Purpose |
| --- | --- | --- |
| GET | `/` | Retrieve activity and observation histories, including photo metadata |
| POST | `/activities` | Add activity: `activityType` 1–5 and optional `notes` |
| POST | `/observations` | Add inspection: `healthStatus` 0–3, optional `growthStage` and `notes` |
| POST multipart | `/observations/{observationId}/photos` | Upload image using form field `photo` |
| GET | `/photos/{photoId}` | Download an authorized, private image |

Web: **My farms → Farm → Crop management → Activities, inspections & photos**
or `/Monitoring/Index?farmId=...&cycleId=...`.

## Private photo storage

By default the Web app saves images under
`src/SmartFarm.Web/App_Data/observation-photos/` when launched with that
project as the content root. This directory is ignored by Git, is **outside
wwwroot**, and can be overridden with `ObservationPhotos:StoragePath`.

Only supported image signatures and matching MIME types are accepted. The
storage key is a random GUID filename, not the submitted filename; each
download enforces authorization and sends `no-store` / `nosniff` headers.
A failed SQL insert attempts to remove the just-written file.

**Production limitation:** local filesystem storage is intended only for
single-instance development. Before deployment, configure a durable,
backed-up private shared storage service and consider image transcoding,
malware scanning, retention/deletion rules, and stricter upload rate limits.
The SQL table stores only metadata, not image bytes.

## Local verification

From the repository root:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
```

Start your existing `SmartFarmDB` and configure the Web connection string
before testing. Android: open `mobile/SmartFarm.Android`, then use
`./gradlew assembleDebug testDebugUnitTest` and launch an emulator.

Test with at least two accounts:
1. Create a farm, plot, and **Active** crop cycle.
2. Record watering and a Healthy inspection as Owner.
3. Log in as Field Worker and record a second activity and inspection.
4. Upload JPEG, PNG, WebP photos and view them from both web and Android.
5. Test a file over 5 MB, a fake image, and a missing photo.
6. Confirm a nonmember cannot view history or download photo URLs.
7. Complete the crop cycle; verify history is still visible but recording
   and uploading are rejected.
8. Confirm SQL schema, migration history, and other phases are untouched.

Local .NET/Android/SQL execution **must be verified before merging**.
