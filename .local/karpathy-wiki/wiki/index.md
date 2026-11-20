# Knowledge Base Index

> **Current planned direction (2026-11-13):** [Farmer-first MVP simplification](project/mvp-simplification.md). Stages 1–5 navigation, recording, offline confirmation and presentation retirement are implemented and locally verified; Stage 6 technical checks recorded with provider/release limits; real-farmer sessions waived/not conducted. Check the plan for merge status. Read [PRODUCT.md](../../../PRODUCT.md) and [the execution plan](../../../docs/superpowers/plans/2026-11-13-mvp-simplification.md) for Codex work. Prior source pages document the existing implementation, not the desired next UX.

SmartFarm source-backed development knowledge: full audit at `ac624be`, with the
reference frontend verification at `ae06ab2` and source publication checks through
`dev` at `3c5a8b0`. Start with [current source status](project/current-source-status.md).
Read the smallest relevant page set, then affected source. **Implemented** means
wired in code; **planned/incomplete** means absent or partial; **runtime-verified**
requires a dated result for the stated revision/environment. Historical results
are not acceptance of the current tree. Documentation is Git-visible; use
`rg --hidden --no-ignore` for explicit hidden-directory searches.

## project

Architecture, behavior, conventions, decisions and dated implementation evidence.

| Article | Summary | Updated |
| --- | --- | --- |
| [MVP simplification](project/mvp-simplification.md) | Stage 6 technical evidence; external PR merge and test-only CI follow-up with explicit approval gate | 2026-11-14 |
| [Current source status](project/current-source-status.md) | Publication delta: admin/MFA, Android navigation, Tailwind, CI and extended-schema verifier limits | 2026-11-14 |
| [Architecture](project/architecture.md) | Independent hosts, service boundaries, live farm access and private media | 2026-11-12 |
| [Features](project/features.md) | Implemented modules, roles and critical business rules | 2026-11-14 |
| [Database](project/database.md) | SQL-first history, fresh schema match and approved production restore evidence | 2026-11-20 |
| [Android and offline sync](project/android.md) | Navigation/dirty-draft guards, Room/cache, scoped confirmations, receipts and session limits | 2026-11-19 |
| [Tasks, weather, and automation](project/automation.md) | Saved-weather categories, current-access Ani alerts, optional guidance and retained Worker rules | 2026-11-20 |
| [Dependencies and development](project/development.md) | B0–B5 checks and newly verified production deployment/provider evidence | 2026-11-20 |
| [Development decisions](project/decisions.md) | Implemented constraints, inferred consequences and integrated agent workflow | 2026-11-12 |
| [Known issues and incomplete work](project/known-issues.md) | Current gaps versus resolved/historical failures and runtime unknowns | 2026-11-12 |
| [Current progress](project/progress.md) | Confirmed PR61–78 provenance, Phases A–D, MVP stage status and dated results | 2026-11-14 |
| [Functional verification](project/functional-verification.md) | Historical role/workflow runtime record, with current interpretation | 2026-11-11 |
| [PR48 review](project/pr48-review.md) | Historical date-boundary fix/merge and scoped runtime evidence | 2026-11-11 |
| [Frontend](project/frontend.md) | Phase 3–7 layouts/forms, charcoal themes, native dialog appearance, reviewed baselines and verification limits | 2026-11-16 |
| [API contracts](project/api-contracts.md) | Backend pages/time/language, alerts, recovery and optional guidance; legacy sync compatibility | 2026-11-20 |
| [Finance and reports](project/finance-reports.md) | Manual cash, unit/cycle-safe reports, platform parity and secure exports | 2026-11-14 |

Maintenance/provenance: [runbook](../README.md), [source manifest](../source-manifest.json),
[append-only operation log](log.md). Agent workflow: [AGENTS.md](../../../AGENTS.md),
[Superpowers guide](../../../docs/superpowers/README.md).
