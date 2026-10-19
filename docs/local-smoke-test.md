# SmartFarm — run the minimal web and Android smoke test

This is a **very small smoke test**, not a new dashboard. The browser shows
a Home page and the Android welcome screen includes **Check backend**.
Both use the existing .NET MVC/API host. It does not run tasks, Hangfire jobs,
weather automation, or offline sync. No schema changes or migrations are needed.

## 1. Get the correct branch

From a terminal in the SmartFarm repository:

```bash
git fetch origin
git switch feature/phase1-stabilization
```

**Important:** PR #14 proposes the missing Phase 4–6 features and the
smoke-test changes for `dev`. Until that PR passes local verification and
is merged, test `feature/phase1-stabilization`. After merging, switch to
`dev` and pull the verified merge commit. See
[Phase 1 stabilization](phase1-stabilization.md).

## 2. First test: .NET web (database NOT required)

Install the **.NET 10 SDK** and verify `dotnet --version` reports 10.x.
From the repository root:

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web
```

Keep that terminal running. In your web browser, open:

- **Web page:** http://localhost:5080/
- **Backend status:** http://localhost:5080/api/status

Expected backend status:

```json
{"app":"SmartFarm","status":"ok"}
```

You can verify from another terminal:

```bash
curl -i http://localhost:5080/api/status
```

Expect HTTP **200** with the JSON above. It only confirms the **web host and
HTTP route**, **not database connectivity** or functional CRUD.

For the DB connection check, in Development only:

```bash
curl -i http://localhost:5080/health/database
```

Expect HTTP **200 Healthy** if a configured SQL Server connection succeeds,
otherwise **503 Unhealthy**. This does not establish that all SQL tables match
the EF Core model.

## 3. Android smoke test

Install **Android Studio**, Android SDK **API 36**, and a working emulator.
Use Android Studio's appropriate Gradle JDK (JDK 17 compatible with the project
configuration). Open **`mobile/SmartFarm.Android`** as an Android Studio
project, **not** the repository root.

Wait for Gradle sync, start an Android emulator, and click **Run**.
You should see **Welcome to SmartFarm**, buttons for **Log in** and
**Create account**, and the new **Check backend** button.

While the .NET web server is running on the **same computer** as the emulator,
tap **Check backend**:

- **Backend reachable:** Android could call the .NET status API.
- **Cannot connect:** Start the .NET server, verify port 5080, or inspect
  emulator networking and Logcat. The Android debug URL is
  `http://10.0.2.2:5080/`. `10.0.2.2` is the Android emulator's alias for
  the host computer, not a SQL Server address.

Optional Android command line from `mobile/SmartFarm.Android`:

```bash
# macOS / Linux
./gradlew assembleDebug testDebugUnitTest

# Windows PowerShell
.\gradlew.bat assembleDebug testDebugUnitTest
```

To run the Compose instrumentation smoke test, start an emulator first and
execute `./gradlew connectedDebugAndroidTest` (or
`.\gradlew.bat connectedDebugAndroidTest` on Windows).

A **physical Android phone** needs a reachable HTTPS backend or explicitly
configured development networking; the emulator's `10.0.2.2` address does
not work from a physical device. No release backend URL is configured yet.

## 4. Optional: test accounts and farm data (SQL Server required)

Your existing `SmartFarmDB` uses **23 SQL-first tables** (7 Identity +
16 application tables). Do not recreate the schema if those tables already
exist; see `database/03_VerifySchema.sql` and
[`docs/backend-database-connection.md`](backend-database-connection.md).

**macOS:** Start Docker Desktop and from the `database` folder run:

```bash
docker compose up -d
docker compose ps
```

Make sure `database/.env` contains your existing `MSSQL_SA_PASSWORD`.
Then configure the Web connection string with your own password:

```bash
dotnet user-secrets set 'ConnectionStrings:SmartFarm' 'Server=127.0.0.1,14333;Database=SmartFarmDB;User ID=sa;Password=<your-password>;Encrypt=True;TrustServerCertificate=True;' --project src/SmartFarm.Web
```

**Windows:** Start your local SQL Server instance and use PowerShell
to configure a trusted local instance (change `localhost` for your actual
instance as needed):

```powershell
dotnet user-secrets set 'ConnectionStrings:SmartFarm' 'Server=localhost;Database=SmartFarmDB;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;' --project src/SmartFarm.Web
```

For SQL Server Express, the server may be `.\SQLEXPRESS`; see the Windows
database instructions. Restart `dotnet run` after setting the secret.
Recheck `/health/database`: expect HTTP **200**.

Now open `http://localhost:5080/Account/Register` or tap **Create
account** in Android. Register, log in, and open **My farms**. Test creating a
farm and plot; use the existing links for crops, inventory, and harvests
where the underlying feature branch includes them.

**Apple Silicon note:** The SQL Server 2022 `linux/amd64` container uses
emulation and is not officially supported by Microsoft on Apple Silicon.
If it fails, use a supported x86-64 SQL Server host instead.

## What success does and does not mean

| Check | What it proves |
| --- | --- |
| Web `/` | ASP.NET Core + Razor view renders |
| `GET /api/status` | .NET backend HTTP responds without database |
| Android **Check backend** | Compose → ViewModel → repository → Retrofit → ASP.NET API works |
| `/health/database` = 200 | EF Core can connect to the selected SQL Server |
| Log in and create farm | Basic Identity/EF/model/database workflows are functional |

If a command fails, keep the full output. No automated builds or device tests
were executed by the assistant in this change; run them locally before merging.

