# Implemented features

> Sources: SmartFarm repository, local source review
> Raw: [Feature sources](../../raw/project/2026-10-27-features-source.md); [Architecture sources](../../raw/project/2026-10-27-architecture-source.md); [Android sources](../../raw/project/2026-10-27-android-source.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [backend source review](../../raw/project/2026-11-11-backend-current.md); [reports source review](../../raw/project/2026-11-11-reports-current.md); [android source review](../../raw/project/2026-11-11-android-current.md); [web-workflow source review](../../raw/project/2026-11-11-web-workflow-current.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md); [MVP retirement](../../raw/project/2026-11-14-mvp-stage-5.md)
> Updated: 2026-11-14

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

**Current MVP override:** [Farmer-first MVP](mvp-simplification.md) supersedes legacy product UI in the table below. Stage 5 retires cash/charts, duplicate plot/timeline pages, public bulletins and ticket creation/replies. Backend finance compatibility and protected read-only histories remain; no underlying records/schema were removed. Required crop, task, photo, supply, harvest, weather and offline workflows remain implemented.

## Status meaning

This is the current source inventory, not a new runtime acceptance result.
Historical live checks are retained in [functional verification](functional-verification.md)
and [PR48 review](pr48-review.md), with their original scope.

| Feature | Current implementation |
| --- | --- |
| Accounts/enrollment | Simple registration; login/profile/refresh; signed-in code request, cancellation, owner approval/rejection/history/code rotation; in-app notices and rate limits |
| Farm workspace | Farms, plots, membership, location selection, layout and crop-history views; dashboard attention drill-downs |
| Crops | Global add-only crop catalog, plans, edits, lifecycle/history, Web crop agenda and illustrations |
| Monitoring | Field activities, inspections/private photos, structured issue category/severity and manager follow-up/task linking |
| Inventory | Setup/edit/archive, balances/low stock, receipt/consumption/adjustment/reversal ledger, filters and low-stock reminders |
| Linked supply use | Fertilizer/pesticide activity plus consumption and related task fulfillment saved atomically; API/Android `activity_stock` sync |
| Harvest | Repeated partial records, void history, unit-separated valid totals and cycle comparisons |
| Tasks | Web/API task management; Android Today/overdue/upcoming/completed groups, presets, manager create/edit, assignee closure and fieldwork links |
| Weather | OpenMeteo ingestion by Worker; saved forecasts/advisories and freshness on Web/Android |
| Reports/finance | Dashboard, filtered harvest/cycle comparisons; manual PHP cash ledger, date-filtered financial insights and Web CSV exports; Android read-only report/finance subset |
| Android offline | User-scoped cached reads; queued activities, observations, inventory movements, harvests, combined activity/stock and dependent photos; review/retry UI |

## Crop agenda and display branding

The Web agenda uses farm timezone dates and shared safe scheduling for planned
planting, actual planting, watering and expected harvest. It excludes terminal
cycles and preserves farm/month context. Android has illustrated crops, a farm
home/workspace and insights, but no equivalent monthly agenda. Latest Pine and
Gold styling supersedes older green/brown UI descriptions; see [frontend](frontend.md).

## Rules that affect future changes

- Registration grants no farm authority. Codes authorize requests, not access.
  Pending/rejected/cancelled requests give no membership. Only Owner controls
  members, enrollment decisions and codes; same decision can replay, conflicting
  decisions cannot overwrite history. Retired invitation routes return Gone.
- Crop types are global and creation-only. Planned → Active/Cancelled; Active →
  Completed/Failed/Cancelled; terminal cycles do not reopen. Only one Active
  cycle per plot. Changes use rowversion; farm/plot archiving checks active crops.
- New monitoring and harvest entries require an Active crop. History remains
  readable with permission. Structured issues require consistent category/severity;
  follow-up updates use observation rowversion and Owner/Manager authority.
- Workers may record field activities, inspections and harvests, and consume stock
  linked to an active same-farm crop activity. Receipts, adjustments, reversals and
  item management require Owner/Manager. Stock never becomes negative; retain
  ledger history. Combined fertilizer/pesticide entry requires a matching supply
  category and rolls back activity/task changes if stock fails.
- Harvest creation never completes a crop or updates stock. Voids require a
  manager/owner and reason. Report totals exclude voids and separate units and
  planting cycles. Harvest amounts are not sales revenue.
- Finance records are manually entered cash, not accounting profit or estimated
  crop value. All farm viewers can read; Owner/Manager records/voids. Insights
  aggregate all matching nonvoided entries, independently of the bounded history.
- Fieldwork can fulfill tasks; lifecycle changes cancel/close pending work and
  dismiss stale reminders. Cross-check these services together.

## Source navigation

[Shared services](../../../../src/SmartFarm.Infrastructure/Services),
[Core rules](../../../../src/SmartFarm.Core/Services),
[API](../../../../src/SmartFarm.Api/Controllers),
[MVC](../../../../src/SmartFarm.Web/Controllers),
[Android navigation](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/navigation/SmartFarmNavHost.kt).

## See Also

[Architecture](architecture.md), [Database](database.md), [Android](android.md),
[Automation](automation.md), [Finance and reports](finance-reports.md),
[Known issues](known-issues.md), [Progress](progress.md).
