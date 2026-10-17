# Android foundation

Open `mobile/SmartFarm.Android` in Android Studio. Set `JAVA_HOME` to a compatible JDK (17–25) and `ANDROID_HOME` to your SDK, or let Android Studio create ignored `local.properties`. Install Android SDK 36 and Build Tools 36.0.0.

```sh
cd mobile/SmartFarm.Android
./gradlew assembleDebug testDebugUnitTest lintDebug
# With a running emulator or connected device:
./gradlew connectedDebugAndroidTest
```

The app supports Android 8.0+ (API 26), targets API 36, and opens a Material 3 welcome screen through Navigation Compose. Existing feature directories remain reserved for later work.

- Gradle 9.3.1, AGP 9.1.1, built-in Kotlin 2.2.10, matching Compose compiler, and KSP 2.3.6 are pinned. The wrapper JAR and distribution use official SHA-256 checksums.
- Room exports versioned schemas under `app/schemas`. Its only table is local technical key/value metadata; it has no business data or destructive migration fallback.
- `ApiClientFactory.create(baseUrl)` creates Retrofit/OkHttp only when a caller supplies an explicit HTTPS URL ending in `/`. No endpoint, API key, request, or logging interceptor is configured. Never put secrets in the APK; authentication and API contracts come later.
- `SmartFarmApplication` supplies WorkManager configuration; `AppContainer` exposes it on demand. No workers or synchronization schedules are registered.

Reference: [AGP compatibility](https://developer.android.com/build/releases/agp-9-1-0-release-notes), [Room schema exports](https://developer.android.com/jetpack/androidx/releases/room), [Gradle checksums](https://gradle.org/release-checksums/).
