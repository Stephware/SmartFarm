# Agent workflow and context refresh

> Source: user-confirmed workflow preference; current repository and installed skill instructions
> Collected: 2026-11-16
> Published: 2026-11-16

Scope: PR #134 on feature/anigo-phase4-data-states. The user explicitly requested
keeping Impeccable, Karpathy, Superpowers, AGENTS.md and Ponytail integration current.
The user chose code-first for future landing/showcase work; app/document work is
code-first in the installed Impeccable workflow regardless. UI Phase 6 landing work
remains reserved and this maintenance does not authorize a redesign or merge.

## Verified integration

Installed instructions reviewed: karpathy-llm-wiki; Superpowers using/execution,
systematic debugging and verification/finishing guidance; Ponytail 5.3.3 simplicity;
Impeccable 4.8.0 setup, native audit/polish, doctor and init/routing maintenance.
These are observed local versions, not claims about the latest upstream versions.
Project context and skill packages are separate: no global package was rewritten,
upgraded or installed as a side effect. Discover actual installed instructions per
session, load only relevant references and avoid duplicated plans or interviews.

AGENTS restores the complementary Karpathy → Superpowers → Ponytail → Impeccable
(UI/UX) → Testing → Knowledge Update workflow and requires freshness checks after
significant changes. The Superpowers guide corrects an obsolete planned-MVP label
and the false claim that CI only compiles Android instrumentation.

The existing approved PRODUCT record lacked the current Impeccable schema marker
and durable platform/user/context/evidence/principles fields. These were added from
already approved requirements, retaining all prior scope/invariants/decisions.
The platform is adaptive Web/native Android; no new target or capability is added.
`.impeccable/config.json` records the user's explicit buildPath=code preference.
Doctor now resolves platform=adaptive, ruleRegistryAvailable=true and findings=[].
No DESIGN.md or competing product record was invented, and no screenshots or
private runtime data were added to shared knowledge.

## Actual CI and test boundaries

verify-smartfarm.yml runs .NET/Node, Android build/JVM and device compilation,
Chromium and stylesheet/parity checks. phase-a-stability.yml checks backup/schema
scripts. android-ui-smoke.yml separately runs all instrumented tests and the full
farmer journey on an emulator for matching PRs. They do not supply disposable SQL,
physical-device, provider or deployment acceptance; inspect exact-head results.

MVC test hosts hit Linux's inotify instance limit while creating JSON configuration
providers. A real-host regression failed with reload enabled and passes with
project-scoped test.runsettings disabling unused live reload. No production config,
runner limit or assertion was weakened. Normal build and all 595 .NET tests passed.

At 22e70e4 all six non-device CI checks passed; the emulator ran 139 tests with 138
passing, no skips and one harvest visibility failure. Enrollment and the full app
journey passed. Harvest logcat showed asynchronous IME hide/show/layout transitions
around the immediate scroll/assertion. The fix waits for actual root IME visibility
before scrolling and physically tapping; visibility, enabled, exact saved quantity
and confirmed history assertions remain. Local enrollment/harvest/full app journey
checks passed: 12 tests. Final full emulator/remote results belong to PR #134;
this capture does not claim those pending results passed.

Source is authoritative; historical counts, this snapshot and workflow existence
are not future runtime acceptance. Keep physical/TalkBack/farmer/provider/SQL limits
explicit, preserve immutable evidence and update only affected current guidance.
