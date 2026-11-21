# Current source status and publication scope

> Sources: SmartFarm source at dev `3c5a8b0`, inspected 2026-11-12
> Raw: [Documentation publication source check](../../raw/project/2026-11-12-documentation-publication.md); [MVP retirement](../../raw/project/2026-11-14-mvp-stage-5.md); [Stage 6 technical acceptance](../../raw/project/2026-11-14-mvp-stage-6.md)
> Updated: 2026-11-14

**Current MVP override:** [Farmer-first MVP status](mvp-simplification.md) and [the execution plan](../../../../docs/superpowers/plans/2026-11-13-mvp-simplification.md) supersede the earlier UI inventory below. Navigation, recording, offline confirmation and Stage 5 retirement are implemented; Stage 6 technical checks are recorded with provider/release limits; farmer sessions were waived/not conducted. Cash/bulletin/ticket-writing/chart presentation is retired; protected histories, compatibility finance API and all stored records remain.

This page records source changes since the earlier full audit and reference UI
verification. It supersedes conflicting baseline statements below; historical
counts/acceptance remain scoped to their recorded revision. This publication is
not a fresh full runtime audit.

## Web and Android

Web uses Razor, focused JavaScript and self-hosted Tailwind utilities. Run `npm ci`
and `npm run css:build` after relevant utility changes; commit generated
`anigo-tailwind.css`. Utilities use `tw:` and no Preflight; `reference-ui.css` loads
after utilities in application/admin layouts. Reuse shared presentation partials,
`farmer-operations.css`, `farm-navigation.js` and effective semantic tokens.

Android's compact workspace is Farm / Tasks / Record / Crops / More; Record opens
a native sheet, and More contains secondary operations. Farm switching can request
form-exit confirmation. Existing offline/session contracts remain authoritative.

## Administration and communication

MVC Super Admin functionality now exists: dashboard, users/farms, audit,
monitoring/reports, support and announcements. The portal requires `SuperAdminMfa`
(role plus MFA claim). Bootstrap is a guarded server CLI action for an existing
enabled account; it is never public signup or a farm membership role. TOTP and
one-time recovery-code login exist. Ordinary farm access remains separately checked.

Support pages scope cases to the signed-in user; admins see internal messages and
versioned status/priority actions. Announcements support admin draft/publication/
archive and user-facing reads. Consult actual controllers and
[operator setup](../../../../database/ADMIN_SETUP.md) before changes or provisioning.
This does not establish production configuration or native parity for these pages.

## SQL source and verification boundary

Current base includes AdminAuditLog: 25 tables, 214 columns, 44 foreign keys,
39 checks and 51 indexes. Finance, support and announcements extend the full EF
source to 29 tables, 256 columns, 53 foreign keys, 48 checks, 60 indexes and eight
rowversions. These are checked-in contracts, not live-schema results.

Select reviewed finance/admin/support/announcement upgrades for the actual target;
never rerun fresh DDL on populated data. `03_VerifySchema.sql` currently checks
only the base plus optional finance counts and can reject support/announcement
extensions. It is not full extended-model acceptance. Compare complete live
metadata and mappings explicitly; no schema/verifier changes are made here.

## Current CI and acceptance limits

CI now runs Node Web checks, local artwork validation, public Chromium smoke/a11y,
Tailwind generation consistency and PR before/after browser captures, alongside
.NET, Android JVM/build/device-test compilation and recovery/schema script checks.
It still does not run live SQL or an Android emulator, authenticated production
flows, external providers, backup restoration or deployment. Do not reuse earlier
runtime counts as acceptance of this revision.

## See Also

[Frontend](frontend.md), [Android](android.md), [Database](database.md),
[Development](development.md), [Architecture](architecture.md), [Known issues](known-issues.md).
