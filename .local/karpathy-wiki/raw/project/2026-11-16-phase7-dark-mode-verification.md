# Phase 7 dark-mode verification

> Source: PR #138 source and local build, browser, emulator and rendered-image evidence
> Collected: 2026-11-16
> Published: 2026-11-16

Scope: feature/anigo-phase7-charcoal-dark-mode, initial head 1ba20822ea34ec7b6c517aae1d290aad8299c07d plus the source hashes in the appended manifest review. No merge authorized. Exact final commit CI belongs to PR #138 and is not inferred from local checks.

MVC and Compose use charcoal #121917, surfaces #202b26, pine navigation #19382c and harvest gold #d4ad61. MVC white-text action green is #397a58 (hover #3e835e), with mint links #8fcfa5. Light palette and Phase 6 composition remain. Narrow photo overlays, accessible CTA fallback #21834e and enlarged-text wrapping correct inherited accessibility defects in both modes.

The framework DatePickerDialog did not inherit Compose's dark choice: a new actual-dialog regression observed a white background. DateInput now selects an explicit dark native style only in app dark mode; light uses the existing default theme (resource ID 0). The real confirmation button returns the ISO date. Welcome's fixed-height photo box clipped the description at 320dp/160% text in both modes; content-driven minimum height fixes it. Three reproduced failures became four passing focused tests, including actual Account appearance switching/persistence.

Local verification: Release .NET build and 600 reported tests; Android debug/release/device builds, 65 JVM tests and all 157 emulator tests; 79 Node tests; seven browser scripts including 806 authenticated Razor renders and 30 public phase-theme-width variants. Automated accessibility, theme draft/persistence and 200% text reflow checks passed. Seven recovery and 15 schema-verifier Python tests passed. Full emulator journey uses the explicit test-server API URL.

All 30 real MVC baselines were generated in pinned Playwright 1.56.1 Linux Chromium, inspected by Codex via review sheets and full-size representatives, and compared at 0.000% afterward. Fifteen dark PNGs and five light landing contrast-only PNGs changed; ten light auth PNGs remain byte-identical and all dimensions match. No thresholds, skipped tests or fake application metrics were introduced. Actual native dialog and representative dark native/role workspace screenshots were inspected.

Limits: no disposable live SQL execution settings, physical devices/TalkBack, real-farmer sessions, production providers or deployment acceptance. Synthetic browser fixtures and MockWebServer tests do not certify those environments. Source/tests and final exact-head CI remain authoritative. Earlier Phase 4/5 statements reserving Phase 6/7 describe their historical scope, not this implementation status.
