# SmartFarm architecture

> Sources: SmartFarm repository, local source review
> Raw: [Architecture sources](../../raw/project/2026-10-27-architecture-source.md); [Feature sources](../../raw/project/2026-10-27-features-source.md); [Android sources](../../raw/project/2026-10-27-android-source.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [backend source review](../../raw/project/2026-11-11-backend-current.md); [web-workflow source review](../../raw/project/2026-11-11-web-workflow-current.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

## Structure and request flow

Reviewed `dev` at `ac624be`. AniGo is the display brand; SmartFarm namespaces,
application identifiers and route contracts remain. “Implemented” means wired in
source, not deployed or freshly runtime-tested. See [progress](progress.md).

| Area | Responsibility | Entry points |
| --- | --- | --- |
| Core | Plain entities, contracts, interfaces, enums and pure rules | `src/SmartFarm.Core/` |
| Infrastructure | Shared business services, repositories/unit of work, EF/Identity, weather and private files | `DependencyInjection.cs`, `Persistence/SmartFarmDbContext.cs` |
| Web | Razor MVC, cookie forms, farm workspace, charts and CSV | `src/SmartFarm.Web/Program.cs` |
| Api | Opaque bearer JSON endpoints and transactional sync ledger | `src/SmartFarm.Api/Program.cs` |
| Worker | Headless Hangfire heartbeat, tasks/reminders and weather processing | `src/SmartFarm.Worker/Program.cs` |
| Android | Compose → StateFlow ViewModels → repositories → Retrofit; selected Room/WorkManager offline operations | `di/AppContainer.kt`, `navigation/SmartFarmNavHost.kt` |

The solution includes all backend hosts and active backend test projects; Android
is a separate Gradle build. Core has no Infrastructure dependency. Infrastructure
references Core; hosts use both. MVC and API call the same business services and
SQL Server directly. Web does not require API host availability for its business
pages. Legacy `Web/Clients` HTTP wrappers and their tests remain; CSV helpers in
that directory are actively used. Do not extend legacy wrappers merely because
they exist. Service validation and authorization must stay shared.

Repositories and `IUnitOfWork` coordinate common mutations. Reports, finance,
automation and the API sync ledger also use DbContext directly; do not describe
repository-per-entity as an absolute requirement.

## Identity and farm boundaries

Web accepts Identity cookies; API farm requests require the opaque Identity access bearer
token, not a JWT. The refresh token is used only by the refresh endpoint. Both use `SmartFarmDbContext`. Data Protection uses
an environment-specific shared application name; separate deployments need the
same protected key ring. Startup does not create schema or apply EF migrations.

`FarmAccessService` checks active users, unarchived farms and live ownership or
membership. Owner is `Farm.OwnerUserId`; membership role codes identify Manager
and Field Worker. Global Identity claims and client UI visibility grant no farm
authority. Inaccessible farm records normally map to not found.

Registration now creates an account using name, email and password. Joining is a
separate signed-in My farms action. Owner approval grants Field Worker, while
promotion is separate. Cancellation, code rotation, request history and in-app
enrollment notifications are implemented. See [API contracts](api-contracts.md).

## Shared private photo storage

Web and API use `AddSharedObservationPhotoStore`. Development has the shared
Web/App_Data fallback. Outside Development, an absolute private storage path
outside `wwwroot`, and not a filesystem root, is required. Both hosts must mount
the same durable volume; matching strings do not create shared storage or migrate
old photos. Retrieval is authorized and private. Backup includes SQL metadata,
photo bytes and protected key material separately; see [recovery](../../../../database/RECOVERY.md).

## Source navigation

[Solution](../../../../SmartFarm.slnx),
[shared services](../../../../src/SmartFarm.Infrastructure/DependencyInjection.cs),
[Web startup](../../../../src/SmartFarm.Web/Program.cs),
[API startup](../../../../src/SmartFarm.Api/Program.cs),
[farm access](../../../../src/SmartFarm.Infrastructure/Security/FarmAccessService.cs),
[MVC isolation tests](../../../../tests/SmartFarm.Auth.Tests/MvcIndependentHostTests.cs).

## See Also

[Features](features.md), [Database](database.md), [Android](android.md),
[Frontend](frontend.md), [API contracts](api-contracts.md), [Decisions](decisions.md),
[Automation](automation.md).
