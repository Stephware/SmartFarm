# Reference repository alignment — source review

Historically reviewed SmartFarm's feature structure and the eight previously supplied GitHub
repositories. This is a **code-structure comparison**, not an executed build or
proof that every existing feature works end to end.

| User-supplied repository | Verified pattern | Current SmartFarm alignment |
| --- | --- | --- |
| [KurtSarmiento/AspNetCoreMultiApp](https://github.com/KurtSarmiento/AspNetCoreMultiApp) | Separate MVC Web, API, Worker; service/repository and DTO examples | **Conceptual match:** Core/Infrastructure/Web/Worker layers, DTOs, and controllers. **Current local implementation:** separate Web/Api/Worker hosts; shared services and scoped repositories/unit of work, with established direct DbContext use for reports/finance/automation/sync. |
| [Stephware/VeterinaryClinicSystem](https://github.com/Stephware/VeterinaryClinicSystem) | Razor MVC controllers, Views, account workflows, ViewModels and service boundaries | **Aligned:** SmartFarm's MVC Accounts/Farms/Crops/Inventory/Harvest pages follow the same broad pattern; simpler UI by design. |
| [Stephware/MyWebApi](https://github.com/Stephware/MyWebApi) | API controllers, DTOs, service contracts | **Aligned:** farm-scoped API controllers call service interfaces and use input/output contracts. Paging is not generalized across all endpoints. |
| [Stephware/RoomDatabaseDiscussion](https://github.com/Stephware/RoomDatabaseDiscussion) | Room entities/DAO/database/repository and StateFlow ViewModels | **Implemented subset:** Room/cache/pending operations/photos plus WorkManager, stable-ID sync and StateFlow; administrative writes remain online. |
| [Stephware/ComposeUIEssentials](https://github.com/Stephware/ComposeUIEssentials) | Jetpack Compose screens and `NavHost` routing | **Aligned:** SmartFarm uses Compose screens, Navigation Compose, and Material 3. |
| [Stephware/MobdevMidtermExam](https://github.com/Stephware/MobdevMidtermExam) | Kotlin feature screens and Compose forms | **Aligned at UI-pattern level:** functional login and separate feature screens, adapted to SmartFarm. |
| [Stephware/WeatherConsoleApplication](https://github.com/Stephware/WeatherConsoleApplication) | Weather client/service abstraction, typed DTO/options | **Implemented:** OpenMeteo adapter, Worker ingestion, stored forecasts/advisories and freshness; live availability is a separate check. |
| [Stephware/HangfireConsoleApp](https://github.com/Stephware/HangfireConsoleApp) | Background job/service separation | **Implemented:** task/reminder, low-stock and weather jobs with in-app notices; headless Worker has no dashboard. |

## Current local source review

Updated 2026-11-11 against `dev` at `ac624be`; the external reference repositories
were not re-fetched or audited in this update. Their rows preserve the original
conceptual comparison, not claims about their current versions.

Use [architecture](../.local/karpathy-wiki/wiki/project/architecture.md) and
[local smoke checks](local-smoke-test.md) for actual SmartFarm hosts/commands.
PR14 pending-branch instructions and the old combined-host preview are historical.
The latest Pine and Gold Razor/Compose frontend and Phases A–D are implemented in
source; [progress](../.local/karpathy-wiki/wiki/project/progress.md) records provenance.
No fresh runtime result is supplied by this structural comparison.
