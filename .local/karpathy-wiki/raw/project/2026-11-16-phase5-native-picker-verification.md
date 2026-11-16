# Phase 5 native picker instrumentation

> Source: PR #135 source, Android test artifacts and focused local verification
> Collected: 2026-11-16
> Published: 2026-11-16

Head 339fb20 failed only the native picker (149/150). Its logs show two shown,
leaked dialogs after accessibility lookup/reopen attempts. The diagnostic-only
c32a344 passed all 150 on Android 15 CI, with no skips; no failed-window snapshot
was produced. All seven checks passed at that revision. The exact intermittent
emulator trigger remains unconfirmed; a controlled asleep-device run failed
before Compose lookup, a different symptom. No production picker defect was found.

The revised test requires actual host window focus before Compose's injected tap,
uses Espresso's in-app dialog root for native year selection and confirmation,
and verifies no early callback plus the changed ISO value 2027-10-26. It opens
once, without selector fallbacks, reopen retries, timeout increases or new packages.
Failure-only screenshot/hierarchy/window/power evidence preserves the original
exception and attempts back cleanup. Production DateInput is unchanged.

The final focused class passed 5/5 on the local Android 17 emulator. Debug/release/
device builds and 65 JVM tests passed. Fresh .NET 600, Node 74, public smoke and
806 authenticated Razor renders passed against the same application source.
Local macOS/Linux PNG differences did not pass the unchanged Linux goldens;
the pinned Linux CI visual check did pass. Final revised-test full-suite/CI
acceptance must be read from PR #135's exact head, not inferred from these results.
SQL execution without disposable settings, physical devices/TalkBack, real farmers
and production providers remain unverified. No merge or Phase 6/7 work is authorized.
