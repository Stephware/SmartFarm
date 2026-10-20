# Phase 2 — Farms, plots, and membership

Uses the existing SQL-first `Farm`, `Plot`, `FarmMember` and Identity tables. There are **no migrations or SQL schema changes**.

Patterns follow the earlier VeterinaryClinicSystem MVC controller/viewmodel separation, CampusEquipment API controller/service layering, and existing SmartFarm EF Core configurations; Android uses the established Retrofit/repository/ViewModel/Compose structure.

## Access model

- **Owner:** `Farm.OwnerUserId`. Can edit/archive a farm, manage plots, issue account-bound invitations, change membership roles, and remove members.
- **Manager:** an active `FarmMember.Role = 1`. Can edit farm details and manage plots, but cannot manage members or archive a whole farm.
- **Field Worker:** an active `FarmMember.Role = 2`. Can view farm details and plots, but cannot edit.
- **Everyone else:** no farm access. Archived farms and removed memberships grant no access. Disabled accounts are denied.

Web views use Identity cookies and antiforgery-protected POST forms; API endpoints require Phase 1 bearer tokens. Permission checks run on the server for every operation, not in the UI only.

## Local usage

1. Pull the Phase 2 feature branch and verify your SQL Server is available.
2. Configure your Web connection string using [the backend guide](backend-database-connection.md).
3. Run:
   ```bash
   dotnet restore SmartFarm.slnx
   dotnet build SmartFarm.slnx
   dotnet test SmartFarm.slnx
   dotnet run --project src/SmartFarm.Web
   ```
4. Register/sign in at `http://localhost:5080/Account/Login`.
5. Use **My farms → Add a farm**, add/edit/archive plots, and invite an existing registered account.

For Android, open `mobile/SmartFarm.Android` in Android Studio and run `./gradlew assembleDebug testDebugUnitTest`. With the Web server running, the emulator connects to `http://10.0.2.2:5080/` in debug builds only. Sign in and choose **My farms**. Android is online-only for this phase; Room offline sync comes later.

## API

Every route requires `Authorization: Bearer <accessToken>`. A nonmember receives 404 for farm-scoped reads and writes to avoid farm enumeration.

| Method | Route | Action |
| --- | --- | --- |
| GET / POST | `/api/farms` | List accessible farms / create an owned farm |
| GET / PUT / DELETE | `/api/farms/{farmId}` | Read, update, soft archive (owner only for archive) |
| GET / POST | `/api/farms/{farmId}/plots` | List/create plots |
| GET / PUT / DELETE | `/api/farms/{farmId}/plots/{plotId}` | Read, update, soft archive plot |
| GET | `/api/farms/{farmId}/members` | List active members |
| POST | `/api/farms/{farmId}/members/invitations` | Owner issues registered-user invitation |
| POST | `/api/farm-invitations/accept` | Invited account accepts a token |
| PUT / DELETE | `/api/farms/{farmId}/members/{memberId}` | Owner changes role / removes member |

Creation uses the existing `FarmInput`/`PlotInput` DTOs; updates and soft archives require a current `rowVersion` as Base64. Stale updates return **409 Conflict**. Duplicate active plot names and memberships are protected by existing SQL unique filtered indexes; conflicting saves return 409.

## Invitation limitations

Only an Owner can generate a 24-hour, Data-Protection-signed invitation. It is bound to an **already registered** target user and cannot grant the Owner role. The owner manually shares the web link (no email sender exists). The target signs into their account and accepts, creating an active `FarmMember`. Already-active accounts cannot accept duplicates; a removal after issuance invalidates that invitation.

Because the existing SQL schema has **no invitation table**, pending invitations cannot be listed or individually revoked by token. Revoking access is done by removing the member; if a link was issued before removal, the link is rejected afterward. A durable, individually revocable invitation and email delivery workflow would require an approved schema and feature change. Do not claim those functions exist today.

Data Protection keys must be persisted/shared for multi-instance production deployments; local development keys are used for now. Send invitation links privately, use HTTPS outside local development, and avoid recording tokens in logs. No automatic migrations or database initialization are triggered.

## Required manual checks

1. Create two test accounts. Account A creates a farm and two plots.
2. Confirm B cannot see or modify A's farm before joining.
3. A issues a Field Worker invitation to B. B accepts it; confirm B can view but cannot edit.
4. A switches B to Manager; confirm B can edit plots but cannot invite/remove members or archive the farm.
5. A removes B; verify B loses access. A can issue a **new** invitation if needed.
6. Create duplicate plot names; verify conflict handling.
7. Open the same plot/farm in two sessions; update from one, then submit the stale row version from the other; expect 409.
8. Confirm archived plots no longer appear, and archiving a farm removes it from active listings.
9. Test the Android emulator against the Web API.

No CI/CD is required for this phase. Local build/runtime tests must pass before merging the PR into `dev`.
