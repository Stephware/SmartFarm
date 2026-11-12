# SmartFarm engineering workflow

Baseline review at `ac624be`; publication source check at `3c5a8b0` on 2026-11-12.
See [current source status](../../.local/karpathy-wiki/wiki/project/current-source-status.md) for later source deltas. This is project guidance for the
installed skills, not a replacement for their instructions or an application plan.
Start with [AGENTS.md](../../AGENTS.md) and the [knowledge index](../../.local/karpathy-wiki/wiki/index.md).

## One complementary workflow

**Karpathy → Superpowers → Ponytail → Impeccable (for UI/UX) → Testing → Knowledge Update**

Use each tool for its responsibility. Read its installed `SKILL.md` when applicable;
do not run every process for every task or duplicate a plan/checklist across tools.
User scope and authorization govern actions, including examples that mention commits.

| Tool | Responsibility in SmartFarm |
| --- | --- |
| Karpathy Wiki | Read relevant knowledge, verify affected source, preserve decisions/history; capture sanitized evidence and update affected pages after significant completed changes. |
| Superpowers | Apply relevant planning, debugging, TDD, implementation, review and verification skills at their actual triggers. |
| Ponytail | Smallest complete maintainable solution; reuse house patterns, then built-ins/installed dependencies. YAGNI; DRY where it improves clarity. Keep validation/security/accessibility/error handling/correctness and essential tests. |
| Impeccable | Applicable Web/Compose design/audit/refinement workflow; accessible responsive farmer-friendly interfaces, incumbent tokens/components, rendered verification and native guidance. |

Installed names include `karpathy-llm-wiki`, `superpowers:using-superpowers`,
`superpowers:brainstorming`, `superpowers:writing-plans`,
`superpowers:systematic-debugging`, `superpowers:test-driven-development`,
`superpowers:executing-plans`, `superpowers:subagent-driven-development`,
`superpowers:requesting-code-review`, `superpowers:verification-before-completion`,
`impeccable`, and `ponytail:ponytail`. Use the session's discovered installation
paths; do not hardcode a plugin cache version into a new project dependency.

## Before changing behavior

Read index/relevant pages, inspect Git status/branch and affected source/callers.
Separate observed behavior from plans and guesses. Keep unrelated work intact.
Apply brainstorming for new creative behavior, then writing-plans when the scope
needs a plan. Follow each skill's approval/handoff rules under the user's current
instructions; do not infer new authorization from this guide. For authorized
execution use the applicable inline or subagent workflow and its workspace/ledger.
Use worktree isolation where required; do not casually switch the user's checkout.

Specs belong in `specs/`; plans in `plans/`, named by date and intent. Describe
exact files/interfaces, business constraints, meaningful tests and review risks.
Record changes in scope and distinguish planned, implemented and runtime-verified.
The dated October documents here are historical evidence, not standing orders to
repeat their tasks, create branches or merge anything.

## Implementation standards

- Core owns provider-independent entities/contracts/interfaces/pure rules;
  Infrastructure owns persistence and shared services; Web and API call those
  services independently. Keep services authoritative for live farm access and
  validation. Use existing repositories/unit of work or established direct
  DbContext patterns as appropriate; avoid introducing a generic abstraction.
- Keep numeric SQL/JSON codes and cached/offline contracts compatible. Update
  affected Core contracts, controllers, Android DTOs/callers and tests together.
- Preserve SQL-first schema, explicit rowversion handling, audit ledgers and
  transaction boundaries. Never run Migrate/EnsureCreated against existing data
  or silently adopt migration history. Database work needs separately scoped
  review/authorization; see [schema guide](../../.local/karpathy-wiki/wiki/project/database.md).
- Account creation grants no membership; Owner controls enrollment/members.
  Preserve active-crop rules, stock nonnegativity, atomic fieldwork/stock rollback,
  idempotent sync and explicit conflict review. Harvest is separate from crop
  completion; quantities/units are separate from manually recorded PHP cash.
- Keep photos private and tokens out of plaintext. Reuse CSV escaping/formula
  protection, antiforgery, cancellation propagation and sanitized errors.
- Prefer focused complete changes, existing components and minimal dependencies.
  Do not remove edge-case handling or meaningful tests to shorten code.

## Debugging and testing

Use systematic-debugging before a fix: reproduce, inspect errors/recent changes,
trace the failing boundary, test one evidence-based hypothesis and fix the root
cause. Relevant boundaries include Web→service→SQL, Android→API→service→SQL,
Room→WorkManager→sync ledger, and Worker→provider/jobs/application storage.
A stale display after sync is not proof of incorrect stock accounting.

Use installed TDD for production behavior changes: a meaningful failing regression,
minimal implementation, green check, then safe refactor. Pure prose changes use
source/link/consistency/diff validation; do not add mirrored tests or alter
application behavior just to test documentation.

Select checks from [development](../../.local/karpathy-wiki/wiki/project/development.md).
A normal green backend suite can omit SQL work; set disposable SQL inputs explicitly.
CI compiles Android instrumentation but does not execute it. Run real device/UI
checks for changed flows when available, and state unavailable checks precisely.
Never use populated SmartFarmDB for write tests. Backup scripts do not replace a
disposable recovery rehearsal.

## Web and Android interface work

Apply Impeccable only to relevant UI work. Read its installed skill and run context
once; follow the selected reference and native guidance. Current UI is Razor/CSS/JS with self-hosted prefixed Tailwind utilities
and Compose/Material3, Harvest Gold + Pine, AniGo. Reuse current components/assets.
There is no PRODUCT.md or DESIGN.md yet; follow the installed prerequisites for a
new surface/redesign, while narrow refinements use incumbent source as permitted.
This document does not authorize a redesign or product-requirements invention.

Verify rendered narrow/wide layouts, keyboard and large fonts, focus/labels/contrast,
role gates, loading/empty/error/offline/stale/pending states, and destructive-action
cancel paths. Native Android checks need device/emulator semantics and rendering;
web-only detector/overlay tools do not validate Compose.

## Review, verification and completion

Use applicable code-review skill for implemented changes. Supply focused scope,
requirements, exact changed range and evidence; independent review does not replace
checks. Fix material findings within scope, and explain unresolved limitations.
Before claiming completion, run the relevant commands, read their complete outcome
and separate test existence from execution. Record command, revision, environment,
result and skipped coverage. Do not reuse a prior run's count as current success.

Follow feature/fix branch → `dev` → reviewed `main` workflow when Git integration
is authorized. Remote protections/CI status must be inspected, not assumed from
README or local refs. Never commit/push/merge/create PRs just because a skill's
example says so. Inspect explicit paths and diffs; never force-add private data.

After significant completed changes, add immutable sanitized evidence, update only
affected current wiki sections/cross-references/index dates, append the log and
manifest provenance. Keep plans/history dated. Wiki, this directory and AGENTS.md
are shareable Git-visible documentation; private verification artifacts remain
ignored. The former blanket docs/wiki ignore policy is superseded.

## Historical records

- [UI improvement plan](plans/2026-10-26-ui-improvements.md): earlier form/safety/draft/navigation work; old failures and palette are not current status.
- [Enrollment design](specs/2026-10-27-farmer-registration-approval-design.md) and [plan](plans/2026-10-27-farmer-registration-approval.md): original approval decision and dated verification; latest signup/rotation behavior supersedes parts of scope.
- [Current progress](../../.local/karpathy-wiki/wiki/project/progress.md): PR61–78, Phases A–D and subsequent frontend provenance.
