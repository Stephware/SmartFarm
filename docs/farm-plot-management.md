# Farms, plots and membership

Reviewed against `dev` at `ac624be` on 2026-11-11; source review, not runtime sign-off.
Uses the SQL-first `Farm`, `Plot`, `FarmMember` and Identity tables. Code-based enrollment adds `Farm.JoinCode` and `FarmJoinRequest`; apply the [non-destructive upgrade](../database/04_FarmerRegistrationApproval.sql) to existing databases.

Patterns follow the earlier VeterinaryClinicSystem MVC controller/viewmodel separation, CampusEquipment API controller/service layering, and existing SmartFarm EF Core configurations; Android uses the established Retrofit/repository/ViewModel/Compose structure.

## Access model

- **Owner:** `Farm.OwnerUserId`. Can edit/archive a farm, manage plots, review join requests, change membership roles, and remove members.
- **Manager:** an active `FarmMember.Role = 1`. Can edit farm details and manage plots, but cannot manage members or archive a whole farm.
- **Field Worker:** an active `FarmMember.Role = 2`. Can view farm details and plots, but cannot edit.
- **Everyone else:** no farm access. Archived farms and removed memberships grant no access. Disabled accounts are denied.

Web views use Identity cookies and antiforgery-protected POST forms; API endpoints require Phase 1 bearer tokens. Permission checks run on the server for every operation, not in the UI only.

## Local usage

Use an initialized **disposable** test database and isolated Web/API configuration
before the write checks below; never point them at populated SmartFarmDB. Configure
both hosts and private photo storage as applicable using [local smoke setup](local-smoke-test.md).
Use approved test-schema preparation; do not blindly rerun guarded production-name
creation scripts on another database. These checklists are future checks, not
results produced by this documentation task.

1. Preserve the checkout; for new feature work start from the intended latest dev
   base and verify SQL configuration without modifying populated data.
2. Configure isolated Web/API test connections using [the backend guide](backend-database-connection.md).
3. Run:
   ```bash
   dotnet restore SmartFarm.slnx
   dotnet build SmartFarm.slnx
   dotnet test SmartFarm.slnx
   dotnet run --project src/SmartFarm.Web
# Separate terminal, required for Android:
dotnet run --project src/SmartFarm.Api
   ```
4. Register/sign in at `http://localhost:5080/Account/Login`.
5. Use **My farms → Add a farm**, add/edit/archive plots, and share the farm code with farmers. Approve their pending requests in the dashboard.

For Android, open `mobile/SmartFarm.Android` in Android Studio and run `./gradlew assembleDebug testDebugUnitTest`. With SmartFarm.Api running, the emulator connects to `http://10.0.2.2:5081/` in debug builds only. Sign in and choose **My farms**. Selected farm/plot/member reads are cached on connection failure. Administrative
mutations and enrollment stay online; other field modules have queued writes.

## API

Every route requires `Authorization: Bearer <accessToken>`. A nonmember receives 404 for farm-scoped reads and writes to avoid farm enumeration.

| Method | Route | Action |
| --- | --- | --- |
| GET / POST | `/api/farms` | List accessible farms / create an owned farm |
| GET / PUT / DELETE | `/api/farms/{farmId}` | Read, update, soft archive (owner only for archive) |
| GET / POST | `/api/farms/{farmId}/plots` | List/create plots |
| GET / PUT / DELETE | `/api/farms/{farmId}/plots/{plotId}` | Read, update, soft archive plot |
| GET | `/api/farms/{farmId}/members` | List active members |
| GET / POST | `/api/farm-join-requests` | Caller lists own requests / submits farm code |
| GET | `/api/farms/{farmId}/join-requests` | Owner views code and pending requests |
| POST | `/api/farms/{farmId}/join-requests/{requestId}/decision` | Owner approves/rejects request |
| PUT / DELETE | `/api/farms/{farmId}/members/{memberId}` | Owner changes role / removes member |

Creation uses the existing `FarmInput`/`PlotInput` DTOs; updates and soft archives require a current `rowVersion` as Base64. Stale updates return **409 Conflict**. Duplicate active plot names and memberships are protected by existing SQL unique filtered indexes; conflicting saves return 409.

## Registration and owner approval

Farmers register with name/email/password, then sign in and submit the owner’s
code from My farms. Signup creates no membership. Pending users may sign in and see status but cannot read farm data. Owners review named applicants on the dashboard or member management and approve them as Field Workers, or reject them. Only the owner can retrieve the code or applicants; Managers cannot approve requests. Decisions preserve history, and repeated submissions do not duplicate pending requests or active membership.

Legacy invitation endpoints return **410 Gone**; old tokens cannot create membership. Database upgrades are explicit and never run automatically. Signup does not create a farm or grant membership until approval.

## Required manual checks

1. Create two test accounts. Account A creates a farm and two plots.
2. Confirm B cannot see or modify A's farm before joining.
3. B submits A’s farm code. Confirm Pending status grants no farm access. A approves B in the dashboard; confirm B can view but cannot edit.
4. A switches B to Manager; confirm B can edit plots but cannot approve/remove members or archive the farm.
5. A removes B; verify B loses access. B can submit another request if access is needed again.
6. Create duplicate plot names; verify conflict handling.
7. Open the same plot/farm in two sessions; update from one, then submit the stale row version from the other; expect 409.
8. Confirm archived plots no longer appear, and archiving a farm removes it from active listings.
9. Test the Android emulator against SmartFarm.Api, including farmer registration, pending status and owner approval.

Owners can rotate codes with a current-code precondition and view request history;
applicants can cancel their Pending request. Decisions and in-app notices are
transactional; code rotation does not cancel existing requests. Apply the reviewed
workflow upgrade as appropriate; see [database setup](../database/README.md).

Farm location search/manual coordinates and schematic plot layout are implemented.
Layout is not a geographic boundary map. Preserve farm-context navigation and
owner-only enrollment data; see [frontend conventions](../.local/karpathy-wiki/wiki/project/frontend.md).
CI now exists, but runtime SQL/device checks still need explicit environments.
