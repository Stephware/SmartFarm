# Android physical-phone sign-in: API connection

The Android app **never connects directly to SQL Server**. It calls SmartFarm.Api, which uses
the configured `ConnectionStrings:SmartFarm` database connection. Do not expose SQL Server
port 1433 to a phone or place database credentials inside the APK.

## Required services

1. Start SQL Server and verify the existing SmartFarm database is available. Keep all data.
2. Start **SmartFarm.Api** (not only SmartFarm.Web) with the *same database* as the web app.
3. Confirm `GET /api/status` responds with `{"app":"SmartFarm","status":"ok"}`.
   This endpoint checks HTTP reachability only; if authentication still fails, also inspect
   the API's database health endpoint/logs and its configured connection string.

## Preferred for a real Android phone connected over USB (local debug)

From the repository root on the development computer, the quickest path is:

```sh
dotnet run --project src/SmartFarm.Api --launch-profile http
# In a separate terminal after connecting/authorizing the phone:
bash scripts/dev/android-phone-login.sh
```

The helper checks API reachability, runs `adb reverse`, and installs the debug APK.
Or perform those steps manually:


```sh
dotnet run --project src/SmartFarm.Api --launch-profile http
# In a second terminal, with USB debugging enabled and device authorized:
adb devices
adb reverse tcp:5081 tcp:5081
adb reverse --list
# Install/run the normal debug APK with Android Studio or:
cd mobile/SmartFarm.Android
./gradlew :app:installDebug
```

A **physical device** automatically uses `http://127.0.0.1:5081/` when the default
debug API URL is selected; `adb reverse` forwards this to the development machine.
The Android **emulator** continues to use `http://10.0.2.2:5081/` without an adb reverse.
This is a local development solution only. USB debugging/adb reverse must remain active
for the phone to reach the development machine this way.

## Alternative: same private Wi-Fi/LAN (debug builds only)

Use this only on a trusted LAN, preferably with test accounts; plain HTTP exposes login
credentials to other systems able to observe that network. HTTPS is preferred.

1. Find the development machine's IPv4 address, e.g. `192.168.1.25` (not the phone's IP).
2. Make the API listen on the LAN interface, for example:
   ```sh
   ASPNETCORE_ENVIRONMENT=Development dotnet run --project src/SmartFarm.Api --no-launch-profile --urls "http://0.0.0.0:5081"
   ```
   Permit the API port through the host firewall only for the trusted local network.
3. Check from the **phone's own browser**:
   `http://192.168.1.25:5081/api/status`.
   If this does not work, the app cannot work either. Check Wi-Fi isolation/hotspot settings,
   host firewall, API binding and that both devices can reach one another.
4. Build and install a debug APK with that explicit endpoint:
   ```sh
   cd mobile/SmartFarm.Android
   ./gradlew :app:installDebug -PsmartfarmDebugApiUrl=http://192.168.1.25:5081/
   ```
   Rebuild if the host's IP changes. The URL must end in `/`.

Only literal loopback or private IPv4 endpoints are permitted over HTTP in debug builds.
Public HTTP endpoints are rejected by the API client. Release builds require HTTPS.

## When the computer is turned off / remote access

Neither `10.0.2.2`, `127.0.0.1`/adb reverse, nor a private home Wi-Fi IP works when
the host serving SmartFarm.Api is off or unreachable. For normal phone use, deploy the API
to a **reachable HTTPS server** that can securely access the SmartFarm database, and build
a release APK with `-PsmartfarmReleaseApiUrl=https://your-api-host/`.
Do not expose SQL Server to the public internet.

## Diagnosing failures without exposing credentials

- Phone browser cannot open `/api/status`: wrong address, missing `adb reverse`, API not
  started, Wi-Fi isolation, firewall, HTTP blocked or port not listening.
- `/api/status` works but login returns **401**: verify account credentials and whether
  Web and API use the **same populated database**.
- `/api/status` works but login returns **503**: inspect API terminal logs and database
  configuration/health; a reachable API does not prove SQL Server is reachable.
- Release APK crashes on an empty API URL: provide the HTTPS `smartfarmReleaseApiUrl`
  at build time. Android never discovers database servers itself.
- If using a reverse proxy under a path prefix, use an API base URL that maps routes
  such as `/api/auth/login` correctly; check any configured PathBase.

Never paste passwords, tokens, private database connection strings or SMTP/Gemini keys
into issues, screenshots, diagnostics, or the repository.
