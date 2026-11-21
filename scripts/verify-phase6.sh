#!/usr/bin/env bash
# Strict, opt-in local end-to-end verification. Never points SQL writes at SmartFarmDB.
# Example:
#   SMARTFARM_API_URL=http://localhost:5081 SMARTFARM_WEB_URL=http://localhost:5080 \
#   SMARTFARM_TEST_CONNECTION='Server=...;Database=SmartFarmEnrollmentTest_verify;...;' \
#   bash scripts/verify-phase6.sh
# The disposable test database must already contain the current SmartFarm schema.
set -euo pipefail
cd "$(dirname "$0")/.."

fail() { printf 'ERROR: %s\n' "$1" >&2; exit 1; }
command -v curl >/dev/null || fail "curl is required."
command -v dotnet >/dev/null || fail "Install the .NET 10 SDK."
: "${SMARTFARM_API_URL:?Set SMARTFARM_API_URL to the running development API.}"
: "${SMARTFARM_WEB_URL:?Set SMARTFARM_WEB_URL to the running development MVC website.}"
: "${SMARTFARM_TEST_CONNECTION:?Set SMARTFARM_TEST_CONNECTION to a disposable SQL Server database.}"

# Both SQL suites enforce the same catalog-prefix allowlist in C#.
# The prefix check here provides fast feedback before any tests run.
case "${SMARTFARM_TEST_CONNECTION}" in
  *SmartFarmEnrollmentTest_*|*SmartFarmEnrollmentBackend_*) ;;
  *) fail "Use a disposable SmartFarmEnrollmentTest_* or SmartFarmEnrollmentBackend_* database, never SmartFarmDB." ;;
esac
export SMARTFARM_ENROLLMENT_TEST_CONNECTION="${SMARTFARM_ENROLLMENT_TEST_CONNECTION:-$SMARTFARM_TEST_CONNECTION}"
case "${SMARTFARM_ENROLLMENT_TEST_CONNECTION}" in
  *SmartFarmEnrollmentTest_*|*SmartFarmEnrollmentBackend_*) ;;
  *) fail "Enrollment tests also require a disposable test database." ;;
esac

check_200() {
  local label="$1" url="$2" code
  code="$(curl --silent --show-error --max-time 15 --output /dev/null --write-out '%{http_code}' "$url")" ||
    fail "$label is unreachable: $url"
  [[ "$code" == 200 ]] || fail "$label returned HTTP $code: $url"
  printf 'PASS: %s (HTTP 200)\n' "$label"
}

check_200 "API status" "${SMARTFARM_API_URL%/}/api/status"
check_200 "API and SQL tables" "${SMARTFARM_API_URL%/}/health/database"
check_200 "MVC home page" "${SMARTFARM_WEB_URL%/}/"
check_200 "MVC and SQL tables" "${SMARTFARM_WEB_URL%/}/health/database"

printf '\n[1/2] .NET API, MVC, SQL workflow and worker tests\n'
dotnet test SmartFarm.slnx --configuration Release
printf '\n[2/2] Android build and Retrofit/Room unit tests\n'
(cd mobile/SmartFarm.Android &&
  bash ./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest)

if [[ "${RUN_ANDROID_DEVICE_TESTS:-0}" == 1 ]]; then
  printf '\n[Device] Room, image and UI instrumentation tests\n'
  (cd mobile/SmartFarm.Android &&
    bash ./gradlew --no-daemon :app:connectedDebugAndroidTest)
else
  printf '\nAndroid emulator tests were not run (set RUN_ANDROID_DEVICE_TESTS=1).\n'
fi

printf '\nPASS: Live endpoints, SQL-backed workflows, backend tests and Android build checks.\n'
printf 'Verify live Hangfire execution in its existing dashboard/logs; these tests check worker scheduling and rules, not a running worker process.\n'
