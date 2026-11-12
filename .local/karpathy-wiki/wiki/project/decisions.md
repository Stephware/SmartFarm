# Development decisions and constraints

> Sources: SmartFarm source comments, implemented rules, and repository workflow
> Raw: [Architecture sources](../../raw/project/2026-10-27-architecture-source.md); [Database sources](../../raw/project/2026-10-27-database-source.md); [Feature sources](../../raw/project/2026-10-27-features-source.md); [Android sources](../../raw/project/2026-10-27-android-source.md); [Progress sources](../../raw/project/2026-10-27-progress-source.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

## Explicit decisions present in the repository

| Decision | Evidence and implication |
| --- | --- |
| Independent MVC/API share services | Host registration and `DependencyInjection.cs`; keep validation and farm permission checks in shared services. |
| SQL-first schema with explicit rollout | Empty baseline, hand mappings, SQL upgrades and additive migrations; no startup schema/history writes. Finance and base CREATE differ. |
| Live per-farm authority | `FarmAccessService`; global Identity roles/local cache do not grant farm rights. |
| Account and membership separate | Latest simple registration plus signed-in requests; owner approval creates Field Worker. Rotation/cancellation preserve request history. |
| Audit history | Archive, reversal and void instead of destructive record edits; harvesting does not complete crops. |
| Global crop catalog | Creation allowed; rename/deactivation omitted to avoid affecting another farm. |
| Atomic retryable field records | Sync ledger validates identity/payload and commits result with mutation; combined supply use rolls back activity/task effects on stock failure. |
| Explicit stock conflict review | Client refetches preview/rowversion and requires confirmation; no silent rebase. |
| Tokens remain memory-only | Generation/mutex guards prevent stale sessions returning; secure persistence remains future work. |
| Private photos | Authorized access, stable-ID identical-byte replay, private durable shared filesystem for Web/API. |
| Separate physical quantities and cash | Harvest groups separate units/cycles; finance is manually recorded PHP cash, not profit/sales estimates. |
| Bounded UI is not bounded totals | Finance aggregates all matching rows beyond history cap; CSV exports preserve filters/units and formula safety. |

## Shared scheduler and photo deployment constraints

Worker and Web agenda share boundary-safe date scheduling. Production Web/API
require explicit private absolute photo paths; operators must provide shared
storage and protected Data Protection keys. Recovery includes database, file bytes
and keys; scripts do not establish a successful restore rehearsal.

## Architectural consequences inferred from code

These are inferences from current implementation, not retrospective ADR approvals:

- Shared persistence/contracts couple all hosts and Android clients; inspect their
  callers, cached DTOs and offline replay when changing a contract.
- Tasks cross crop transitions, monitoring, linked stock and harvest; check
  transaction boundaries and reminder dismissal together.
- Additional offline actions need a server idempotency/conflict contract. Room
  tables alone do not supply retries, authorization or accounting correctness.
- Current plot-area report estimates are not historical normalized yield. Charts
  may be bounded; totals and exports must retain their full authorized scope.

## Existing workflow

Use focused feature/fix branches targeting `dev`, then reviewed `dev`→`main`
promotion. README protection guidance is intended workflow, not verified remote
settings. Preserve unrelated work; review diff and meaningful checks before review.
Do not infer permission to commit/push/merge from a skill's example commands.

> **Status: Outdated** (2026-11-11)
> Earlier local-only documentation policy is superseded by the user's explicit
> synchronization instruction. Wiki, runbook/manifest, Superpowers docs and
> AGENTS.md are now Git-visible and must not be re-ignored. Historical runtime
> artifacts, secrets and generated output remain excluded. This task authorizes
> no commit, push, merge, PR, application or database change.

Future agent workflow: Karpathy → applicable Superpowers → Ponytail → Impeccable
when UI work applies → meaningful tests → knowledge update. Follow the installed
skills, not a copied substitute; see [agent guide](../../../../AGENTS.md) and
[workflow](../../../../docs/superpowers/README.md).

## See Also

[Architecture](architecture.md), [Database](database.md), [Features](features.md),
[Android](android.md), [Finance and reports](finance-reports.md),
[Progress](progress.md), [Known issues](known-issues.md).
