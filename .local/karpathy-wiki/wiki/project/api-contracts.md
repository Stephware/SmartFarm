# API contracts and compatibility

> Sources: SmartFarm repository, source review
> Raw: [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

## Boundaries and responses

`SmartFarm.Api` is the Android/JSON host; Web business pages independently call
shared services. Contract authority is `Core/Contracts`, API models/controllers
and Android Retrofit DTOs/callers. No OpenAPI/Swagger generator is configured.
Opaque Identity bearer protects farm APIs; browser cookies do not substitute.

Typical outcomes: invalid model/domain input → Bad Request; inaccessible farm or
resource → Not Found; stale rowversion or conflicting action → Conflict; missing
bearer authentication → Unauthorized. Rate-limited actions return Too Many Requests
with Retry-After when available. DB failures are sanitized Service Unavailable.
Retired invitation routes return Gone and cannot create membership.

Existing numeric SQL/JSON codes remain stable. Selected converters also accept
symbolic names, and DTOs expose readable name fields. This is not universal string
enum serialization: inspect each property, especially numeric issue fields.
Use ISO farm dates, UTC event timestamps, GUID IDs and Base64 SQL rowversions.
Do not confuse membership Role with the different internal access-level enum.

## Current route map

Routes below are rooted at `/api`; farm business routes require bearer plus
operation-level shared service checks.

| Area | Routes / contract |
| --- | --- |
| Liveness/reference | `GET /status`, `GET /reference-data`; Development `/health/database` is separate and not full readiness |
| Account | `POST /auth/register` name/email/password, optional matching confirmation; login/refresh; `GET /auth/me`; no server logout/revocation |
| Farm/plot/member | `/farms`, `/{farmId}`, nested plots/members and `/access`; update/archive use version inputs |
| Enrollment | `/farm-join-requests` mine/submit `{farmCode}`; farm `/join-requests` owner overview; request `/decision` `{approve}`, `/cancel`; farm `/join-code/regenerate` `{currentCode}` |
| Crops | Farm `/crop-types`, `/crop-cycles`; plot `/crop-cycles` planning; cycle `/transitions` |
| Monitoring | Cycle `/monitoring` history, `/activities`, `/observations`, observation `/follow-up`; observation `/photos` POST or stable-ID photo PUT; private `/photos/{photoId}` GET |
| Inventory/harvest | Farm `/inventory` item/history/transaction/reversal; cycle `/harvests` record/list/void |
| Work/notifications | Farm `/tasks` create/update/close; `/notifications` current-user list/read |
| Read models | Farm `/dashboard`, `/weather`, `/reports/harvests?from&to&plotId&cropTypeId`, `/finance`, `/finance/insights?from&to` |
| Sync | `POST /sync/operations/{operationId}` with exactly one type-matching payload; activity, activity_stock, observation, inventory or harvest |

This is navigation, not an exhaustive duplicate of DTO fields. Inspect controller
verb/route attributes and input types before changing callers. CSV downloads live
in authenticated MVC actions, not separate JSON API export endpoints.

## Transaction and retry rules

Sync rechecks access even on replay; actor/farm/type/hash must match a stored
operation. Business write and saved result share a transaction. Combined stock
has no separate direct API route: it is a sync operation calling the shared atomic
service. Photo idempotency uses a separate stable-ID PUT and byte comparison.
A raw direct record POST is not automatically idempotent because sync exists.
Keep additive report fields/defaults and old numeric codes compatible with cached
and offline clients. Never silently reinterpret a queued payload after a schema
or business-rule change.

## Source navigation

[Controllers](../../../../src/SmartFarm.Api/Controllers),
[API auth models](../../../../src/SmartFarm.Api/Models/Auth),
[Core contracts](../../../../src/SmartFarm.Core/Contracts),
[Android API consumers](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api).

## See Also

[Architecture](architecture.md), [Features](features.md), [Android](android.md),
[Database](database.md), [Finance and reports](finance-reports.md).
