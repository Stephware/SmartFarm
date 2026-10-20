# Reference repository alignment — source review

Reviewed SmartFarm's feature structure and the eight previously supplied GitHub
repositories. This is a **code-structure comparison**, not an executed build or
proof that every existing feature works end to end.

| User-supplied repository | Verified pattern | Current SmartFarm alignment |
| --- | --- | --- |
| [KurtSarmiento/AspNetCoreMultiApp](https://github.com/KurtSarmiento/AspNetCoreMultiApp) | Separate MVC Web, API, Worker; service/repository and DTO examples | **Conceptual match:** Core/Infrastructure/Web/Worker layers, DTOs, and controllers. **Difference:** SmartFarm hosts MVC and API together in `SmartFarm.Web`, rather than separate web/API hosts; EF Core is used directly inside Infrastructure services, rather than a repository class for each entity. |
| [Stephware/VeterinaryClinicSystem](https://github.com/Stephware/VeterinaryClinicSystem) | Razor MVC controllers, Views, account workflows, ViewModels and service boundaries | **Aligned:** SmartFarm's MVC Accounts/Farms/Crops/Inventory/Harvest pages follow the same broad pattern; simpler UI by design. |
| [Stephware/MyWebApi](https://github.com/Stephware/MyWebApi) | API controllers, DTOs, service contracts | **Aligned:** farm-scoped API controllers call service interfaces and use input/output contracts. Paging is not generalized across all endpoints. |
| [Stephware/RoomDatabaseDiscussion](https://github.com/Stephware/RoomDatabaseDiscussion) | Room entities/DAO/database/repository and StateFlow ViewModels | **Partial:** SmartFarm has Room database and DAO foundation plus repositories and StateFlow ViewModels; live farm operations are currently API-only. Offline persistence/sync is deferred. |
| [Stephware/ComposeUIEssentials](https://github.com/Stephware/ComposeUIEssentials) | Jetpack Compose screens and `NavHost` routing | **Aligned:** SmartFarm uses Compose screens, Navigation Compose, and Material 3. |
| [Stephware/MobdevMidtermExam](https://github.com/Stephware/MobdevMidtermExam) | Kotlin feature screens and Compose forms | **Aligned at UI-pattern level:** functional login and separate feature screens, adapted to SmartFarm. |
| [Stephware/WeatherConsoleApplication](https://github.com/Stephware/WeatherConsoleApplication) | Weather client/service abstraction, typed DTO/options | **Foundation only:** SmartFarm has an isolated weather integration structure; real farm forecast/advisory workflows are not verified as implemented. |
| [Stephware/HangfireConsoleApp](https://github.com/Stephware/HangfireConsoleApp) | Background job/service separation | **Foundation only:** SmartFarm has a Worker/Hangfire setup, but scheduled business jobs, automation and notifications remain deferred. |

## Smoke test design

The web test uses the existing Razor Home view and a tiny anonymous
`GET /api/status` controller. The Android test uses the existing
Retrofit client and a small `StatusRepository` + `StatusViewModel`
(StateFlow), displayed in the existing Compose Welcome screen. It adds
no new UI framework, CSS library, schema object, authentication policy,
background job, or local synchronization behavior.

## Limits and next verification

The repository inspection confirms **structural alignment** with the
references, not identical architecture or guaranteed runtime correctness.
`dotnet build`, `dotnet test`, `./gradlew assembleDebug`, Android device
execution, and SQL Server integration must be checked locally.

Git integration note: PR #14 consolidates the missing later feature work
into `dev`, pending .NET/Android/SQL verification.

