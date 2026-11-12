# Android setup and architecture

Reviewed against `dev` at `ac624be` on 2026-11-11. Implementation status below is source-backed; it is not a fresh runtime acceptance result.

Open `mobile/SmartFarm.Android`, not repository root, in Android Studio. Install
Android SDK 36/Build Tools 36.0.0 and a compatible Gradle JDK; tracked source and
CI target Java 17 bytecode. Keep machine-specific `local.properties` ignored.

```sh
cd mobile/SmartFarm.Android
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
# Authorized emulator/device needed:
bash ./gradlew :app:connectedDebugAndroidTest
```

Build manifests pin Gradle 9.3.1, AGP 9.1.1, Kotlin Compose plugin 2.2.10, KSP 2.3.6,
Room 2.8.5. Minimum API is 26, compile/target 36. CI builds debug/device-test APKs and
runs JVM tests; it does not execute instrumentation. Do not treat local untracked
JVM configuration as a shared build requirement.

## Runtime wiring and networking

`AppContainer` manually composes Retrofit/OkHttp, repositories, Room/cache, sync
and WorkManager. Compose/StateFlow screens cover farm home, plots/members/settings,
crops, tasks, monitoring/photos, stock, harvests, weather, reports/finance insights,
notifications/account and sync review. AniGo is display branding; package remains
`com.smartfarm.mobile`.

Debug defaults to `http://10.0.2.2:5081/`; override via
`-PsmartfarmDebugApiUrl=...`. Release requires
`-PsmartfarmReleaseApiUrl=https://your-api.example/`. API factory requires trailing
slash, no URL credentials/query/fragment, HTTPS except explicitly allowed local
debug hosts. Main manifest forbids cleartext; debug security permits local hosts.
Never put secrets or API credentials in the APK.

## Local persistence and limitations

Room schema 4 has metadata, cached responses, pending operations/photos with
explicit migrations. Exported schemas 1 and 4 are tracked. API-first cached reads
fall back only on connection failure, scoped per signed-in user; enrollment,
tasks/notifications and reports are live. Supported queued field writes include
combined activity/stock and dependent photos. Setup/planning/follow-up/void/reversal
writes remain online. See [Android/offline contract](../.local/karpathy-wiki/wiki/project/android.md).

Tokens are memory-only; process restart requires sign-in. Refresh/logout/account
switch uses generation/mutex guards. No secure token persistence, offline login,
push transport or device weather ingestion exists. Sync does not yet refresh all
open confirmed data/cache after upload; test displayed state separately.

Use the [frontend conventions](../.local/karpathy-wiki/wiki/project/frontend.md)
and applicable Impeccable instructions for future UI work; this guide does not
redesign the existing Pine and Gold Compose implementation.
