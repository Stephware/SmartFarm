# Finance, harvest reports and exports

> Sources: SmartFarm repository, source review
> Raw: [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md); [MVP retirement](../../raw/project/2026-11-14-mvp-stage-5.md)
> Updated: 2026-11-14

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

**Current MVP retirement:** Stage 5 removes cash/advanced-chart UI and Web cash CSV; those Web routes return 410. Native summaries fetch confirmed harvests only. The authenticated finance API/shared services remain for compatibility, with historical rows/tables unchanged. Simple harvest history, separate-unit/cycle totals and safe harvest CSV remain. Finance UI descriptions below are historical; service/contracts still describe the retained backend. See [MVP simplification](mvp-simplification.md).

## Harvest and dashboard read models

All active farm viewers can read reports; services authorize the farm before
querying. Harvest date filters use farm-local inclusive dates converted to UTC
boundaries, with UTC fallback for an invalid legacy timezone. Plot/crop filters
apply before aggregation. Valid totals exclude voids; void records remain in
history. Unit, crop/plot, monthly and cycle totals keep units separate.

Cycle comparison aggregates repeated partial harvests for one planting cycle,
not every planting on a plot. Filtered totals are not lifetime yield. Optional
quantity-per-area estimates use the current plot area, not historical area;
do not label them normalized agronomic productivity.

Dashboard provides crop/plot counts, task/stock/current concerning crop attention,
recent valid harvests and the last 30 farm days grouped by unit. “Concerning” uses
the latest observation on an active crop, not any historical bad observation or
follow-up resolution alone.

## Manual cash ledger and insights

Finance is PHP Income/Expense entered manually with category, date, optional
same-farm cycle allocation and notes. Owner/Manager records or voids with a reason;
all farm viewers can read. Amounts are positive with two decimal places. Voided
rows remain, excluded from cash totals. No automatic harvest sales or inventory
cost entries exist. Net recorded cash is not complete accounting profit.

History displays the newest 500 entries. Overview and filtered insights aggregate
all matching nonvoid rows, not that display subset. Inclusive ledger dates yield
income/expense/net, monthly, category and cycle/Farm-wide allocations. Date bounds
are 2000-01-01 through 2100-12-30 and From must not exceed To.

## Web versus Android

Web has full ledger entry/void/history and harvest/finance filters, accessible
chart/table alternatives and CSV exports. Harvest HTML shows recent 100 entries;
monthly charts cap 12 bars and comparisons 10 per unit. Finance HTML bounds trend
and breakdown display. These display limits must not truncate totals or exports.

Android insights are live read-only harvest/finance summaries with farm-local date
presets and independent failure states. They exclude pending local harvests. There
is no Room report cache, mobile cycle-total model/view, CSV export or finance entry
editor. Reports cancel old loads and clear on farm/period/account change.

## CSV safety and contract

MVC `ExportCsv`, `ExportCycleCsv`, and `ExportInsightsCsv` reauthorize and reuse
filters. Harvest entry export retains void flags; cycle totals export valid
quantities/counts, cycle identity, dates, units and filter context. Empty cycle
reports produce a header only. Finance export includes full matching summary,
monthly, category and allocation aggregates. Reuse shared cell escaping and
formula-injection protection, including leading whitespace/control characters;
quantities/money use invariant decimal formatting. Do not invent API CSV routes.

## Source navigation

[Report contracts](../../../../src/SmartFarm.Core/Contracts/Reports/FarmReportContracts.cs),
[report service](../../../../src/SmartFarm.Infrastructure/Services/Reports/FarmReportService.cs),
[finance service](../../../../src/SmartFarm.Infrastructure/Services/Finance/FarmFinanceService.cs),
[Web reports](../../../../src/SmartFarm.Web/Controllers/ReportsController.cs),
[Web finance](../../../../src/SmartFarm.Web/Controllers/FarmFinanceController.cs),
[CSV helpers](../../../../src/SmartFarm.Web/Clients).

## See Also

[Features](features.md), [Database](database.md), [API contracts](api-contracts.md),
[Android](android.md), [Frontend](frontend.md), [Decisions](decisions.md).
