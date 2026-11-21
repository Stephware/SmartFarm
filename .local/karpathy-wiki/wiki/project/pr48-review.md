# PR48 review, fix, and merge

> Sources: SmartFarm PR48 source, independent review, and local runtime checks
> Raw: [PR48 review evidence](../../raw/project/2026-10-28-pr48-review.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [Current audit](../../raw/project/2026-11-11-documentation-audit.md)
> Updated: 2026-11-11


## Current interpretation

This is a historical verification record for the named October revision, not a
fresh acceptance run. Preserve its counts and limits. Current Android now has
farm home/report/finance insights and manager task forms; registration is simple
account signup followed by signed-in farm joining. The post-sync freshness and
memory-only session limits remain supported by current source. See
[Android](android.md), [progress](progress.md) and [known issues](known-issues.md).

> **Status: Outdated** (2026-11-11)
> Statements below about absent Android reports, then-current Git/branch state
> or keeping wiki/docs ignored describe historical context. Documentation is now
> Git-visible by explicit user instruction; old runtime results are not refreshed.


## Current result: merged and verified

[PR48](https://github.com/Stephware/SmartFarm/pull/48) merged into `dev` at
`de548ef108767bcadab47745d63e50fb43f3ea64`. Tested head was
`0418bddc09fceb435b0f77a0114f96c72f8804ff`; its tree matches merged `dev` exactly.
The shared `TaskRules.ScheduledTasks` watering loop uses day-number arithmetic,
converting only valid scheduled dates. This fixes both the agenda and background
worker boundary cases without changing accepted dates or schema.

Four new regressions failed before the fix and passed afterward. The fresh full
backend run passed **317** tests with all five live SQL cases configured. Fresh
Web testing passed **51** checks across **155** HTTP requests. A separate accepted
maximum-date activation now leaves the same crop page at **HTTP200** with the
calendar rendered, instead of the former **HTTP500**.

Unchanged Android files retain the earlier **18** JVM and **36** emulator passes;
production path validation and Web script files retain their earlier checks.
Android was not rerun after the C#-only fix. Independent fix review found no blocker.
Browser visual interaction, physical devices, release installation, production
volume durability and existing-photo migration remain unverified. The separate
stale Android stock display issue remains open.

Backend summary (`../../verification/2026-10-28-pr48-fix/backend-summary.json`; local-only artifact),
fixed boundary runtime (`../../verification/2026-10-28-pr48-fix/date-boundary-result.json`; local-only artifact),
fresh Web report (`../../verification/2026-10-28-pr48-fix/web-report.md`; local-only artifact),
merge/tree verification (`../../verification/2026-10-28-pr48-fix/merge.json`; local-only artifact).
Task-created hosts/database and keys were removed; the existing database was
preserved. Local `dev` matches `origin/dev`, tracked files are clean, and the wiki
stays local and ignored.

## Historical review before the fix

### Scope and decision

[PR48](https://github.com/Stephware/SmartFarm/pull/48) proposes a monthly crop
agenda, shared private photo-storage configuration, and AniGo display labels.
Reviewed head: `9a5165d3ca809f00b61eebec508c6dbb54556d6f`.
The PR remains **open/draft and unmerged** because a real crop-page crash was
reproduced. Current `dev` remains `53d4b389b1c14a4d09f1a38c502c54a8b83e89f7`;
these proposed features are not implemented on `dev` yet.

> **Status: Outdated** (2026-10-28)
> The boundary failure was fixed in shared scheduling and PR48 merged into dev.
> See the current result above and the linked fix/merge evidence.


### Original blocker: accepted planting date crashes the crop page

At [CropAgendaBuilder.cs:54](https://github.com/Stephware/SmartFarm/blob/9a5165d3ca809f00b61eebec508c6dbb54556d6f/src/SmartFarm.Web/ViewModels/Crops/CropAgendaBuilder.cs#L54),
the builder enumerates `ScheduledTasks` before excluding events outside the month.
An active crop planted on `9999-12-31`, with daily watering and no expected
harvest, causes the scheduler's initial `AddDays` to overflow.

Live reproduction on a separate synthetic farm:

1. Register/login; create farm, plot, crop type and planting plan through the API.
2. Authenticated Web crop page for ordinary October renders HTTP200.
3. Activate with `plantedDate=9999-12-31`, watering interval one and no expected
   harvest. The real API accepts it and persists Active status.
4. Reload the same crop page: HTTP500, `ArgumentOutOfRangeException`, with
   `CropAgendaBuilder` in the stack.

The existing scheduler has the date-boundary weakness, but this PR newly calls it
on every crop-page load. A supported API write can therefore break that farm's
crop page. Skip scheduling active crops planted after the displayed month before
enumerating the schedule, and add a regression test for the accepted boundary.
This fix was recommended, not applied during review.

> **Status: Outdated** (2026-10-28)
> The boundary failure was fixed in shared scheduling and PR48 merged into dev.
> See the current result above and the linked fix/merge evidence.


### Original checks that passed

| Check | Result |
|-------|--------|
| Full backend suite with disposable SQL connections | 313 passed; five live SQL cases ran |
| Android JVM suite | 18 passed |
| Full Android emulator suite | 36 passed, including updated welcome test |
| Actual Android labels | AniGo welcome/offline message and compiled release resources checked |
| Focused live Web/API runtime | 51 assertions across 155 requests passed |
| Agenda normal cases | Dates, month filtering/order, navigation, escaping, roles, invalid months and legacy timezone fallback passed |
| Shared photos | Valid PNG Web→API and API→Web roundtrips matched exactly; roles, headers and public-path privacy passed |
| Production startup | Ten checks: both hosts reject missing/relative/public/root paths and start with a private absolute path |
| Existing Web UI scripts | Two passed |

The new passing unit tests do not cover the reproduced planting-date boundary.
Normal-path passes do not make the PR ready to merge.

> **Status: Outdated** (2026-10-28)
> The boundary failure was fixed in shared scheduling and PR48 merged into dev.
> See the current result above and the linked fix/merge evidence.


### Original evidence and limits

Boundary reproduction (`../../verification/2026-10-28-pr48/date-boundary-result.json`; local-only artifact),
backend summary (`../../verification/2026-10-28-pr48/backend-summary.json`; local-only artifact),
Web report (`../../verification/2026-10-28-pr48/web-report.md`; local-only artifact),
Web assertions (`../../verification/2026-10-28-pr48/web-checks.json`; local-only artifact),
Android report (`../../verification/2026-10-28-pr48/android-report.md`; local-only artifact),
production path checks (`../../verification/2026-10-28-pr48/production-storage-results.json`; local-only artifact).

Web checks used real HTTP forms and services/SQL, not browser interaction.
Android ran on one emulator; release resources were built, not release-installed.
An actual durable production volume and existing-photo migration were not tested.
The README correctly requires the same durable private volume on both deployed
hosts; matching path strings alone are insufficient.

Only disposable test data was written. Test hosts/database and emulator were
cleaned up. Source was not edited; the checkout returned to clean `dev`. Reports
and wiki remain local/ignored, with credentials and signing keys excluded.

> **Status: Outdated** (2026-10-28)
> The boundary failure was fixed in shared scheduling and PR48 merged into dev.
> See the current result above and the linked fix/merge evidence.


## See Also

[Progress](progress.md), [Known issues](known-issues.md),
[Functional verification](functional-verification.md), [Development](development.md).
