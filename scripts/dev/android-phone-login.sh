#!/usr/bin/env bash
set -euo pipefail

# Local debug only: forward Android's loopback:5081 to the developer's SmartFarm.Api.
# The app uses an API, never a direct connection to SQL Server.
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
if ! command -v adb >/dev/null 2>&1; then
  echo "Android platform-tools (adb) is required. Install it through Android Studio." >&2
  exit 1
fi
if ! curl --fail --silent --show-error --max-time 5 http://127.0.0.1:5081/api/status >/dev/null; then
  echo "SmartFarm.Api is not responding on 127.0.0.1:5081." >&2
  echo "First run: dotnet run --project src/SmartFarm.Api --launch-profile http" >&2
  exit 1
fi

if ! curl --fail --silent --show-error --max-time 10 http://127.0.0.1:5081/health/database >/dev/null; then
  echo "SmartFarm.Api is reachable, but its database health check failed." >&2
  echo "Check ConnectionStrings:SmartFarm and SQL Server in the API terminal." >&2
  echo "Do not reset or reseed your existing database to fix a connection issue." >&2
  exit 1
fi

adb devices
adb reverse tcp:5081 tcp:5081
echo "Forwarded Android 127.0.0.1:5081 to this computer's SmartFarm.Api."
(
  cd "$root/mobile/SmartFarm.Android"
  ./gradlew :app:installDebug
)
echo "Installed the local debug app. Open AniGo on the connected phone and sign in."
echo "Keep SmartFarm.Api running; adb reverse stops working after device disconnect."
