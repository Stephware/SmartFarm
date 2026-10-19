#!/usr/bin/env bash
# From the repo root: bash scripts/verify-phase1.sh [--skip-android]
set -euo pipefail
cd "$(dirname "$0")/.."
command -v dotnet >/dev/null || { echo "Install .NET 10 SDK." >&2; exit 1; }
echo "[1/4] SDK"; dotnet --version
echo "[2/4] .NET restore and build"
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
echo "[3/4] .NET tests"
dotnet test SmartFarm.slnx --no-build
if [[ "$*" == *"--skip-android"* ]]; then
  echo "[4/4] Android skipped by request."
  exit 0
fi
command -v java >/dev/null || { echo "Install Java/Android Studio Gradle JDK." >&2; exit 1; }
echo "[4/4] Android debug build and unit tests"
(cd mobile/SmartFarm.Android && bash ./gradlew :app:assembleDebug :app:testDebugUnitTest)
echo "PASS: .NET and Android local build checks."

