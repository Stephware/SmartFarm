# MVP Stage 5 public baseline review

> Source: PR #128 CI job logs, current committed source/manifest and private same-browser base/candidate public renders
> Collected: 2026-11-14 (Asia/Manila)
> Published: 2026-11-14

The initial reviewed-baseline job and its fresh rerun stopped at the source-fingerprint gate before pixel comparison. The shared layout changed for authenticated navigation, but its reviewed manifest was not refreshed. Direct hashing of the checked-in layout now supplies the manifest fingerprint; explicit public-page review provenance is appended. A read-only CI guard prints the checked-out revision/manifest checksum and rejects setup-time changes to that tracked metadata before running the unchanged visual gate. Initial interpretation of the log hash direction was incorrect; the direct source bytes are authoritative.

A private export of dev 9c9adce and the Stage 5 candidate rendered public landing, login and registration in the same installed Chrome browser. All 36 pairs at 320, 375, 390, 768, 1024 and 1440 pixels in light/dark had exactly zero changed pixels, including antialiasing. The exported base used an unreachable database endpoint and only public GET requests. Its temporary host was stopped by exact PID/command guard. All existing Linux golden PNGs and the pixel threshold remain unchanged. A cross-platform Chrome-to-Linux comparison had rasterizer differences and is not evidence of a candidate regression or a passing Linux comparison.

No application, API, database, Android or business behavior changed in this follow-up. The first PR commit's other six CI checks passed; Android CI completed 118 tests with zero skipped/failed. Final remote checks on the updated head remain gated in PR #128. No merge with a failing check is authorized.
