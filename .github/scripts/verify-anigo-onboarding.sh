#!/usr/bin/env bash
# Phase 8: run the AniGo onboarding and adjacent auth/farm UI suites on a live emulator,
# then export actual Compose screenshots from the Android debug app.
# Invoke from mobile/SmartFarm.Android, after the emulator boots.
set -uo pipefail

bash ./gradlew --no-daemon :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.smartfarm.mobile.CarabaoOnboardingPreviewTest,com.smartfarm.mobile.PhotoAuthScreensTest,com.smartfarm.mobile.Phase6WelcomeScreenTest,com.smartfarm.mobile.FarmOnboardingScreenTest,com.smartfarm.mobile.WelcomeScreenTest
test_exit=$?

mkdir -p app/build/ui-screenshots
adb shell run-as com.smartfarm.mobile ls -l files/anigo-onboarding || true
missing=0
for name in \
  01-farm-light 02-crop-light 03-work-light 04-harvest-light \
  01-farm-dark 02-crop-dark 03-work-dark 04-harvest-dark \
  intro-light farm-320dp-large-text
do
  destination="app/build/ui-screenshots/${name}.png"
  if ! adb exec-out run-as com.smartfarm.mobile \
    cat "files/anigo-onboarding/${name}.png" > "$destination"
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
echo "Android tests and all 10 PNG visual captures passed."
