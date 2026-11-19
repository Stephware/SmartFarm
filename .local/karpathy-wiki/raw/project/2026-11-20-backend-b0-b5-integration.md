# Backend B0–B5 integration on current dev

Source: feature/anigo-backend-b0-b5 after local rebase and verification
Collected: 2026-11-20
Published: 2026-11-20

Original execution began from fetched dev 6fbd0a07. During execution frontend PR #148 merged; the unpublished backend branch was rebased cleanly onto dev add44d95, preserving all frontend work. Backend-only diff contains no Views, wwwroot or mobile changes.

Rebased logical commits: B0 e0ae988d, B1 937d86c7, B2 bae7dc95, B3 38cfd66c, B4 6b5e73e9, B5 d3384b1a. Earlier raw evidence retains its original pre-rebase commit IDs and results as history.

Integrated Release strict disposable-SQL suite at d3384b1a: 649 passed, zero failed/skipped (147 API, 390 Auth, 21 schema, 37 weather, 54 Worker). Integrated Node suite: 100 passed, zero failed/skipped; the earlier 90-test result predates the frontend merge. Android debug build/JVM/instrumented-test compilation tasks remain successful; the unchanged Android source reused up-to-date output from the prior executed 78 JVM tests. Python schema tests 17 and backup-script tests 7 passed on unchanged verifier/script sources. Full Release solution build includes the seeder.

Scoped wiki evidence verification with installed Python 3.10 found zero fidelity suspects and zero evidence errors. It also reported the pre-existing navigation-ux raw capture outside the four selected article references; that unrelated backlog was left untouched. Changed Markdown relative links and whitespace checks passed.

Local populated SmartFarmDB remains unchanged due to 11 schema differences, with no backup or seed there. Separate disposable backup/checksum/VERIFYONLY, 40-record demo seed, repeat preservation and live role login/isolation passed. SMTP and Gemini actual providers remain unconfigured/unverified. No frontend presentation, physical-device/emulator execution, production Worker/deployment, destructive migration, full restore rehearsal or merge into dev is claimed.

Delivery: feature branch is prepared for a Draft PR to dev. CI and PR status must be checked remotely after push; local test success does not assert CI success or release readiness.
