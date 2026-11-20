#!/usr/bin/env bash
# Phase 8: run the AniGo onboarding and adjacent auth/farm UI suites on a live emulator,
# then export actual Compose screenshots from the Android debug app.
# Invoke from mobile/SmartFarm.Android, after the emulator boots.
set -uo pipefail

bash ./gradlew --no-daemon :app:connectedDebugAndroidTest \
  -PsmartfarmDebugApiUrl=http://127.0.0.1:5081/ \
  -Pandroid.testInstrumentationRunnerArguments.class=com.smartfarm.mobile.AccountPreferencesTest,com.smartfarm.mobile.AniGoSpriteAssetTest,com.smartfarm.mobile.AniGoSessionNavigationTest,com.smartfarm.mobile.CarabaoOnboardingPreviewTest,com.smartfarm.mobile.FullAppJourneyTest,com.smartfarm.mobile.PhotoAuthScreensTest,com.smartfarm.mobile.Phase6WelcomeScreenTest,com.smartfarm.mobile.FarmOnboardingScreenTest,com.smartfarm.mobile.WelcomeScreenTest
test_exit=$?

mkdir -p app/build/ui-screenshots
adb shell ls -l /sdcard/Pictures/AniGoOnboardingAudit || true
missing=0
for name in \
  00-welcome-light 00-welcome-dark 00-welcome-phone-light \
  01-farm-light 01-farm-phone-light 02-crop-light 03-work-light 04-harvest-light 04-harvest-phone-dark \
  01-farm-dark 02-crop-dark 03-work-dark 04-harvest-dark \
  01-farm-tablet-light 02-crop-tablet-light 03-work-tablet-light 04-harvest-tablet-light \
  01-farm-landscape-dark 02-crop-landscape-dark 03-work-landscape-dark 04-harvest-landscape-dark \
  intro-light intro-dark intro-landscape-dark-large-text intro-landscape-light-large-text farm-320dp-large-text \
  large-text-dark-1 large-text-dark-2 large-text-dark-3 large-text-dark-4
do
  destination="app/build/ui-screenshots/${name}.png"
  if ! adb pull \
    "/sdcard/Pictures/AniGoOnboardingAudit/${name}.png" "$destination"
  then
    echo "::error::Cannot export screenshot: ${name}"
    missing=1
  elif ! file --mime-type "$destination" | grep -q 'image/png'
  then
    echo "::error::Screenshot missing or not PNG: ${name}"
    missing=1
  fi
done

find app/build/ui-screenshots -type f -printf '%f %s bytes\n'
if [ "$test_exit" -ne 0 ]; then
  echo "::error::Android instrumentation suite failed"
  exit "$test_exit"
fi
if [ "$missing" -ne 0 ]; then
  echo "::error::At least one required visual-review capture is missing"
  exit 1
fi
echo "Android tests and all 30 PNG visual captures passed."
