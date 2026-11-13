# Android and offline synchronization

> Sources: SmartFarm Android and API source review
> Raw: [Android sources](../../raw/project/2026-10-27-android-source.md); [Feature sources](../../raw/project/2026-10-27-features-source.md); [Functional E2E evidence](../../raw/project/2026-10-27-functional-e2e.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Current Android sources](../../raw/project/2026-11-11-android-current.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

> **New planned mobile UX:** Today / Record / My Farm, with sync only as automatic status plus exception recovery. Keep Room/WorkManager and all safe conflict/photo/session handling. See [MVP plan](mvp-simplification.md). The five-tab navigation described below is **currently implemented**, not the target.

## Display name

The app displays AniGo; application/namespace remains `com.smartfarm.mobile`.
The reference reconstruction in PR #86 follows the earlier workspace overhaul.
Light cards, inset-aware headers, reusable crop/plot components and compact tabs/
expanded rail remain native Compose. Appearance is an explicit persisted boolean;
its default is light regardless of OS and it never stores credentials.
October runtime evidence is historical. The later reference revision passed
47 JVM tests and 67 Pixel 9 emulator tests, with real MainActivity light/dark
visual checks. See [frontend](frontend.md) for scoped evidence and limits.

## Implemented local flow

Compose screens consume StateFlow ViewModels. `AppContainer` manually wires
Retrofit, repositories, Room and WorkManager. Room schema is version 4 with explicit
migrations; exported schemas 1 and 4 are tracked. Tables hold local metadata,
user-scoped response snapshots, pending operations and pending photos.

Navigation includes Farm, Tasks, Record, Crops and More workspace actions,
monitoring/harvest details, sync review, notifications and a farm insights screen.
Farm detail has overview/weather/plots/members/settings. Tasks now include manager
create/edit, farm-local Today/overdue/upcoming/completed groups, presets and
active-crop logging shortcuts. No equivalent Android monthly crop agenda exists.

`OfflineReadCache` is API-first and falls back only on connection IOException to
an existing user/resource snapshot. HTTP auth/permission/not-found/validation
failures never become cached successes. Selected reads cover farms/plots/members,
crops, monitoring, inventory, harvests and weather. Enrollment codes/applicants,
tasks, notifications and insights require live requests; cached choices do not
authorize management writes.

Supported queue types: `activity`, `activity_stock`, `observation`, `inventory`,
`harvest`. Combined supply use is optional for fertilizer/pesticide activities;
correct category, positive precise quantity, item identity and rowversion travel
as one operation. Structured issues travel with observations. Photo rows/files
have a separate queue; observation and dependent photo publication is atomic in
Room. Local-save receipts and pending rows do not mean server-confirmed stock or
yield. Some direct Retrofit POST methods remain but current record forms use sync.

## Retry and conflict contract

- Stable operation IDs go to `api/sync/operations/{operationId}`. Server validates
  one exact payload, current farm access, actor/farm/type/hash, and saves business
  result plus ledger atomically. A successful replay does not repeat the write.
- WorkManager serializes uploads with connectivity/backoff. Receipt validation
  always checks operation ID/type. Observations additionally require a valid result
  ID; combined stock validates activity ID shape and matching inventory ID. Ordinary
  activity/inventory/harvest result targets and fields have no broader client check.
  Rejections move to review; transport/server failures retry; cancellation propagates.
- Inventory and combined-stock review fetch current stock, show expected balance
  and require explicit confirmation. Confirmation refetches unchanged rowversion,
  updates the reviewed payload under the same operation ID and returns it pending.
  No silent conflict rebase. A failed server transaction leaves no successful ledger.
- Photos optimize once into private files and retry identical bytes with stable-ID
  PUT. Server compares existing content; client checks returned photo/observation
  identities before deleting queue/file. Saved-photo replay may succeed after crop
  completion, while new recording still requires Active status.
- Discarding an observation removes dependent photo rows/files. DAO operations and
  private-file actions remain user-scoped.

## Boundaries and incomplete work

Current source still has no sync-completion invalidation of the read cache or
refresh of open inventory/monitoring/harvest loaded state. Pending rows disappear
but confirmed values may require reload. The October stale-stock symptom remains
historical runtime evidence with continuing source support; it was not reproduced
again in this audit. Fix display freshness without changing stock accounting.

This is selected offline support. Farm setup/enrollment, crop plans/transitions,
task/notification writes, issue follow-up, item management/reversal and harvest
voiding remain online. Reports are live read-only harvest/finance summaries with
farm-local date presets and separate error states. Mobile lacks the server's
cycle-total comparison DTO/view, CSV export and detailed finance entry editor.
Pending harvests are excluded from confirmed reports.

Tokens remain in memory. Refresh is serialized; a generation counter blocks
in-flight login/refresh/results from reviving a logged-out account. Logout clears
feature/photo presentation state. Room rows remain user-scoped and durable;
process restart requires sign-in before authenticated queue processing resumes.
Never add plaintext token persistence. No Android weather-ingestion worker,
push delivery or offline login is implemented.

Weather reads saved server data with separate cache timestamp and provider
retrieval time. Stale/missing forecasts and missing coordinates are distinct;
changing coordinates invalidates saved weather. Private images use bounded
memory caches; Glide disk caching is disabled.

## Source navigation

[Composition root](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/di/AppContainer.kt),
[sync](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncRepository.kt),
[receipt validation](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncReceiptValidator.kt),
[read cache](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/OfflineReadCache.kt),
[session safety](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/AuthRepository.kt),
[insights](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/insights),
[API ledger](../../../../src/SmartFarm.Api/Controllers/SyncOperationsController.cs).

## See Also

[Architecture](architecture.md), [Features](features.md), [Known issues](known-issues.md),
[Development](development.md), [Frontend](frontend.md), [API contracts](api-contracts.md).
