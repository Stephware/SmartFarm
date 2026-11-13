# AniGo product context

## Platform
web

This product record scopes the ASP.NET Core MVC/Razor Web redesign. The separate
native Android client remains supported and out of scope for visual changes here.

## Stack
ASP.NET Core MVC/Razor on .NET 10; shared SmartFarm services and SQL Server;
Identity cookies on Web, separate opaque-bearer API; Tailwind CSS v4 with prefixed
utilities and no Preflight. Preserve established code and minimize dependencies.

## Users and purpose
Farm Owners manage farm performance, people, requests and finances. Managers plan
and coordinate field operations, tasks, crop issues and supplies. Field Workers
need their assigned work and clear activity/inspection/harvest recording. Super
Admins operate a separately protected platform administration workspace. Farm
permissions and live authorization remain service-owned, independent of global roles.

## Operating context
Responsive Web must work at 320, 375, 768, 1024 and 1440 pixels, in light and dark
mode, with keyboard and accessible forms. Keep task and recording flows practical
for nontechnical farmers. Actual records, empty data, errors, many plots/tasks,
mixed harvest units, photos and pending requests are essential states.

## Capabilities and constraints
Preserve farm setup/enrollment; crop planning/lifecycle/history; monitoring/photos;
tasks/calendar; audited inventory and partial harvests; weather freshness;
unit-separated reports; manually recorded PHP income/expenses/cash difference;
notifications, account, announcements/support and protected administration.
No fabricated statistics, inferred crop health, sales or financial profit. Never
alter authorization, transactions, stable-ID sync, validation or database integrity
for presentation. No migrations or Android changes without an essential shared contract.

## Brand commitments
Keep AniGo's pine-green and restrained gold identity. Replace disorganized
composition with a coherent professional agricultural workspace. The supplied
VeterinaryClinicSystem master reference sets the bar for organization and craft,
not branding or business logic. Authentic licensed locally served agricultural
photography is appropriate for marketing; stock images must not imply real farm
conditions. Preserve all crop SVG assets for compatibility; remove their primary
visual prominence. Prefer neutral record placeholders and actual authorized photos.

## Evidence
User's Phase 12A–12F brief, 2026-11-13; current SmartFarm dev source and Phase 1–11
history; read-only VeterinaryClinicSystem source/render research. Historical test
results are not acceptance of this redesign. Production deployment, localization
beyond incumbent English and physical-device acceptance remain separately scoped.

## Principles and inclusion
One obvious primary action per task; role-appropriate information; truthful state;
clear numeric units/dates; readable type/contrast; visible labels/validation/focus;
44px minimum targets; responsive tables and forms without feature removal.
