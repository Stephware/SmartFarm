#!/usr/bin/env bash
# Run from any working directory: bash scripts/verify-phase-a.sh [--sql] [--device] [--skip-android]
# --sql requires a disposable, initialized SMARTFARM_TEST_CONNECTION.
# --device runs connected instrumentation tests on an already authorized Android emulator/device.
set -euo pipefail
cd "$(dirname "$0")/.."

run_sql=false
run_device=false
skip_android=false
for arg in "$@"; do
    case "$arg" in
        --sql) run_sql=true ;;
        --device) run_device=true ;;
        --skip-android) skip_android=true ;;
        -h|--help)
            echo "Usage: bash scripts/verify-phase-a.sh [--sql] [--device] [--skip-android]"
            echo "By default SQL integration and connected Android tests are NOT run."
            exit 0 ;;
        *) echo "Unknown argument: $arg" >&2; exit 2 ;;
    esac
done
if "$run_device" && "$skip_android"; then
    echo "--device and --skip-android cannot be combined." >&2; exit 2
fi
command -v dotnet >/dev/null || { echo "Missing .NET SDK." >&2; exit 2; }
command -v python3 >/dev/null || { echo "Missing Python 3." >&2; exit 2; }

echo "== SmartFarm Phase A: compilation and regression tests =="
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx -c Release --no-restore
dotnet test SmartFarm.slnx -c Release --no-build
python3 -m unittest discover -s database/verification -v
python3 -m unittest discover -s scripts/tests -v

if "$run_sql"; then
    if [[ -z "${SMARTFARM_TEST_CONNECTION:-}" ]]; then
        echo "SQL integration NOT run: set SMARTFARM_TEST_CONNECTION to a disposable initialized SQL Server database." >&2
        exit 2
    fi
    echo "== Running SQL-backed API farm-to-harvest regression =="
    # The test enforces disposable database name prefixes before making any changes.
    SMARTFARM_REQUIRE_SQL_TESTS=1 dotnet test tests/SmartFarm.Api.Tests/SmartFarm.Api.Tests.csproj \
        -c Release --no-build --filter 'FullyQualifiedName~BusinessWorkflowSqlTests'
else
    echo "NOT VERIFIED: SQL-backed end-to-end integration. Rerun with --sql and a disposable test database."
fi

if "$skip_android"; then
    echo "NOT VERIFIED: Android compilation and tests (--skip-android)."
else
    command -v java >/dev/null || { echo "Missing Java/Android SDK JDK." >&2; exit 2; }
    echo "== Android compilation, unit tests and instrumented-test compilation =="
    (cd mobile/SmartFarm.Android && bash ./gradlew --no-daemon \
        :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin)
    if "$run_device"; then
        command -v adb >/dev/null || { echo "adb is required for --device." >&2; exit 2; }
        if ! adb devices | awk 'NR>1 && $2=="device" { found=1 } END { exit !found }'; then
            echo "No authorized Android emulator/device connected. Device tests NOT run." >&2
            exit 2
        fi
        echo "== Running real connected Android instrumentation tests =="
        (cd mobile/SmartFarm.Android && bash ./gradlew --no-daemon :app:connectedDebugAndroidTest)
    else
        echo "NOT VERIFIED: Android on-device instrumentation execution. Rerun with --device."
    fi
fi

echo "PASS: requested Phase A checks completed."
echo "Reminder: SQL restoration, private-photo recovery, and interactive browser acceptance require separate real-environment verification."
