# Activities, inspections, issues and private photos

Reviewed against `dev` at `ac624be` on 2026-11-11. Current monitoring includes
activities, inspections/private photos, task integration, structured crop issues,
manager follow-up and selected offline sync. Source exists; no runtime acceptance
was performed by this documentation update. Existing databases need a reviewed
issue-field rollout; see [database](../.local/karpathy-wiki/wiki/project/database.md).

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
  Retrospective entry times, deleting/editing original field details, and
  photo removal are not implemented; manager follow-up metadata is versioned/editable.
- Activity logging can fulfill an eligible linked/matching task and dismiss reminders.
  It does not blindly complete somebody else's manual assignment.
- Android queues activities, structured observations, dependent photos and optional
  fertilizer/pesticide activity+stock use. Web activity form still records the
  activity separately; do not assume an equivalent atomic stock form.
- Structured category/severity must agree with health; Owner/Manager can link a
  same-cycle Inspection/Other task and explicitly mark an issue Open/Resolved with
  rowversion. Task completion does not automatically resolve the issue; follow-up
  is online-only and may update accessible ended-cycle history.

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
| POST | `/activities` | Add activity: `activityType` 1–5, optional `notes`/`farmTaskId` |
| POST | `/observations` | Add inspection: `healthStatus` 0–3, optional `growthStage`/`notes`, numeric `issueCategory`/`issueSeverity` |
| PUT | `/observations/{observationId}/follow-up` | Manager Open/Resolved, optional task, current rowVersion |
| PUT multipart | `/observations/{observationId}/photos/{photoId}` | Stable-ID replay-safe private photo upload |
| POST multipart | `/observations/{observationId}/photos` | Upload image using form field `photo` |
| GET | `/photos/{photoId}` | Download an authorized, private image |

Web: **My farms → Farm → Crop management → Activities, inspections & photos**
or `/Monitoring/Index?farmId=...&cycleId=...`.

## Private photo storage

In Development, Web and API share the fallback folder
`src/SmartFarm.Web/App_Data/observation-photos/` through the shared registration. This directory is ignored by Git, is **outside
wwwroot**, and can be overridden with `ObservationPhotos:StoragePath`.

Only supported image signatures and matching MIME types are accepted. The
storage key is a random GUID filename, not the submitted filename; each
download enforces authorization and sends `no-store` / `nosniff` headers.
A failed SQL insert attempts to remove the just-written file.

**Deployment:** outside Development startup requires an explicit absolute,
non-root private path outside `wwwroot`. Both hosts need the same durable mounted
volume and permissions; identical path strings on different disks do not share
photos. Existing files must be migrated/verified before retiring old storage.
SQL holds metadata, not bytes. Stable-ID retries compare identical existing bytes;
new photos require Active crops, existing saved-photo replay can survive completion.
See [offline/photo contract](../.local/karpathy-wiki/wiki/project/android.md) and
[recovery](../database/RECOVERY.md).

## Local verification

Use an initialized **disposable** test database and isolated Web/API configuration
before the write checks below; never point them at populated SmartFarmDB. Configure
both hosts and private photo storage as applicable using [local smoke setup](local-smoke-test.md).
Use approved test-schema preparation; do not blindly rerun guarded production-name
creation scripts on another database. These checklists are future checks, not
results produced by this documentation task.

From the repository root:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
# Separate terminal, required for Android:
dotnet run --project src/SmartFarm.Api
```

Configure the disposable test connection on both Web and API before testing. Android: open `mobile/SmartFarm.Android`, then use
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
8. Verify reviewed issue-field schema compatibility; test rowversion/follow-up roles,
   task behavior and identical-byte photo retries. Do not apply live schema changes
   as part of a test without separate authorization.

Local .NET/Android/SQL execution **must be verified before merging**.
