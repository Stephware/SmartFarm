# Authentication and authorization (Phase 1)

SmartFarm uses the existing seven ASP.NET Core Identity tables. No new tables, migrations, or database seeding are required. This phase follows the MVC account-controller pattern from the reference VeterinaryClinicSystem and the separated Web API controller pattern in the reference API projects; authentication itself uses ASP.NET Core Identity rather than copying custom password handling.

## Run the web application

1. Set up the original SQL Server `SmartFarmDB` using [the database guide](../database/README.md). The EF Core migration baseline remains **unapplied**.
2. Configure your local Web connection string as explained in [backend database connection](backend-database-connection.md). Use `ConnectionStrings:SmartFarm` through user secrets or environment variables.
3. From the repository root:

   ```bash
   dotnet tool restore
   dotnet restore SmartFarm.slnx
   dotnet build SmartFarm.slnx
   dotnet test SmartFarm.slnx
   dotnet run --project src/SmartFarm.Web
   ```

4. Open `http://localhost:5080/Account/Register`, register a test user, then log in at `/Account/Login`, open `/Account/Profile`, and log out using the header form. The MVC forms use antiforgery tokens and Identity cookies.
5. Check `http://localhost:5080/health/database` returns 200 with a reachable database (Development only).

Registration asks for full name, email, password and confirmation. Passwords require 10+ characters, uppercase, lowercase and a number. Identity enforces duplicate email checks and password lockout after five failed attempts for 10 minutes. Sign-up does **not** create a farm or grant management rights.

## Android bearer API

The API uses Identity's **opaque, proprietary bearer tokens**, not JWTs. Browser cookies are not accepted on these bearer-only API endpoints.

| Endpoint | Request | Result |
| --- | --- | --- |
| `POST /api/auth/register` | JSON `fullName, email, password, confirmPassword` | 201 when account created |
| `POST /api/auth/login` | JSON `email, password` | Access/refresh tokens |
| `POST /api/auth/refresh` | JSON `refreshToken` | Fresh access/refresh tokens |
| `GET /api/auth/me` | `Authorization: Bearer <access-token>` | Authenticated account |
| `GET /api/farms/{farmId}/access` | Bearer token | Caller’s farm-specific permission (or 404) |

Access tokens expire after 30 minutes and refresh tokens after seven days. Refresh requires a valid Identity security stamp and an enabled account. The Web application does not expose these endpoints as cookie-authenticated routes.

To test API registration/login locally in Development:

```bash
curl -i -H 'Content-Type: application/json' \
  -d '{"fullName":"Test Farmer","email":"tester@example.test","password":"TestPassword123","confirmPassword":"TestPassword123"}' \
  http://localhost:5080/api/auth/register

curl -i -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.test","password":"TestPassword123"}' \
  http://localhost:5080/api/auth/login
```

Use the returned `accessToken` (do not paste it into shared terminals, logs, tickets or the repo) when testing `/api/auth/me`. Credentials above are only examples; change them in your development environment. **Use HTTPS outside localhost development.**

## Farm permissions

The SQL schema makes `Farm.OwnerUserId` the **Owner**. Active `FarmMember` rows use `Role=1` for Manager and `Role=2` for Field Worker. A removed membership or archived farm grants no access.

| Role | View farm | Manage farm | Manage membership |
| --- | --- | --- | --- |
| Owner | Yes | Yes | Yes |
| Manager | Yes | Yes | No |
| Field Worker | Yes | No | No |
| Nonmember | No | No | No |

The service is in `SmartFarm.Infrastructure/Security/FarmAccessService.cs`; policies are defined in `SmartFarm.Core/Services/Security/FarmAccessRules.cs`. It checks membership **against the database per request**, not by trusting a global role claim. Future feature endpoints must call it before accessing farm-owned records. Global `AspNetRoles` entries are not created during registration.

## Android

Open `mobile/SmartFarm.Android` in Android Studio. The debug build uses `http://10.0.2.2:5080/` for the emulator and permits cleartext **only** to local development hosts through its debug network-security configuration. Production builds require a real HTTPS API address; the placeholder release URL is intentionally empty.

Run `./gradlew assembleDebug testDebugUnitTest` from the Android project folder. Start the local ASP.NET Core Web host, run the emulator, register/log in using the Compose screens, then visit the account screen and log out.

**Security limitations to address before production:** Android tokens currently live only in memory (restart signs the user out); logout discards local tokens but cannot instantly revoke already issued opaque access tokens. Do not store tokens in Room or plaintext preferences. Email verification, password recovery, two-factor authentication, production HTTPS/configuration, and secure persisted token storage are future work. The public registration endpoint should have rate limiting/abuse controls before public deployment.

## Testing and constraints

- `dotnet test SmartFarm.slnx` includes auth input and role-matrix unit tests, along with the existing offline foundation tests.
- Complete web/database/API registration, login, refresh, disabled-user, unauthorized access and Android emulator smoke tests **require your local SQL Server and Android SDK**.
- Do not call `Database.Migrate()`, `EnsureCreated()`, or alter migration history for this feature.
- No GitHub Actions or CI/CD is introduced by this phase.
